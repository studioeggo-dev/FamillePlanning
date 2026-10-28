# Ouvrir FamillePlanning Web dans un NOUVEAU Google AI Studio

## Objectif

**Ne toucher à aucun ancien projet Google AI Studio.** Créer un nouvel espace AI Studio destiné exclusivement au **Web de FamillePlanning**, en important le dépôt GitHub déjà utilisé pour Android :

https://github.com/studioeggo-dev/FamillePlanning

Le même dépôt contient `android/` (Kotlin), `src/` (React Web) et `functions/` (backend). L'application Web est un client séparé, mais partage la base Firebase familiale avec Android.

## Procédure

1. Ouvrir https://aistudio.google.com/ et choisir **Build / Créer**.
2. Depuis le champ du prompt, cliquer sur **+ / Ajouter des fichiers** > **Import from GitHub / Importer depuis GitHub**.
3. Autoriser l'accès GitHub au dépôt `studioeggo-dev/FamillePlanning` si l'option n'apparaît pas.
4. Importer le dépôt dans **un nouveau projet** AI Studio et sélectionner le mode **application Web**.
5. Vérifier la prévisualisation du Web React existant. Il doit démarrer sur l'authentification / configuration Firebase, **jamais** sur une famille de démonstration.
6. Une fois le projet associé, utiliser la synchronisation GitHub pour envoyer **uniquement les changements Web voulus**, après comparaison des fichiers modifiés. Ne pas reconstruire / remplacer `android/`.
7. Activer la configuration Web Firebase via les variables `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN` et `VITE_FIREBASE_APP_ID` de l'application Firebase **Web**; voir `.env.example`. Ne pas utiliser l'App ID Android.
8. Avant de publier le Web, configurer son domaine dans **Firebase Authentication > Authorized domains** et contrôler la connexion Google / e-mail.
9. Le projet n'est pas publié automatiquement par un simple build GitHub. Choisir explicitement son hébergement (par exemple Firebase Hosting).

## Premier prompt à donner à AI Studio (nouveau projet uniquement)

> Tu développes l'application Web FamillePlanning en React/TypeScript, **à partir de ce dépôt GitHub**.
>
> Ce projet est NOUVEAU et indépendant de mon ancien travail Google AI Studio : n'importe aucun fichier, code ou thème d'un autre projet AI Studio.
>
> Le dossier `android/` est l'application Android native déjà développée, utilisée par de vraies familles. **Ne le modifie pas.** Le dossier `functions/` contient les opérations Firebase sécurisées ; **ne déploie rien sans vérification**, notamment les règles Firestore.
>
> Ton travail porte sur l'application Web dans `src/` et les fichiers Web racine. La Web et Android doivent utiliser les **mêmes données Firebase**, la même base Firestore **nommée** et les mêmes identifiants de foyer/membres.
>
> Commence par examiner `README.md`, `FAMILY_MEMBERS_V1.md`, `src/firebase.ts` et `src/familyService.ts`. Ne recrée pas de famille de démonstration ni de rendez-vous fictifs.
>
> Priorités : connexion Google et e-mail, lecture du foyer déjà créé sur Android, affichage du planning réel, gestion des parents et enfants, invitation d'adultes par lien privé sécurisé.
>
> L'interface doit être colorée, intuitive, adaptée aux parents et aux enfants et parfaitement responsive.
>
> Préserve la compatibilité Firebase et les fonctionnalités Android existantes. Propose les modifications dans les seuls fichiers Web nécessaires et vérifie avec `npm run lint` et `npm run build`.
>
> Signale sans les masquer les configurations ou déploiements Firebase manquants. Ne simule jamais une connexion, un événement ni une invitation réussis.

## Ce qui existe déjà dans le nouveau Web sur GitHub

`src/App.tsx`, `src/firebase.ts`, `src/familyService.ts`, `src/index.css` implémentent une première base React orientée Firebase ; leur compilation a passé les contrôles TypeScript et Vite en CI. C'est un **point de départ**, pas une application déjà déployée ni validée de bout en bout.

## Ressource officielle

https://ai.google.dev/gemini-api/docs/aistudio-build-mode
