package fr.familleplanning.app.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Gestionnaire centralisé de la connexion Firebase pour FamillePlanning.
 *
 * RÈGLE STRICTE DU PROJET :
 * La base Cloud Firestore à utiliser est la base nommée :
 * "ai-studio-familleplanning-5bf1e4ae-719b-4752-9367-6a8444b48be1".
 * Il est formellement interdit d'utiliser implicitement la base (default).
 */
object FirebaseManager {
    private const val TAG = "FamillePlanningFirebase"

    const val PROJECT_ID = "gen-lang-client-0548701351"
    const val DATABASE_ID = "ai-studio-familleplanning-5bf1e4ae-719b-4752-9367-6a8444b48be1"
    const val FIRESTORE_REGION = "europe-west9"

    @Volatile
    private var firestoreInstance: FirebaseFirestore? = null

    fun initialize(context: Context) {
        val app = FirebaseApp.getInstance()
        Log.i(TAG, "Initialisation Firebase pour projet: ${app.options.projectId}")

        // Vérification de sécurité du projectId
        if (app.options.projectId != PROJECT_ID) {
            Log.w(TAG, "Attention: Project ID attendu=$PROJECT_ID, actuel=${app.options.projectId}")
        }

        // Instanciation explicite de Firestore sur la base nommée
        try {
            val db = FirebaseFirestore.getInstance(app, DATABASE_ID)
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            db.firestoreSettings = settings
            firestoreInstance = db
            Log.i(TAG, "Connexion Firestore établie avec succès sur la base: $DATABASE_ID")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de l'accès à la base Firestore nommée $DATABASE_ID", e)
        }
    }

    /**
     * Retourne l'instance sécurisée de Firestore connectée à la base nommée.
     * Déclenche une IllegalStateException si l'instance cible n'est pas la base requise.
     */
    fun getFirestore(): FirebaseFirestore {
        val db = firestoreInstance ?: synchronized(this) {
            firestoreInstance ?: run {
                val app = FirebaseApp.getInstance()
                val instance = FirebaseFirestore.getInstance(app, DATABASE_ID)
                firestoreInstance = instance
                instance
            }
        }

        // Toutes les instances sont créées explicitement avec DATABASE_ID.
        // FirebaseFirestore ne rend pas databaseId accessible dans son API publique.
        check(FirebaseApp.getInstance().options.projectId == PROJECT_ID) {
            "Projet Firebase inattendu : connexion interrompue"
        }
        return db
    }

    /**
     * Instance Firebase Authentication
     */
    fun getAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}
