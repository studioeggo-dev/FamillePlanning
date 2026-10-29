import {
  collection, doc, getDoc, getDocs, setDoc, updateDoc, deleteDoc, writeBatch, onSnapshot,
  serverTimestamp, Timestamp, arrayUnion, type Unsubscribe
} from "firebase/firestore";
import { httpsCallable } from "firebase/functions";
import {db, cloudFunctions} from "./firebase";
import type { User } from "firebase/auth";

export interface Family {
  id: string;
  name: string;
  ownerId: string;
}
export interface Member {
  id: string;
  firstName: string;
  role: "parent" | "adult" | "child" | string;
  roleLabel: string;
  color: string;
  isChild: boolean;
  hasAccount: boolean;
  userId: string | null;
  birthDate: string | null;
}
export interface Event {
  id: string;
  familyId?: string;
  title: string;
  description: string;
  startAt: Date;
  endAt: Date;
  isAllDay: boolean;
  participantIds: string[];
  location: string;
  category: string;
  color: string;
  createdBy?: string;
  createdAt?: Date;
}
export interface AppProfile {
  currentFamilyId: string | null;
  displayName: string;
}
const database = () => {
  if (!db) throw new Error("La configuration Firebase Web est manquante.");
  return db;
};
const functions = () => {
  if (!cloudFunctions) throw new Error("Les fonctions Firebase ne sont pas configurées.");
  return cloudFunctions;
};
export const friendlyError = (error: unknown) => {
  const source = error as {code?:string, message?:string};
  switch (source?.code) {
    case "functions/not-found":
      return "Fonction Firebase non déployée. Déployez les fonctions d'invitation sur le projet.";
    case "functions/permission-denied":
      return source.message || "Cette action est réservée à un parent titulaire.";
    case "permission-denied":
    case "firestore/permission-denied":
      return "Les autorisations Firebase n'acceptent pas cette opération.";
    case "functions/failed-precondition":
      return source.message || "Cette invitation n'est plus utilisable.";
    case "auth/email-already-in-use":
      return "Cette adresse est déjà enregistrée. Connectez-vous.";
    case "auth/invalid-credential":
      return "Adresse ou mot de passe incorrect.";
    case "auth/unauthorized-domain":
      return "Ce domaine doit être autorisé dans Firebase Authentication.";
    default: return source?.message || "Une erreur est survenue.";
  }
};
export async function readProfile(uid: string): Promise<AppProfile> {
  const snap = await getDoc(doc(database(), "users", uid));
  const data = snap.data() || {};
  const familyIds = Array.isArray(data.familyIds) ? data.familyIds : [];
  return {
    currentFamilyId: data.currentFamilyId || data.activeFamilyId || familyIds[0] || null,
    displayName: data.displayName || ""
  };
}
export async function createFirstFamily(user: User, name: string, firstName: string, color: string) {
  const homeName = name.trim();
  const personName = firstName.trim();
  if (homeName.length < 2 || homeName.length > 80 || personName.length < 2 || personName.length > 60) {
    throw new Error("Renseignez le nom du foyer et votre prénom.");
  }
  const db = database();
  const userRef = doc(db, "users", user.uid);
  const existing = await getDoc(userRef);
  if (existing.data()?.currentFamilyId) throw new Error("Un foyer existe déjà pour ce compte.");
  const familyRef = doc(collection(db, "families"));
  const memberRef = doc(collection(db, "families", familyRef.id, "members"));
  const accessRef = doc(db, "families", familyRef.id, "access", user.uid);
  const batch = writeBatch(db);
  batch.set(familyRef, {name:homeName, ownerId:user.uid, timezone:"Europe/Paris", createdAt:serverTimestamp()});
  batch.set(accessRef, {role:"owner", joinedAt:serverTimestamp()});
  batch.set(memberRef, {
    name:personName, userId:user.uid, role:"parent", roleLabel:"Parent titulaire",
    color, avatarEmoji:"🙂", isChild:false, hasAccount:true
  });
  batch.set(userRef, {
    id:user.uid, email:user.email || "", displayName:user.displayName || personName,
    currentFamilyId:familyRef.id, familyIds:arrayUnion(familyRef.id),
    updatedAt:serverTimestamp(), ...(!existing.exists() ? {createdAt:serverTimestamp()} : {})
  }, {merge:true});
  await batch.commit();
  return familyRef.id;
}
export async function getHome(familyId:string):Promise<Family> {
  const snap = await getDoc(doc(database(),"families",familyId));
  if (!snap.exists()) throw new Error("Foyer introuvable dans Firebase.");
  const data = snap.data();
  return {id:snap.id, name:String(data.name||"Ma famille"),ownerId:String(data.ownerId||data.createdBy||"")};
}
export async function getRole(familyId:string,uid:string):Promise<string> {
  const access = await getDoc(doc(database(),"families",familyId,"access",uid));
  return String(access.data()?.role || "");
}
export function observeMembers(familyId:string,cb:(members:Member[])=>void,onError:(error:Error)=>void):Unsubscribe {
  return onSnapshot(collection(database(),"families",familyId,"members"), snapshot => {
    const members:Member[] = snapshot.docs.map(s => {
      const d=s.data();
      const role=String(d.role||"adult");
      return {
        id:s.id, firstName:String(d.name||d.firstName||"?"),
        role, roleLabel:String(d.roleLabel || (role==="parent"?"Parent titulaire":role==="child"?"Enfant":"Adulte")),
        color:String(d.color||"#FF6B6B"), isChild:d.isChild === true || role==="child",
        hasAccount:d.hasAccount === true || typeof d.userId==="string",
        userId:typeof d.userId==="string"?d.userId:null,
        birthDate:typeof d.birthDate==="string"?d.birthDate:null
      };
    });
    cb(members);
  },error=>onError(error));
}
function parseDate(val: unknown): Date | null {
  if (!val) return null;
  if (typeof (val as Timestamp).toDate === "function") return (val as Timestamp).toDate();
  if (val instanceof Date) return isNaN(val.getTime()) ? null : val;
  if (typeof val === "string" || typeof val === "number") {
    const d = new Date(val);
    return isNaN(d.getTime()) ? null : d;
  }
  return null;
}

export function observeEvents(familyId:string,cb:(events:Event[])=>void,onError:(error:Error)=>void):Unsubscribe {
  return onSnapshot(collection(database(),"families",familyId,"events"),snapshot=>{
    cb(snapshot.docs.flatMap(s=>{
      const d=s.data();
      const start = parseDate(d.startAt || d.startInstant || d.startDate || d.startTime);
      const end = parseDate(d.endAt || d.endInstant || d.endDate || d.endTime) || start;
      if (!start || !end) return [];
      const participants = Array.isArray(d.participantIds) ? d.participantIds :
        Array.isArray(d.participants) ? d.participants : [];
      const color = String(d.color || d.colorHex || "#FF6B6B");
      const isAllDay = d.allDay === true || d.isAllDay === true;
      const createdAt = parseDate(d.createdAt) || undefined;
      return [{
        id:s.id, familyId, title:String(d.title||d.name||""),
        description:String(d.description||""), startAt:start, endAt:end,
        isAllDay, participantIds:participants, location:String(d.location||""),
        category:String(d.category||"general"), color,
        createdBy:typeof d.createdBy==="string"?d.createdBy:undefined, createdAt
      }];
    }));
  },error=>onError(error));
}

export async function addEvent(familyId:string,uid:string,data:{
  title:string; description?:string; startAt:Date; endAt:Date;
  isAllDay:boolean; participantIds:string[]; category:string; color:string; location?:string;
}):Promise<string> {
  if (data.endAt < data.startAt || !data.title.trim()) throw new Error("Vérifiez le titre et les horaires du rendez-vous.");
  const ref=doc(collection(database(),"families",familyId,"events"));
  await setDoc(ref,{
    familyId, title:data.title.trim(), description:data.description?.trim()||"",
    startAt:Timestamp.fromDate(data.startAt), endAt:Timestamp.fromDate(data.endAt),
    allDay:Boolean(data.isAllDay), participantIds:data.participantIds,
    category:data.category||"general", color:data.color||"#FF6B6B",
    location:data.location?.trim()||"", createdBy:uid, createdAt:serverTimestamp()
  });
  return ref.id;
}

export async function updateEvent(familyId:string,eventId:string,data:{
  title:string; description?:string; startAt:Date; endAt:Date;
  isAllDay:boolean; participantIds:string[]; category:string; color:string; location?:string;
}):Promise<void> {
  if (data.endAt < data.startAt || !data.title.trim()) throw new Error("Vérifiez le titre et les horaires du rendez-vous.");
  const ref=doc(database(),"families",familyId,"events",eventId);
  await updateDoc(ref,{
    title:data.title.trim(), description:data.description?.trim()||"",
    startAt:Timestamp.fromDate(data.startAt), endAt:Timestamp.fromDate(data.endAt),
    allDay:Boolean(data.isAllDay), participantIds:data.participantIds,
    category:data.category||"general", color:data.color||"#FF6B6B",
    location:data.location?.trim()||""
  });
}

export async function deleteEvent(familyId:string,eventId:string):Promise<void> {
  const ref=doc(database(),"families",familyId,"events",eventId);
  await deleteDoc(ref);
}
async function invoke<P,R>(name:string, payload:P):Promise<R> {
  const run=httpsCallable<P,R>(functions(),name);
  const result=await run(payload);
  return result.data;
}
export async function addChild(
  familyId:string, uid:string, firstName:string, birthDate:string|null, color:string
):Promise<{memberId:string}> {
  const home = await getHome(familyId);
  const name = firstName.trim();
  if (name.length < 2 || name.length > 60 || !/^#[0-9a-fA-F]{6}$/.test(color)) {
    throw new Error("Prénom ou couleur invalide.");
  }
  if (birthDate) {
    const date = new Date(birthDate+"T00:00:00Z");
    if (!/^\d{4}-\d{2}-\d{2}$/.test(birthDate) ||
        !Number.isFinite(date.getTime()) ||
        date.toISOString().slice(0,10) !== birthDate ||
        date.getTime() > Date.now()) {
      throw new Error("Date de naissance invalide.");
    }
  }
  if (home.ownerId === uid) {
    // Règles Firestore côté serveur : seuls les administrateurs peuvent créer.
    const ref = doc(collection(database(),"families",familyId,"members"));
    await setDoc(ref,{
      name,role:"child",roleLabel:"Enfant",isChild:true,
      hasAccount:false,userId:null,birthDate,color,
      createdBy:uid,createdAt:serverTimestamp()
    });
    return {memberId:ref.id};
  }
  return invoke<{familyId:string,firstName:string,birthDate:string|null,color:string},{memberId:string}>(
    "addFamilyChild",{familyId,firstName:name,birthDate,color});
}
export const createInvite = (familyId:string,role:"parent"|"adult",email:string|null) =>
  invoke<{familyId:string,role:string,email:string|null},{token:string,expiresAt:string}>(
    "createFamilyInvite",{familyId,role,email});
export const previewInvite = (token:string) =>
  invoke<{token:string},{familyName:string,role:string,expiresAt:string}>("previewFamilyInvite",{token});
export const acceptInvite = (token:string,firstName:string,color:string) =>
  invoke<{token:string,firstName:string,color:string},{familyId:string,role:string}>(
    "acceptFamilyInvite",{token,firstName,color});
