# FamillePlanning — Gestion des membres, enfants et invitations (V1)

## Architecture unique pour Android + Web

- **Projet Firebase** : `gen-lang-client-0548701351`, numéro `583230474913`.
- **Firestore** : base **nommée** `ai-studio-familleplanning-5bf1e4ae-719b-4752-9367-6a8444b48be1` en `europe-west9`.
- **Android** : Kotlin, Jetpack Compose, Google + e-mail/mot de passe, liens `familleplanning://invite?token=<jeton>`.
- **Web** : React, Firebase Auth + Firestore, liens HTTPS `<vrai-site>?invite=<jeton>`.
- **Backend** : `functions/index.js`, Callable Firebase Functions v2 en `europe-west9`, Firebase Admin SDK utilise explicitement la **base nommée**. **Ne jamais** utiliser implicitement `(default)`.
- **Zéro démo** : ni membres ni rendez-vous fictifs, ni compte enfant.

## Hiérarchie

| Rôle | Compte Auth | Nombre | Permissions |
| --- | --- | --- | --- |
| `owner` (parent fondateur) | Oui | 1 | Gère le foyer, enfants et invitations. |
| `parent` (second titulaire) | Oui | 0 ou 1 | Gère enfants et invitations. |
| `adult` | Oui | Autant que nécessaire | Participe au calendrier, sans administration. |
| `child` | **Non** | Autant que nécessaire | Profil de calendrier, administré par les titulaires. |

Les rôles ne codent aucun sexe ni genre. Le fondateur est titulaire automatiquement ; le second titulaire peut être un père, une mère ou toute autre personne responsable. Il n'y a **jamais plus de deux parents titulaires**. Les enfants de moins de 13 ans ne doivent pas posséder de compte Firebase de l'application.

Attention : les écrans de création de compte s'adressent aux adultes, mais ne constituent **pas une vérification d'âge indépendante**. Prévoir les contrôles de conformité avant lancement public.

## Schéma Firestore partagé

- `users/{uid}` : `currentFamilyId`, `familyIds`, `displayName`, `email` ; partagé Android et Web.
- `families/{id}` : `name`, `ownerId`, `timezone`, `createdAt`.
- `families/{id}/access/{uid}` : `role = owner | parent | adult`, `joinedAt`.
- `families/{id}/members/{memberId}` : `name`, `role`, `roleLabel`, `color`, `userId` (nul pour un enfant), `isChild`, `hasAccount`, `birthDate` (facultative).
- `families/{id}/events/{eventId}` : `startAt`, `endAt`, `participantIds` (IDs membres), etc.
- `families/{id}/parentSlots/second` : verrou transactionnel du deuxième titulaire (écriture **serveur exclusivement**).
- `familyInvites/{sha256(token)}` : jeton **haché** seulement, `familyId`, `role`, `targetEmail`, statut, expiration, consommation.
- `familyInviteQuotas/{uid}` : quota pour freiner les invitations abusives.

Les jetons bruts ne sont **jamais** stockés en clair dans Firestore. Les invitations expirent sous 48h et sont consommées une fois. Lors d'une invitation ciblée, l'e-mail **vérifié** du destinataire doit correspondre. L'ajout d'un second titulaire et la consommation du lien sont transactionnels.

### Backend disponible dans le code

- `createFamilyInvite`
- `previewFamilyInvite`
- `acceptFamilyInvite`
- `revokeFamilyInvite`
- `addFamilyChild`

Ces fonctions sont développées mais **leur déploiement effectif n'est pas confirmé**. Tant qu'elles ne sont pas publiées dans Firebase, les nouveaux boutons Android/Web indiqueront une erreur serveur. La compilation des applications **ne déploie jamais les fonctions automatiquement**.

## Déploiement côté Firebase (bloquant)

1. Vérifier les droits administrateur sur le projet **et** la facturation **Blaze** nécessaire aux Cloud Functions, sans engager de dépense sans autorisation.
2. Installer Firebase CLI, se connecter au bon projet (Google AI Studio ou environnement autorisé).
3. Depuis la racine du dépôt : `cd functions && npm install && npm test && cd ..`.
4. Commande ciblée : `firebase deploy --project gen-lang-client-0548701351 --only functions`.
5. Vérifier les fonctions en région **`europe-west9`** dans la console. Ne pas confondre avec la localisation Firestore `europe-west9`.
6. **Ne jamais déployer de nouvelles règles Firestore générales depuis ce dépôt** : le `firebase.json` fourni configure volontairement **uniquement les fonctions** ; les règles de production validées précédemment restent en place.
7. La règle Firestore finale doit toujours interdire la liste globale des familles : `allow list: if false`. Les collections `familyInvites`, `familyInviteQuotas`, `parentSlots` ne sont accessibles qu'au SDK Admin du backend.
8. Un service de déploiement AI Studio peut remplacer Firebase CLI **si** ce service sait réellement déployer les **Cloud Functions** et si son compte a les permissions nécessaires.

### Activation Web (blocante)

La version `src/App.tsx` est **une vraie application Web** et non un simulateur. Sa compilation CI ne garantit pas que l'instance hébergée est configurée.

1. Console Firebase > Paramètres du projet > application **Web** > configuration SDK.
2. Fournir `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN`, `VITE_FIREBASE_APP_ID` dans l'environnement du site, à partir de l'**application Web**, pas de la configuration Android.
3. Ajouter le vrai domaine d'hébergement dans **Authentication > Settings > Authorized domains**.
4. Activer **Google** et **E-mail/mot de passe** dans Firebase Authentication si nécessaire.
5. Publier la version Web sur un domaine HTTPS réel : ses liens d'invitation seront alors de la forme `https://vrai-domaine/?invite=TOKEN`. Une URL locale ne se partage pas à distance.
6. Tester ouverture du lien depuis Android : lien web dans navigateur (authentification et acceptation), lien natif `familleplanning://invite?token=TOKEN` directement dans l'app. Le compte rejoint la **même** famille et retrouve ses données après connexion.

### Test fonctionnel, après déploiement

1. Parent fondateur (Android) crée un profil enfant. Vérifier `hasAccount:false`, `userId:null` et puce de couleur dans Android et Web.
2. Parent fondateur crée un lien pour « deuxième parent », partage, l'autre adulte se connecte sur Android via Google ou e-mail vérifié et accepte.
3. Vérifier deux titulaires et refuser une troisième demande de parent, même sous deux acceptations simultanées.
4. L'adulte invité (rôle `adult`) peut lire le planning mais pas inviter d'autres personnes ni créer des enfants.
5. L'enfant n'a pas de jeton Firebase ni de connexion propre.
6. Créer un événement attribué à l'enfant sur Android, le voir sur Web, puis inversement.
7. Réutiliser le même lien : refus. Expiration 48 heures : refus. E-mail cible différente : refus.
8. Vérifier l'impossibilité d'énumérer les familles et de lire le foyer d'un autre compte.

## Important

Le code préparé couvre l'interface Android et Web, le schéma partagé et les fonctions d'invitation. Il **ne constitue pas une validation de bout en bout sur deux appareils** tant que les fonctions Cloud et le site Web n'ont pas été publiés et testés réellement.
