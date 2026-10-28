# FamillePlanning

Un produit, **deux applications développées en parallèle**, et un seul backend Firebase.

## Dépôt GitHub de référence

Ce dépôt **est** le projet FamillePlanning à développer. Ne pas importer, fusionner ou écraser le projet Google AI Studio historique de l'utilisateur. Pour Google AI Studio, créer **un nouvel espace de travail** en important ce dépôt.

| Partie | Emplacement | Environnement |
| --- | --- | --- |
| Application Android native | `android/` | Kotlin / Jetpack Compose, GitHub Actions |
| Application Web | `src/`, `index.html`, `package.json`, `vite.config.ts` | React / TypeScript / Vite, nouvel espace Google AI Studio |
| Backend métier sécurisé | `functions/` | Firebase Cloud Functions (déploiement séparé) |
| CI Android | `.github/workflows/android-build.yml` | Compilation et signature d'un APK |
| CI Web | `.github/workflows/web-build.yml` | TypeScript + build du site |

### Convention de travail

- **Ne pas modifier le dossier `android/` lors des itérations Web AI Studio** sauf demande explicite.
- **Ne pas modifier les fichiers Web pendant les correctifs purement Android** sauf nécessité réelle de schéma partagé.
- Utiliser les mêmes collections, identifiants et contrats backend pour les deux clients.
- Le Web et Android ont **des Firebase App IDs distincts**, dans **le même projet Firebase**.
- Le dépôt GitHub (branche `main`) est la référence du code ; vérifier les conflits avant de pousser depuis AI Studio.
- Ne pas écraser ou importer l'ancien projet AI Studio ; on repart dans **un nouveau projet AI Studio** à partir de ce code.
- Aucune donnée de famille de démonstration : uniquement les données du foyer réel.

### Backend partagé

- Firebase project ID : `gen-lang-client-0548701351`
- Firebase project number : `583230474913`
- Firestore database ID : `ai-studio-familleplanning-5bf1e4ae-719b-4752-9367-6a8444b48be1`
- Région Firestore : `europe-west9`
- Android package : `fr.familleplanning.app`

Les modifications de règles Firestore et le déploiement des Cloud Functions nécessitent un accès administrateur spécifique : une compilation GitHub n'active **pas** les nouvelles fonctions.

### Démarrer le Web dans un **nouveau projet Google AI Studio**

Voir [AI_STUDIO_NOUVEAU_WEB.md](AI_STUDIO_NOUVEAU_WEB.md). Le dossier racine contient déjà les fichiers Web React / Vite, il n'est pas nécessaire de créer un second dépôt.

### Développement de la gestion familiale

Voir [FAMILY_MEMBERS_V1.md](FAMILY_MEMBERS_V1.md). Les invitations et l'enrôlement utilisent des fonctions sécurisées qui doivent être publiées sur Firebase avant un test complet. Les enfants sont des profils sans compte de connexion.
