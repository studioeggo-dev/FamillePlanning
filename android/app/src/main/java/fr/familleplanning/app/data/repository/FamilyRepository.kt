package fr.familleplanning.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import fr.familleplanning.app.data.models.Family
import fr.familleplanning.app.data.models.FamilyAccess
import fr.familleplanning.app.data.models.FamilyMember
import fr.familleplanning.app.firebase.FirebaseManager
import kotlinx.coroutines.tasks.await

/**
 * Dépôt pour la lecture des informations du foyer familial.
 *
 * RÈGLE STRICTE PHASE 3A :
 * - Lecture autorisée du foyer existant.
 * - Récupération des membres du foyer.
 * - Ne pas créer de famille fictive dans Firestore.
 */
class FamilyRepository(
    private val firestore: FirebaseFirestore = FirebaseManager.getFirestore()
) {

    /**
     * Récupère les données du foyer familial: families/{familyId}
     */
    suspend fun getFamily(familyId: String): Result<Family?> {
        return try {
            val snapshot = firestore
                .collection("families")
                .document(familyId)
                .get()
                .await()

            if (snapshot.exists() && snapshot.data != null) {
                Result.success(Family.fromMap(snapshot.id, snapshot.data!!))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Récupère la liste des membres du foyer: families/{familyId}/members
     */
    suspend fun getFamilyMembers(familyId: String): Result<List<FamilyMember>> {
        return try {
            val querySnapshot = firestore
                .collection("families")
                .document(familyId)
                .collection("members")
                .get()
                .await()

            val members = querySnapshot.documents.mapNotNull { doc ->
                doc.data?.let { map ->
                    try {
                        FamilyMember.fromMap(doc.id, map)
                    } catch (e: Exception) {
                        null // Fichier/document malformé ignoré en toute sécurité
                    }
                }
            }
            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Vérifie le niveau d'accès de l'utilisateur: families/{familyId}/access/{uid}
     */
    suspend fun getFamilyAccess(familyId: String, uid: String): Result<FamilyAccess?> {
        return try {
            val doc = firestore
                .collection("families")
                .document(familyId)
                .collection("access")
                .document(uid)
                .get()
                .await()

            if (doc.exists() && doc.data != null) {
                Result.success(FamilyAccess.fromMap(doc.id, doc.data!!))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
