# FAMILLEPLANNING — FEUILLE DE ROUTE ANDROID

## Phase 3A : Initialisation Android Native, Interface et Connexion Firebase (ACTUELLE)
- [x] Structure de projet Kotlin DSL (`settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`)
- [x] Package applicatif stable : `fr.familleplanning.app`
- [x] Intégration de `google-services.json` ciblant `gen-lang-client-0548701351`
- [x] Configuration explicite de Cloud Firestore sur la base nommée `ai-studio-familleplanning-5bf1e4ae-719b-4752-9367-6a8444b48be1`
- [x] Conception visuelle Jetpack Compose & Material 3 (thème chaleureux, cartes arrondies, couleurs des participants)
- [x] Écran 1 : Bienvenue (logo, message d'accueil, bouton Google, session, gestion d'erreurs)
- [x] Écran 2 : Planning (en-tête familial, vue semaine / mois, filtres participants, grille démo)
- [x] Écran 3 : Ma famille (cartes des membres, prénom, avatar, rôle, pastille colorée)
- [x] Écran 4 : Paramètres (compte Google, foyer actif, déconnexion, fuseau Europe/Paris)
- [x] Modèles de données Kotlin alignés avec les collections Firestore existantes
- [x] Architecture préliminaire du récepteur de partage natif (`ACTION_SEND`)
- [x] Documentation complète d'architecture et de configuration

---

## Phase 3B : Synchronisation du Calendrier Android (À VENIR)
- [ ] Branchement des requêtes Firestore en temps réel (`SnapshotListener`) sur `families/{familyId}/events`
- [ ] Écriture et mise à jour d'événements familiaux avec contrôle de rôle
- [ ] Prise en charge du cache hors-ligne Firestore natif
- [ ] Sélection des participants multiples et attribution automatique des couleurs

---

## Phase 3C : Récepteur de Partage Natif Android (Sharesheet) (À VENIR)
- [ ] Activation des `intent-filter` pour `Intent.ACTION_SEND` et `Intent.ACTION_SEND_MULTIPLE`
- [ ] Prise en charge des types MIME : `text/plain`, `image/*`, `application/pdf`
- [ ] Écran modal de prévisualisation du contenu intercepté (message, capture, courrier scolaire)
- [ ] Validation manuelle par le parent avant toute création d'événement

---

## Phase 3D : Enrichissement et Mémoire Familiale (À VENIR)
- [ ] Intégration de la collection `families/{familyId}/memory`
- [ ] Organisation chronologique des souvenirs et documents partagés
- [ ] Recherche multi-critères dans le planning familial
