import { initializeApp, getApps, type FirebaseApp } from "firebase/app";
import { getAuth, type Auth } from "firebase/auth";
import { getFirestore, type Firestore } from "firebase/firestore";
import { getFunctions, type Functions } from "firebase/functions";

// Même base nommée que l'application Android native.
export const DATABASE_ID = "ai-studio-familleplanning-5bf1e4ae-719b-4752-9367-6a8444b48be1";
export const PROJECT_ID = "gen-lang-client-0548701351";
export const FIREBASE_REGION = "europe-west9";

// Configuration officielle de l'application Web "ai-studio-applet-webapp"
// copiée depuis Firebase > Paramètres du projet > Vos applications (28/10/2026).
// Ces identifiants SDK Web sont des paramètres clients publics, PAS des clés
// d'administration : la protection des familles dépend des règles Firestore.
// En cas d'hébergement différent, les variables VITE_ peuvent remplacer les
// valeurs ci-dessous, sans jamais utiliser l'appId Android.
const webFirebaseConfig = {
  apiKey: "AIzaSyAp37G05WjMjJ3Et3QuvSti6XIxsLKfFQ4",
  authDomain: "gen-lang-client-0548701351.firebaseapp.com",
  appId: "1:583230474913:web:967099f736c6e7b04dbee0",
};
const apiKey = import.meta.env.VITE_FIREBASE_API_KEY?.trim() || webFirebaseConfig.apiKey;
const appId = import.meta.env.VITE_FIREBASE_APP_ID?.trim() || webFirebaseConfig.appId;
const authDomain = import.meta.env.VITE_FIREBASE_AUTH_DOMAIN?.trim() || webFirebaseConfig.authDomain;

export const configured = Boolean(apiKey && appId && authDomain);

export let app: FirebaseApp | null = null;
export let auth: Auth | null = null;
export let db: Firestore | null = null;
export let cloudFunctions: Functions | null = null;
if (configured) {
  // Une instance nommée évite de réutiliser une éventuelle app Firebase
  // initialisée par Google AI Studio et configurée pour un autre projet.
  const appName = "familleplanning-web";
  app = getApps().find(candidate => candidate.name === appName)
    || initializeApp({apiKey, appId, authDomain, projectId: PROJECT_ID}, appName);
  auth = getAuth(app);
  auth.languageCode = "fr";
  db = getFirestore(app, DATABASE_ID);
  cloudFunctions = getFunctions(app, FIREBASE_REGION);
}
