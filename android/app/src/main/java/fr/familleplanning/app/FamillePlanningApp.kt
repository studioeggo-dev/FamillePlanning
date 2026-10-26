package fr.familleplanning.app

import android.app.Application
import com.google.firebase.FirebaseApp
import fr.familleplanning.app.firebase.FirebaseManager

/**
 * Application principale FamillePlanning.
 * Initialise Firebase et configure l'accès direct à la base de données
 * Cloud Firestore nommée du projet.
 */
class FamillePlanningApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialisation de Firebase
        FirebaseApp.initializeApp(this)

        // 2. Validation et initialisation du connecteur Firestore dédié
        FirebaseManager.initialize(this)
    }
}
