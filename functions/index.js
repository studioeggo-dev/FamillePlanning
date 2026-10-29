"use strict";

const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore, Timestamp, FieldValue} = require("firebase-admin/firestore");
const {
  createToken, tokenHash, requiredName, validColor, normalizeEmail,
  validBirthDate, isHolder, isFullHolderGroup, normalizeInviteRole
} = require("./inviteCore");

const PROJECT_ID = "gen-lang-client-0548701351";
const DATABASE_ID = "ai-studio-familleplanning-5bf1e4ae-719b-4752-9367-6a8444b48be1";
const REGION = "europe-west9";
const app = initializeApp({projectId: PROJECT_ID});
const db = getFirestore(app, DATABASE_ID);
const options = {region: REGION, maxInstances: 10, timeoutSeconds: 30};
const signedIn = (request) => {
  if (!request.auth?.uid) throw new HttpsError("unauthenticated", "Connecte-toi pour continuer.");
  return request.auth.uid;
};
const assertVerifiedEmail = (request) => {
  const email = normalizeEmail(request.auth?.token?.email);
  if (!email || request.auth.token.email_verified !== true) {
    throw new HttpsError("permission-denied", "Vérifie ton adresse e-mail avant de rejoindre une famille.");
  }
  return email;
};
const parse = (fn) => {
  try { return fn(); }
  catch { throw new HttpsError("invalid-argument", "Vérifie les informations saisies."); }
};
const accessRef = (familyId, uid) => db.collection("families").doc(familyId).collection("access").doc(uid);
const safeFamilyId = (value) => {
  if (typeof value !== "string" || !/^[A-Za-z0-9_-]{8,128}$/.test(value)) {
    throw new HttpsError("invalid-argument", "Identifiant de famille invalide.");
  }
  return value;
};
async function assertParent(familyId, uid) {
  const [family, access] = await Promise.all([
    db.collection("families").doc(familyId).get(), accessRef(familyId, uid).get()
  ]);
  if (!family.exists || !(family.get("ownerId") === uid || (access.exists && isHolder(access.get("role"))))) {
    throw new HttpsError("permission-denied", "Seul un parent titulaire peut effectuer cette action.");
  }
  return family;
}
const apiError = (error) => {
  if (error instanceof HttpsError) throw error;
  console.error("Family operation failed:", error?.code || error?.name || "unknown");
  throw new HttpsError("internal", "Action indisponible. Réessaie plus tard.");
};

// Invitation de participants connectés, sans condition de lien familial ou de majorité.
// Le rôle parent reste accepté pour les anciennes invitations, sans être l'option
// standard : il est distinct du profil affiché (enfant, ado, proche, colocataire).
// Un participant peut contribuer aux événements mais n'administre pas le foyer.
exports.createFamilyInvite = onCall(options, async (request) => {
  try {
    const uid = signedIn(request);
    const familyId = safeFamilyId(request.data?.familyId);
    const role = parse(() => normalizeInviteRole(request.data?.role));
    const email = parse(() => normalizeEmail(request.data?.email || null));
    await assertParent(familyId, uid);
    const token = createToken();
    const invite = db.collection("familyInvites").doc(tokenHash(token));
    const quota = db.collection("familyInviteQuotas").doc(uid);
    const now = Date.now();
    const expiryMs = now + 48 * 60 * 60 * 1000;
    await db.runTransaction(async (tx) => {
      const quotaSnap = await tx.get(quota);
      const state = quotaSnap.data() || {};
      const start = state.windowStart?.toMillis?.() || 0;
      const count = now - start >= 86400000 ? 0 : (state.count || 0);
      if (count >= 10) throw new HttpsError("resource-exhausted", "Limite de 10 invitations par jour.");
      tx.set(quota, {
        windowStart: Timestamp.fromMillis(count === 0 ? now : start),
        count: count + 1
      }, {merge:true});
      tx.create(invite, {
        familyId, createdBy: uid, role, targetEmail: email,
        status:"pending", createdAt: Timestamp.fromMillis(now),
        expiresAt: Timestamp.fromMillis(expiryMs)
      });
    });
    return {token, expiresAt: new Date(expiryMs).toISOString()};
  } catch (e) { apiError(e); }
});

exports.previewFamilyInvite = onCall(options, async (request) => {
  try {
    signedIn(request);
    const hash = parse(() => tokenHash(request.data?.token));
    const invite = await db.collection("familyInvites").doc(hash).get();
    const data = invite.data();
    if (!data || data.status !== "pending" || data.expiresAt.toMillis() <= Date.now()) {
      throw new HttpsError("not-found", "Invitation expirée, révoquée ou déjà utilisée.");
    }
    const family = await db.collection("families").doc(data.familyId).get();
    if (!family.exists) throw new HttpsError("not-found", "Famille introuvable.");
    return {familyName: family.get("name"), role: data.role, expiresAt: data.expiresAt.toDate().toISOString()};
  } catch (e) { apiError(e); }
});

exports.acceptFamilyInvite = onCall(options, async (request) => {
  try {
    const uid = signedIn(request);
    const email = assertVerifiedEmail(request);
    const hash = parse(() => tokenHash(request.data?.token));
    const name = parse(() => requiredName(request.data?.firstName, 2, 60));
    const color = parse(() => validColor(request.data?.color || "#48DBFB"));
    const inviteRef = db.collection("familyInvites").doc(hash);
    return await db.runTransaction(async (tx) => {
      const invite = await tx.get(inviteRef);
      const data = invite.data();
      if (!data || data.status !== "pending" || data.expiresAt.toMillis() <= Date.now()) {
        throw new HttpsError("failed-precondition", "Ce lien n'est plus valide.");
      }
      if (data.targetEmail && data.targetEmail !== email) {
        throw new HttpsError("permission-denied", "Cette invitation est réservée à une autre adresse e-mail.");
      }
      const familyRef = db.collection("families").doc(data.familyId);
      const userRef = db.collection("users").doc(uid);
      const membershipRef = accessRef(data.familyId, uid);
      const membersRef = familyRef.collection("members");
      const secondSeat = familyRef.collection("parentSlots").doc("second");
      const holderQuery = familyRef.collection("access").where("role", "in", ["owner", "parent", "admin"]);
      // All reads are before the first transaction write.
      const [family, user, access, holders, reservedSeat] = await Promise.all([
        tx.get(familyRef), tx.get(userRef), tx.get(membershipRef),
        tx.get(holderQuery), tx.get(secondSeat)
      ]);
      if (!family.exists) throw new HttpsError("not-found", "Famille introuvable.");
      if (access.exists) throw new HttpsError("already-exists", "Tu fais déjà partie de cette famille.");
      if (data.role === "parent" &&
          (reservedSeat.exists || isFullHolderGroup(holders.docs.map(d=>d.get("role"))))) {
        throw new HttpsError("failed-precondition", "Cette famille possède déjà deux parents titulaires.");
      }
      const memberRef = membersRef.doc();
      const now = FieldValue.serverTimestamp();
      // Les vieux liens role=adult restent acceptés, mais les nouveaux profils
      // affichent un rôle neutre en âge et en relation familiale.
      const memberRole = data.role === "adult" ? "participant" : data.role;
      // One document for the second-holder slot prevents concurrent invitations
      // from enrolling two second parents (transaction read/write conflict).
      if (data.role === "parent") {
        tx.create(secondSeat, {uid, createdAt:now});
      }
      tx.create(membershipRef, {role: data.role === "adult" ? "participant" : data.role, joinedAt: now, invitedBy: data.createdBy});
      tx.create(memberRef, {
        name, userId: uid, role: memberRole,
        roleLabel: memberRole === "parent" ? "Parent titulaire" : "Participant invité",
        color, avatarEmoji:"🙂", isChild:false, hasAccount:true, createdAt:now
      });
      tx.set(userRef, {
        id: uid,
        displayName: request.auth.token.name || name, email,
        currentFamilyId: data.familyId,
        familyIds: FieldValue.arrayUnion(data.familyId),
        updatedAt: now,
        ...(!user.exists ? {createdAt: now} : {})
      }, {merge:true});
      tx.update(inviteRef, {status:"accepted", acceptedBy:uid, acceptedAt:now});
      return {familyId:data.familyId, role:data.role};
    });
  } catch (e) { apiError(e); }
});

exports.revokeFamilyInvite = onCall(options, async (request) => {
  try {
    const uid = signedIn(request);
    const hash = parse(() => tokenHash(request.data?.token));
    const ref = db.collection("familyInvites").doc(hash);
    const invite = await ref.get();
    if (!invite.exists) throw new HttpsError("not-found", "Invitation introuvable.");
    await assertParent(invite.get("familyId"), uid);
    await db.runTransaction(async tx => {
      const snap = await tx.get(ref);
      if (snap.exists && snap.get("status") === "pending") {
        tx.update(ref,{status:"revoked", revokedBy:uid, revokedAt:FieldValue.serverTimestamp()});
      }
    });
    return {revoked:true};
  } catch (e) { apiError(e); }
});

exports.addFamilyChild = onCall(options, async (request) => {
  try {
    const uid = signedIn(request);
    const familyId = safeFamilyId(request.data?.familyId);
    const name = parse(() => requiredName(request.data?.firstName, 2, 60));
    const color = parse(() => validColor(request.data?.color || "#A55EEA"));
    const birthDate = parse(() => validBirthDate(request.data?.birthDate));
    await assertParent(familyId, uid);
    const ref = db.collection("families").doc(familyId).collection("members").doc();
    await ref.create({
      name, color, role:"child", roleLabel:"Enfant", isChild:true,
      hasAccount:false, userId:null, birthDate,
      createdBy:uid, createdAt: FieldValue.serverTimestamp()
    });
    return {memberId:ref.id};
  } catch (e) { apiError(e); }
});
