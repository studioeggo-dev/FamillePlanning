package fr.familleplanning.app.data.repository

import com.google.firebase.auth.FirebaseUser
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
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
    private val firestore: FirebaseFirestore = FirebaseManager.getFirestore(),
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("europe-west9")
) {

    /**
     * Crée le foyer et ses droits owner dans un unique batch.
     * Les règles Firestore côté serveur sont l'autorité finale.
     * Aucune élévation de privilège locale, aucune écriture démo.
     */
    suspend fun createFirstFamily(
        signedInUser: FirebaseUser,
        familyName: String,
        memberName: String,
        memberColor: String
    ): Result<String> {
        return try {
            val name = familyName.trim()
            val firstName = memberName.trim()
            require(name.length in 2..80) { "Le nom de famille doit contenir entre 2 et 80 caractères." }
            require(firstName.length in 2..60) { "Indiquez votre prénom (2 à 60 caractères)." }
            require(Regex("^#[0-9A-Fa-f]{6}$").matches(memberColor)) { "Couleur invalide." }

            val uid = signedInUser.uid
            val userRef = firestore.collection("users").document(uid)
            val existingUser = userRef.get().await()
            val linkedFamily = existingUser.getString("currentFamilyId")
            if (!linkedFamily.isNullOrBlank()) {
                throw IllegalStateException("Un foyer existe déjà. Actualisez votre compte pour le retrouver.")
            }

            val familyRef = firestore.collection("families").document()
            val familyId = familyRef.id
            val accessRef = familyRef.collection("access").document(uid)
            val memberRef = familyRef.collection("members").document()
            val now = Timestamp.now()
            val batch = firestore.batch()

            batch.set(familyRef, mapOf(
                "name" to name,
                "ownerId" to uid,
                "timezone" to "Europe/Paris",
                "createdAt" to now
            ))
            batch.set(accessRef, mapOf(
                "role" to "owner",
                "joinedAt" to now
            ))
            batch.set(memberRef, mapOf(
                "name" to firstName,
                "userId" to uid,
                "role" to "parent",
                "roleLabel" to "Parent",
                "color" to memberColor,
                "avatarEmoji" to "😊"
            ))
            if (existingUser.exists()) {
                batch.update(userRef,
                    "currentFamilyId", familyId,
                    "familyIds", FieldValue.arrayUnion(familyId),
                    "updatedAt", now
                )
            } else {
                batch.set(userRef, mapOf(
                    "id" to uid,
                    "displayName" to (signedInUser.displayName ?: firstName),
                    "email" to (signedInUser.email ?: ""),
                    "photoUrl" to signedInUser.photoUrl?.toString(),
                    "currentFamilyId" to familyId,
                    "familyIds" to listOf(familyId),
                    "createdAt" to now,
                    "updatedAt" to now
                ))
            }
            batch.commit().await()
            Result.success(familyId)
        } catch (e: Exception) {
            Log.e("FamilyRepository", "Création du foyer refusée (vérifier les règles Firestore de la base nommée)", e)
            Result.failure(e)
        }
    }

    /**
     * Les opérations sensibles sont traitées côté serveur sur la base Firestore nommée.
     * L'interface ne peut jamais s'attribuer localement un rôle de parent.
     */
    suspend fun addChild(
        familyId: String, firstName: String, birthDate: String?, color: String
    ): Result<String> = try {
        val result = functions.getHttpsCallable("addFamilyChild").call(
            mapOf("familyId" to familyId, "firstName" to firstName,
                "birthDate" to (birthDate?.takeIf { it.isNotBlank() }),
                "color" to color)
        ).await()
        val memberId = (result.getData() as? Map<*, *>)?.get("memberId") as? String
            ?: throw IllegalStateException("Réponse serveur incomplète")
        Result.success(memberId)
    } catch (e: Exception) {
        Log.e("FamilyRepository", "Ajout d'enfant impossible", e)
        Result.failure(e)
    }

    suspend fun createInvite(
        familyId: String, role: String, email: String?
    ): Result<String> = try {
        val response = functions.getHttpsCallable("createFamilyInvite").call(
            mapOf("familyId" to familyId, "role" to role,
                "email" to (email?.takeIf { it.isNotBlank() }))
        ).await()
        val token = (response.getData() as? Map<*, *>)?.get("token") as? String
            ?: throw IllegalStateException("Invitation introuvable dans la réponse du serveur")
        Result.success(token)
    } catch (e: Exception) {
        Log.e("FamilyRepository", "Impossible de créer le lien", e)
        Result.failure(e)
    }

    suspend fun previewInvite(token: String): Result<String> = try {
        val response = functions.getHttpsCallable("previewFamilyInvite")
            .call(mapOf("token" to token)).await()
        val data = response.getData() as? Map<*, *>
        Result.success(data?.get("familyName") as? String ?: "Famille")
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun acceptInvite(
        token: String, firstName: String, color: String
    ): Result<String> = try {
        val response = functions.getHttpsCallable("acceptFamilyInvite")
            .call(mapOf("token" to token, "firstName" to firstName, "color" to color)).await()
        val familyId = (response.getData() as? Map<*, *>)?.get("familyId") as? String
            ?: throw IllegalStateException("Réponse serveur incomplète")
        Result.success(familyId)
    } catch (e: Exception) {
        Result.failure(e)
    }

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
