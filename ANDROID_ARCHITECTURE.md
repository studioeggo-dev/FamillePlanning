# FAMILLEPLANNING — ARCHITECTURE ANDROID NATIVE (PHASE 3A)

## 1. Vue d'ensemble

FamillePlanning Android est conçue selon les standards Android officiels modernes :
- **Langage** : Kotlin (version 2.0+)
- **UI Toolkit** : Jetpack Compose & Material 3
- **Modèle architectural** : Single Activity (`MainActivity`), Architecture Repository & MVVM (Model-View-ViewModel)
- **Gestion de l'asynchronisme et des flux** : Kotlin Coroutines & `StateFlow`
- **Navigation** : Navigation Compose avec `NavHost` typé et destinations scellées
- **Persistence & Cloud** : Firebase Authentication et Cloud Firestore SDK officiel

---

## 2. Structure du projet

```
android/
├── build.gradle.kts                      # Configuration racine Gradle
├── settings.gradle.kts                   # Modules et référentiels
├── gradle.properties                     # Propriétés de compilation JVM / AndroidX
└── app/
    ├── build.gradle.kts                  # Dépendances Compose, Firebase BOM, Lifecycle
    ├── google-services.json              # Configuration Firebase spécifique au package
    └── src/
        └── main/
            ├── AndroidManifest.xml       # Permissions, déclaration Single Activity & préparation ACTION_SEND
            ├── res/
            │   ├── values/strings.xml    # Libellés applicatifs en français
            │   ├── values/colors.xml     # Palette de couleurs FamillePlanning
            │   └── values/themes.xml     # Thème natif Material
            └── java/fr/familleplanning/app/
                ├── FamillePlanningApp.kt # Application class (Init Firebase + Base nommée)
                ├── MainActivity.kt       # Activité unique et lanceur Google Sign-In
                ├── ui/
                │   ├── theme/
                │   │   ├── Color.kt      # Palette FamillePlanning (Corail, Ciel, Menthe, etc.)
                │   │   ├── Type.kt       # Typographie douce et moderne
                │   │   ├── Shape.kt      # Cartes arrondies (24dp) et boutons larges
                │   │   └── Theme.kt      # Thème Material 3
                │   ├── components/
                │   │   ├── FamilyHeader.kt   # En-tête de foyer, date, bascule Semaine/Mois
                │   │   ├── MemberChip.kt     # Pastille filtre par participant
                │   │   ├── EventCard.kt      # Carte d'événement avec bandeau coloré
                │   │   └── AppBottomBar.kt   # Barre de navigation inférieure
                │   └── screens/
                │       ├── WelcomeScreen.kt  # Accueil, logo, bouton Google, session
                │       ├── PlanningScreen.kt # Vue calendrier semaine/mois et démo locale
                │       ├── FamilyScreen.kt   # Membres du foyer et statuts Firestore
                │       └── SettingsScreen.kt # Paramètres, déconnexion et diagnostics
                ├── navigation/
                │   ├── Screen.kt         # Routes scellées (Welcome, Planning, Family, Settings)
                │   └── AppNavHost.kt     # Hôte de navigation Compose
                ├── data/
                │   ├── models/
                │   │   ├── Models.kt     # User, Family, FamilyAccess, FamilyMember, CalendarEvent, FamilyMemoryEntry
                │   │   └── DateTimeUtils.kt # Gestion stricte du fuseau "Europe/Paris"
                │   └── repository/
                │       ├── AuthRepository.kt     # Authentification et session
                │       ├── FamilyRepository.kt   # Données foyer Firestore
                │       └── PlanningRepository.kt # Événements démo isolés (Phase 3A)
                ├── firebase/
                │   └── FirebaseManager.kt        # Accès direct à la base Firestore nommée
                ├── viewmodel/
                │   ├── AuthViewModel.kt
                │   ├── PlanningViewModel.kt
                │   ├── FamilyViewModel.kt
                │   └── SettingsViewModel.kt
                └── sharing/
                    └── ShareReceiverArchitecture.kt # Préparation ACTION_SEND (Phase 3C)
```

---

## 3. Correspondance des Modèles Web / Android

| Concept métier | Chemin Firestore | Modèle TypeScript Web | Modèle Kotlin Android |
| :--- | :--- | :--- | :--- |
| **Utilisateur** | `users/{uid}` | `interface UserProfile` | `data class User` |
| **Foyer** | `families/{familyId}` | `interface Family` | `data class Family` |
| **Droit d'accès** | `families/{familyId}/access/{uid}` | `interface FamilyAccess` | `data class FamilyAccess` |
| **Membre** | `families/{familyId}/members/{memberId}` | `interface FamilyMember` | `data class FamilyMember` |
| **Événement** | `families/{familyId}/events/{eventId}` | `interface CalendarEvent` | `data class CalendarEvent` |
| **Mémoire** | `families/{familyId}/memory/{memoryId}` | `interface MemoryEntry` | `data class FamilyMemoryEntry` |

---

## 4. Gestion Temporelle et Fuseau Horaire

- **Règle absolue** : La famille est rattachée au fuseau `Europe/Paris`.
- **Conversion** :
  - Dans Cloud Firestore : Dates stockées en `Timestamp` UTC (`Instant`).
  - Côté Android : Toutes les projections horaires sont traduites vers `ZoneId.of("Europe/Paris")`.
  - Aucune heure locale n'est traitée comme une heure UTC pour éviter tout décalage d'une heure ou deux lors des changements d'heure (heure d'hiver / heure d'été).

---

## 5. Préparation du Partage Android (Sharesheet)

Le module `fr.familleplanning.app.sharing.ShareReceiverArchitecture` prépare la future Phase 3C :
- Contrats pour `Intent.ACTION_SEND` (texte brut de SMS/email, image de courrier, document PDF)
- Contrats pour `Intent.ACTION_SEND_MULTIPLE`
- Aucun moteur d'inférence ni analyseur n'est activé en Phase 3A conformément aux limites strictes.
