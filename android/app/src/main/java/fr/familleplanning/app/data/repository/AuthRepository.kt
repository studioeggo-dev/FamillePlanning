package fr.familleplanning.app.data.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import fr.familleplanning.app.data.models.User
import fr.familleplanning.app.firebase.FirebaseManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant

/**
 * Dépôt gérant l'authentification Firebase et la restauration de session.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseManager.getAuth()
) {

    /**
     * Écoute en temps réel l'état d'authentification Firebase (restauration automatique de session).
     */
    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { currentAuth ->
            trySend(currentAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Authentifie l'utilisateur avec des identifiants Google Sign-In.
     */
    suspend fun signInWithGoogleCredential(credential: AuthCredential): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("Utilisateur introuvable après connexion")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Récupère le document profil utilisateur depuis Cloud Firestore: users/{uid}
     */
    suspend fun getUserProfile(uid: String): Result<User?> {
        return try {
            val doc = FirebaseManager.getFirestore()
                .collection("users")
                .document(uid)
                .get()
                .await()

            if (doc.exists() && doc.data != null) {
                Result.success(User.fromMap(doc.id, doc.data!!))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Déconnecte l'utilisateur actuel.
     */
    fun signOut() {
        auth.signOut()
    }
}
