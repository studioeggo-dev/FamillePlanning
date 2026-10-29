# FamillePlanning — Contrat fonctionnel des membres et future mémoire familiale

## Intention produit

FamillePlanning est un planning collaboratif, pas nécessairement un planning limité à deux parents et leurs enfants.

Deux actions principales, identiques Web / Android :

1. **Inviter un participant par lien** : n'importe quel proche avec un compte admissible (ado, colocataire, grand-parent, ami) peut rejoindre le foyer partagé. Il s'authentifie avec Google ou un e-mail vérifié. L'organisateur peut réserver l'invitation à un e-mail. Jeton aléatoire à usage unique, durée de vie 48 h, hachage en base, 10 invitations maximum par émetteur et par jour. Par défaut le rôle Firestore est `participant`. Les anciennes valeurs `adult` sont acceptées comme alias; `parent` est conservé uniquement pour rétrocompatibilité et une éventuelle gestion avancée.
2. **Ajouter un profil enfant sans compte** : prénom et couleur obligatoires, date de naissance facultative. Aucun identifiant de connexion, ni compte Google, ni adresse e-mail. Un adulte organisateur enregistre les renseignements et peut le sélectionner dans les événements.

Les droits d'accès (`families/{id}/access/{uid}`) sont **indépendants** du profil de calendrier (`families/{id}/members/{memberId}`). Aucun enfant sans compte ne reçoit un accès au backend ; un participant connecté ne devient pas administrateur du foyer.

## Schéma en place et conventions compatibles

- `members/{memberId}` : `name`, `color`, `role`, `roleLabel`, `userId` (identifiant Auth pour un utilisateur connecté, null pour un profil sans compte), `hasAccount`, `isChild`, `birthDate` (facultative).
- `access/{uid}` : `role` = `owner` / `parent` historique / `participant` (ou `adult` historique). Ces droits sont toujours validés côté serveur. `participant` peut voir les membres et créer/modifier des événements grâce à ses droits Firestore, mais ne peut pas créer des invitations ni administrer la famille.
- `events/{eventId}` : `participantIds` contient les ID stables des documents `members`, que le membre ait ou non un compte. Les mêmes IDs servent aux filtres et aux couleurs Android et Web.
- Ne jamais déduire les permissions Firebase d'un champ d'affichage `roleLabel` ou de `isChild`.

## Mémoire familiale : préparation de la future saisie par partage

Cette étape **définit le contrat**, elle ne met pas encore en service un modèle d'extraction automatique.

Pour chaque membre, une mémoire structurée optionnelle pourra décrire :

- **Identité utile** : prénom affiché, surnoms et orthographes alternatives. Éviter les identifiants personnels inutiles.
- **Activités** : sport, club, jour habituel, lieu habituel, éventuellement entraîneur / contact si les responsables choisissent de les renseigner.
- **École et organisation** : école, classe, activités périscolaires ; ne collecter que ce qui sert à l'organisation.
- **Corrections explicites** : un adulte corrige « Gabin » vers le bon profil ; la correction vérifiée peut aider les propositions futures.

Il faut identifier les participants par `memberId`, jamais uniquement par le texte `Gabin`. En cas de plusieurs correspondances, demander de choisir. Une date relative comme « mercredi » doit être interprétée en Europe/Paris selon le contexte temporel du message, et être montrée explicitement avant validation. Un horaire incomplet, un lieu absent ou une ambiguïté ne sont jamais inventés.

## Exemple attendu de proposition

Texte partagé : « foot mercredi 10 - 12h pour Gabin (11 ans) ».

Après extraction, l'application propose :
- Titre : « Foot »
- Date : le mercredi correspondant au message, affiché en jour et date exacts
- Heures : 10:00–12:00, fuseau Europe/Paris
- Participant : `memberId` du profil Gabin, si correspondance suffisamment fiable
- Lieu : vide si non mentionné
- Bouton : « Ajouter au planning » après validation humaine.

Si l'activité est connue dans la mémoire familiale, un lieu ou un nom habituel pourra être **suggéré**, mais ne doit pas être imposé comme une information certaine.

## Sécurité, enfants et consentement

Pas de compte requis pour créer un profil enfant ; accès aux données du foyer strictement réservé aux comptes membres. Pour un adolescent souhaitant un compte, vérifier les conditions d'âge du fournisseur d'authentification et, lorsque nécessaire, le consentement du responsable légal. La date de naissance complète n'est pas obligatoire. La mémoire et les données sensibles doivent être modifiables et supprimables par les responsables autorisés. Ne pas utiliser les données familiales pour l'entraînement d'un modèle ou un traitement tiers sans base appropriée et information explicite.

## Découpage de développement

- **4A** : rôle participant générique côté invitation, libellés Web/Android, stabilité des IDs et profils enfants sans compte.
- **4B** : déploiement sécurisé et tests d'intégration des Cloud Functions d'invitation, URL HTTPS Web stable + Android deep link, contrôle du partage, révocation.
- **4C** : gestion des profils enfants, corrections, identité et activités optionnelles enregistrées dans Firestore avec règles dédiées.
- **5** : partage natif Android / Web, extraction proposée puis validation humaine ; aucun enregistrement autonome.

Important : le code des Cloud Functions ne vaut pas déploiement. Tant qu'elles ne sont pas publiées sur le bon projet, la création d'invitations échoue proprement.
