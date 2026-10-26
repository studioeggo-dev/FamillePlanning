# FAMILLEPLANNING — CONFIGURATION FIREBASE ANDROID (PHASE 3A)

## 1. Paramètres de Connexion

L'application Android est raccordée sans modification d'architecture au backend Firebase existant :

| Paramètre | Valeur déclarée et configurée |
| :--- | :--- |
| **Projet Firebase** | `gen-lang-client-0548701351` |
| **Numéro de projet** | `763272373321` |
| **Base Cloud Firestore** | `ai-studio-familleplanning-5bf1e4ae-719b-4752-9367-6a8444b48be1` (Base nommée) |
| **Région Firestore** | `europe-west9` (Paris) |
| **Package Android** | `fr.familleplanning.app` |
| **Fichier de configuration** | `/android/app/google-services.json` |

---

## 2. Règle Critique sur la Base Firestore

Pour éviter toute écriture ou lecture parasite sur la base par défaut non initialisée :

```kotlin
// Dans fr.familleplanning.app.firebase.FirebaseManager :
val app = FirebaseApp.getInstance()
val db = FirebaseFirestore.getInstance(app, "ai-studio-familleplanning-5bf1e4ae-719b-4752-9367-6a8444b48be1")
```

Un contrôle de sécurité dynamique vérifie en permanence que `db.databaseId.database == DATABASE_ID`. Toute tentative d'accès à la base `(default)` déclenche immédiatement une exception bloquante.

---

## 3. Authentification et Restauration de Session

- **Moyen d'authentification** : Google Sign-In avec `play-services-auth` et échange d'identifiants Firebase via `GoogleAuthProvider.getCredential(idToken, null)`.
- **Restauration de session** :
  - `FirebaseAuth.AuthStateListener` écoute l'état de l'utilisateur.
  - Dès le lancement dans `AuthViewModel`, si `authRepository.currentUser != null`, la session est immédiatement restaurée et l'écran principal ou la vue calendrier est accessible sans reconnexion nécessaire.

---

## 4. Politique de Sécurité et Limites de la Phase 3A

1. **Aucune écriture dans les événements Firestore** :
   Les événements affichés sur l'écran Planning en Phase 3A proviennent exclusivement de données de démonstration locales hébergées dans `PlanningRepository`.
2. **Pas de famille fictive dans Firestore** :
   Aucun faux document n'est créé dans Firestore. Si le compte connecté n'est pas encore lié à un foyer dans `users/{uid}`, l'application affiche un statut informatif clair invitant à lier le foyer.
3. **Aucune clé privée ni token d'administration dans le code client** :
   Conformément aux règles de sécurité mobile, seules les clés publiques et identifiants clients Android autorisés figurent dans `google-services.json`.
