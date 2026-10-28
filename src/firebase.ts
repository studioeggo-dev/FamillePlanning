import { initializeApp, getApps, type FirebaseApp } from "firebase/app";
import { getAuth, type Auth } from "firebase/auth";
import { getFirestore, type Firestore } from "firebase/firestore";
import { getFunctions, type Functions } from "firebase/functions";

// Même base nommée que l'application Android native.
export const DATABASE_ID = "ai-studio-familleplanning-5bf1e4ae-719b-4752-9367-6a8444b48be1";
export const PROJECT_ID = "gen-lang-client-0548701351";
export const FIREBASE_REGION = "europe-west9";

// L'application Web a son propre identifiant Firebase Web et son domaine OAuth.
// Ne jamais substituer l'identifiant d'application Android à l'identifiant Web.
const apiKey = import.meta.env.VITE_FIREBASE_API_KEY?.trim() || "";
const appId = import.meta.env.VITE_FIREBASE_APP_ID?.trim() || "";
const authDomain = import.meta.env.VITE_FIREBASE_AUTH_DOMAIN?.trim() || "";

export const configured = Boolean(apiKey && appId && authDomain);

export let app: FirebaseApp | null = null;
export let auth: Auth | null = null;
export let db: Firestore | null = null;
export let cloudFunctions: Functions | null = null;
if (configured) {
  app = getApps()[0] || initializeApp({
    apiKey, appId, authDomain, projectId: PROJECT_ID
  });
  auth = getAuth(app);
  auth.languageCode = "fr";
  db = getFirestore(app, DATABASE_ID);
  cloudFunctions = getFunctions(app, FIREBASE_REGION);
}
