import {
  collection, doc, getDoc, getDocs, setDoc, writeBatch, onSnapshot,
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
  title: string;
  startAt: Date;
  endAt: Date;
  participantIds: string[];
  location: string;
  category: string;
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
export function observeEvents(familyId:string,cb:(events:Event[])=>void,onError:(error:Error)=>void):Unsubscribe {
  return onSnapshot(collection(database(),"families",familyId,"events"),snapshot=>{
    cb(snapshot.docs.flatMap(s=>{
      const d=s.data();
      const start = (d.startAt || d.startInstant) as Timestamp | undefined;
      const end = (d.endAt || d.endInstant) as Timestamp | undefined;
      if (!start?.toDate || !end?.toDate) return [];
      return [{
        id:s.id, title:String(d.title||""), startAt:start.toDate(), endAt:end.toDate(),
        participantIds:Array.isArray(d.participantIds)?d.participantIds:[],
        location:String(d.location||""),category:String(d.category||"general")
      }];
    }));
  },error=>onError(error));
}
export async function addEvent(familyId:string,uid:string,data:{
  title:string,startAt:Date,endAt:Date,participantIds:string[],location:string
}) {
  if (data.endAt<=data.startAt || !data.title.trim()) throw new Error("Vérifiez le titre et les horaires.");
  const ref=doc(collection(database(),"families",familyId,"events"));
  await setDoc(ref,{
    familyId,title:data.title.trim(),description:"",
    startAt:Timestamp.fromDate(data.startAt),endAt:Timestamp.fromDate(data.endAt),
    allDay:false,participantIds:data.participantIds,category:"general",
    color:"#FF6B6B",location:data.location.trim(),createdBy:uid,createdAt:serverTimestamp()
  });
}
async function invoke<P,R>(name:string, payload:P):Promise<R> {
  const run=httpsCallable<P,R>(functions(),name);
  const result=await run(payload);
  return result.data;
}
export const addChild = (familyId:string,firstName:string,birthDate:string|null,color:string) =>
  invoke<{familyId:string,firstName:string,birthDate:string|null,color:string},{memberId:string}>(
    "addFamilyChild",{familyId,firstName,birthDate,color});
export const createInvite = (familyId:string,role:"parent"|"adult",email:string|null) =>
  invoke<{familyId:string,role:string,email:string|null},{token:string,expiresAt:string}>(
    "createFamilyInvite",{familyId,role,email});
export const previewInvite = (token:string) =>
  invoke<{token:string},{familyName:string,role:string,expiresAt:string}>("previewFamilyInvite",{token});
export const acceptInvite = (token:string,firstName:string,color:string) =>
  invoke<{token:string,firstName:string,color:string},{familyId:string,role:string}>(
    "acceptFamilyInvite",{token,firstName,color});
