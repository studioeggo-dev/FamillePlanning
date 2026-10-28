# FamillePlanning — CORRECTION BLOQUANTE : création du premier foyer depuis Android

## Symptôme réel

Google Sign-In fonctionne. Sur Android, l'utilisateur peut remplir « Créons votre famille ! », mais la tentative d'enregistrement renvoie `PERMISSION_DENIED: Missing or insufficient permissions`.

Le refus est renvoyé par **Cloud Firestore** au moment d'une écriture atomique. Ne jamais « corriger » cela par une règle générique `allow write: if request.auth != null`.

## Base et projet à contrôler

- Firebase project ID : `gen-lang-client-0548701351`
- Numéro réel du projet Google : `583230474913`
- Firestore database ID **nommé** : `ai-studio-familleplanning-5bf1e4ae-719b-4752-9367-6a8444b48be1`
- Région : `europe-west9`
- Package Android : `fr.familleplanning.app`
- Point d'entrée Kotlin : `android/app/src/main/java/fr/familleplanning/app/data/repository/FamilyRepository.kt` (`createFirstFamily`).

**Les règles de la base (default) ne sont pas celles de cette application.** Lire la version effectivement déployée sur la base nommée, pas seulement un ancien fichier de règles ni un simulateur personnalisé.

## Écritures exactes effectuées par Android dans le WriteBatch

1. `families/{familyId}` : `name`, `ownerId = auth.uid`, `timezone = "Europe/Paris"`, `createdAt`.
2. `families/{familyId}/access/{auth.uid}` : `role = "owner"`, `joinedAt`.
3. `families/{familyId}/members/{memberId}` : `name`, `userId = auth.uid`, `role = "parent"`, `roleLabel = "Parent"`, `color`, `avatarEmoji`.
4. `users/{auth.uid}` :
   - création si absent : `id`, `displayName`, `email`, `photoUrl`, `currentFamilyId`, `familyIds`, `createdAt`, `updatedAt` ;
   - mise à jour si existant : `currentFamilyId`, `familyIds` (arrayUnion), `updatedAt`.

La création atomique ne suffit pas à accorder des droits. **Les quatre mutations doivent chacune être autorisées par les règles**. Pour le premier foyer, il faut des conditions d'amorçage sur documents encore inexistants et le résultat du lot (`existsAfter`, `getAfter`). Un `exists()` classique voit l'état avant le lot.

## Travail Firebase nécessaire avant validation

1. Lire les règles **réellement actives** de la base nommée. Vérifier que les règles de bootstrap existent bien sur la base et ne sont pas uniquement celles de `(default)`.
2. Reproduire le WriteBatch exact dans **Firebase Emulator Suite**, avec le fichier de règles à déployer.
3. Identifier **quelle(s) des quatre écritures** est/sont refusée(s), en contrôlant notamment les listes autorisées de champs (`isValidUser`, `isValidFamily`, `members`) et la condition `existsAfter` de l'accès owner.
4. Corriger seulement les conditions nécessaires au premier foyer **dans une famille nouvellement créée**. Un utilisateur ne doit jamais pouvoir se créer un document d'accès owner sur une famille préexistante.
5. Préserver les invariants : contrôle `auth.uid`, contrôle `ownerId`, immutabilité de l'appartenance, refus d'auto-promotion et de rétrogradation du dernier owner, isolation des autres familles, mémoire familiale verrouillée.
6. Tester au minimum : première famille autorisée; utilisateur anonyme refusé; création owner dans famille existante refusée; auto-promotion refusée; lecture croisée refusée; l'utilisateur ayant déjà un foyer ne doit pas pouvoir contourner les contrôles; profil nouveau et profil déjà existant.
7. Déployer **sur cette base nommée** via un outil qui permet ce déploiement (Firebase CLI, projet déjà authentifié, ou environnement AI Studio disposant des autorisations). Contrôler l'identifiant de base avant de publier.
8. Refaire la création réelle depuis Android. Confirmer que les quatre documents existent, que le propriétaire peut les relire, puis vérifier l'affichage du calendrier vide (aucune donnée fictive).

## Gardes-fous

- Ne pas publier de règles `allow read, write: if true` ni `allow write: if request.auth != null` universel.
- Ne pas introduire de Firebase Admin SDK ni de clé privée dans le code Android.
- Ne pas inventer la réussite des tests ou du déploiement.
- Ne pas écraser les règles de la base nommée sans sauvegarde et comparaison des règles existantes.
- Préserver le schéma Web et les fonctionnalités de l'application familiale.
- Pour un déploiement sélectif sur une base nommée configurée dans firebase.json, consulter : https://firebase.google.com/docs/rules/manage-deploy

## Résultat attendu

Fournir :
- version avant/après des règles corrigées ;
- base nommée réellement ciblée ;
- résultat des tests des quatre écritures en batch ;
- résultat du déploiement réel ;
- résultat d'un test de création depuis Android.

**Cette correction backend est bloquante. La nouvelle interface Android seule ne suffit pas à lever PERMISSION_DENIED.**

## Revue du rapport AI Studio du 28 octobre 2026

Le rapport transmis par AI Studio affirme : 10 scénarios Firebase Emulator réussis, règles déployées sur la base nommée, suppression du `PERMISSION_DENIED`. **Ces assertions n'ont pas été vérifiées par un accès direct au backend Firebase** depuis ce dépôt. Aucune règle de production (`firestore.rules` / `firebase.json`) n'a été synchronisée ici au moment de la revue.

### Correction de confidentialité indispensable

Le code de règles cité dans le rapport contient :

```javascript
match /families/{familyId} {
  allow get: if isSignedIn() && (
    resource.data.ownerId == request.auth.uid ||
    exists(/databases/$(database)/documents/families/$(familyId)/access/$(request.auth.uid))
  );
  allow list: if isSignedIn(); // NON : expose la liste de toutes les familles aux comptes connectés
}
```

Sur la base réelle, remplacer la règle `list` par une politique compatible avec les usages autorisés. Si l'application charge la famille par son ID enregistré dans `users/{uid}.currentFamilyId`, **le choix le plus restrictif est** :

```javascript
allow list: if false;
```

Cela laisse `get` autoriser les membres à charger directement leur famille, sans permettre de parcourir les foyers des autres. Ne remplacer ce choix par une règle de liste filtrée qu'après avoir démontré la nécessité de la requête correspondante et sa conformité aux règles.

### Tests manquants à ajouter

- Un utilisateur authentifié non membre ne peut pas exécuter `getDocs(collection(db, "families"))` ni énumérer les familles d'autres foyers.
- Le propriétaire peut toujours lire `families/{currentFamilyId}` via `getDoc`.
- La création initiale du foyer et la lecture des 4 documents restent autorisées.
- Une seconde famille ou un accès owner sur un foyer existant ne peut pas être forgé par un utilisateur tiers.
- Les règles sont **déployées et vérifiées dans** `ai-studio-familleplanning-5bf1e4ae-719b-4752-9367-6a8444b48be1`, pas seulement dans `(default)`.

### Autre incohérence de configuration dans le rapport

Le rapport affiche `583230474913 (GCP) / 763272373321` comme numéros de projet. Le **fichier officiel Google Services Android** fourni par l'utilisateur indique `583230474913` ; éviter de réintroduire `763272373321` comme identifiant OAuth/Firebase.
