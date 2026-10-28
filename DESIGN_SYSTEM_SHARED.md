# FamillePlanning — Contrat UX/UI partagé Android et Web

**Source de référence initiale :** `android/app/src/main/java/fr/familleplanning/app/ui/theme/`, `android/app/src/main/java/fr/familleplanning/app/ui/components/` et `android/app/src/main/java/fr/familleplanning/app/ui/screens/`. Le Web doit reproduire la même **expérience utilisateur** et les mêmes flux, sans être un simple simulateur Android dans un cadre de téléphone.

## Même produit, deux interfaces réellement natives à chaque plateforme

- Android : Kotlin / Jetpack Compose dans `android/`.
- Web : React / TypeScript / Vite dans `src/`; largeur téléphone responsive et écran ordinateur.
- Mêmes libellés, organisation, hiérarchie visuelle, rôles et règles de navigation ; présentation desktop adaptée aux grands écrans sans modifier les fonctions.
- Les modifications de parcours ou de design significatives doivent être appliquées aux deux clients. Aucun changement Web ne doit écraser les fichiers `android/`.
- Une seule identité Firebase, un seul `currentFamilyId`, les mêmes IDs de membres dans `participantIds` des événements, et **aucune donnée fictive**.
- GitHub `main` est le code de référence, synchronisé via Settings > GitHub > Pull (AI Studio), puis Push après vérification. Ne pas envoyer un vieux simulateur en remplaçant `src/App.tsx` de GitHub.

## Palette exacte (commune)

| Usage | Couleur |
| --- | --- |
| Action principale / corail | `#FF6B6B` |
| Corail foncé | `#EE5253` |
| Bleu ciel | `#48DBFB` |
| Menthe | `#1DD1A1` |
| Ambre | `#FECA57` |
| Lilas | `#A55EEA` |
| Pêche | `#FF9F43` |
| Fond général ivoire clair | `#FDFBF7` |
| Cartes / panneaux | `#FFFFFF` |
| Cartes secondaires | `#F7F4EE` |
| Bordures | `#EFE9DE` |
| Texte principal | `#2D3436` |
| Texte secondaire | `#636E72` |

Les couleurs de membres sont **librement choisies** et ne sont jamais associées par défaut à un genre (« maman rose / papa bleu » interdit). L'identifiant membre et sa couleur sont identiques Android/Web.

## Composants communs

1. **En-tête** : nom du foyer, date courante/choisie, action « Aujourd'hui » et navigation par dates.
2. **Calendrier** : bascule « Semaine / Mois », sélection d'une journée, pastilles de participants, événements en cartes lisibles, bouton + corail pour créer un événement.
3. **Navigation** : « Planning », « Ma famille », « Paramètres ». Navigation basse sur mobile Web comme sur Android. Desktop : menu latéral ou barre haute admis, mais mêmes sections et ordre.
4. **Membres** : pastille colorée avec initiale, prénom, rôle, distinction « Enfant · sans compte », identique sur les deux clients.
5. **Actions parentales** : « Ajouter un enfant », « Inviter un adulte », rôle « Parent titulaire » (maximum deux), « Adulte invité ». Aucun compte enfant de moins de 13 ans.
6. **Invitation** : choix parent titulaire/adulte, adresse réservée facultative, lien privé à usage unique valable 48 heures, saisie manuelle du code, acceptation après authentification.
7. **États réels** : chargement, erreur Firebase explicite, calendrier vide et succès. **Interdit de remplacer silencieusement un échec par la famille Martin ou des rendez-vous d'exemple.**
8. **Accessibilité** : surfaces tactiles >= 44 px, contraste suffisant, textes lisibles, étiquettes accessibles, réactivité téléphone/ordinateur, prise en compte navigation clavier.
9. **Expérience commune** : connexion et création de foyer, gestion des enfants, création/édition d'événements, dates, participants, invitations, erreurs, synchronisation ; chaque parcours doit être équivalent côté Web et Android.
10. **Pas de faux succès** : un bouton ne confirme jamais une opération Firebase avant réponse effective du backend.

## Processus de modification

- Pour toute fonctionnalité : comparer Android et Web, documenter les différences et confirmer la parité des actions au terme de la modification.
- AI Studio modifie `src/`, CSS et fichiers Web associés. **Il ne touche pas `android/`, aux règles de production Firestore, ni aux fonctions Firebase sans instruction explicite.**
- Les changements Android sont proposés par ChatGPT puis compilés avec GitHub Actions ; leur équivalent Web fait l'objet d'un prompt AI Studio.
- Vérifier `npm run lint` et `npm run build` avant Push Web, et Android build GitHub Actions avant APK.
- Vérifier un événement réel et un membre réel en utilisant le même compte depuis les deux interfaces.
