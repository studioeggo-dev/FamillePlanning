package fr.familleplanning.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import fr.familleplanning.app.data.models.CalendarEvent
import fr.familleplanning.app.data.models.InvalidDocumentException
import fr.familleplanning.app.firebase.FirebaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

sealed class EventsSyncState {
    object Idle : EventsSyncState()
    object Loading : EventsSyncState()
    data class RealTimeSuccess(val familyId: String, val events: List<CalendarEvent>) : EventsSyncState()
    data class Error(val familyId: String?, val message: String) : EventsSyncState()
}

/**
 * Dépôt du planning avec synchronisation en temps réel sur Cloud Firestore (base nommée).
 *
 * ÉTAPE 3 - SYNCHRONISATION ANDROID/WEB :
 * 1. Écoute en temps réel via addSnapshotListener sur families/{familyId}/events.
 * 2. Création, modification et suppression d'événements.
 * 3. Filtrage multi-participants.
 * 4. Nettoyage automatique des écouteurs lors des déconnexions/changements de foyer.
 * 5. Aucun compte ni événement de démonstration dans l'application Android.
 */
class PlanningRepository(
    private val firestore: FirebaseFirestore = FirebaseManager.getFirestore()
) {
    private val TAG = "PlanningRepository"

    private val _syncState = MutableStateFlow<EventsSyncState>(EventsSyncState.Idle)
    val syncState: StateFlow<EventsSyncState> = _syncState.asStateFlow()

    private var activeListenerRegistration: ListenerRegistration? = null
    private var activeFamilyId: String? = null

    /**
     * Active la synchronisation uniquement si un vrai foyer est associé.
     * Jamais de données d'exemple substituées aux données Firestore.
     */
    fun startListeningToFamilyEvents(familyId: String?) {
        // Nettoyage préalable de l'écouteur précédent si le foyer change
        if (activeFamilyId != familyId) {
            stopListening()
        }

        activeFamilyId = familyId

        if (familyId.isNullOrBlank()) {
            _syncState.value = EventsSyncState.Idle
            return
        }

        _syncState.value = EventsSyncState.Loading

        try {
            val eventsRef = firestore
                .collection("families")
                .document(familyId)
                .collection("events")

            activeListenerRegistration = eventsRef.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Erreur lors de l'écoute Firestore en temps réel pour $familyId", error)
                    _syncState.value = EventsSyncState.Error(familyId, error.localizedMessage ?: "Erreur de synchronisation Firestore")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val events = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { map ->
                            try {
                                CalendarEvent.fromMap(doc.id, map)
                            } catch (e: InvalidDocumentException) {
                                Log.w(TAG, "Document d'événement ignoré car malformé: ${doc.id}", e)
                                null
                            }
                        }
                    }

                    Log.i(TAG, "Synchronisation temps réel réussie: ${events.size} événements reçus pour $familyId")
                    _syncState.value = EventsSyncState.RealTimeSuccess(familyId, events)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Impossible de démarrer l'écouteur Firestore", e)
            _syncState.value = EventsSyncState.Error(familyId, e.localizedMessage ?: "Erreur d'initialisation")
        }
    }

    /**
     * Arrête et nettoie l'écouteur Firestore en temps réel.
     */
    fun stopListening() {
        activeListenerRegistration?.remove()
        activeListenerRegistration = null
        activeFamilyId = null
        _syncState.value = EventsSyncState.Idle
    }

    /**
     * CRUD exclusivement distant, protégé par les règles Firestore existantes.
     */
    suspend fun addEvent(familyId: String?, event: CalendarEvent): Result<Unit> {
        if (familyId.isNullOrBlank()) return Result.failure(IllegalStateException("Créez d'abord votre famille."))
        return try {
            firestore.collection("families").document(familyId)
                .collection("events").add(event.toFirestoreMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Création d'événement refusée par Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun updateEvent(familyId: String?, event: CalendarEvent): Result<Unit> {
        if (familyId.isNullOrBlank()) return Result.failure(IllegalStateException("Créez d'abord votre famille."))
        return try {
            firestore.collection("families").document(familyId)
                .collection("events").document(event.id)
                .set(event.toFirestoreMap(), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Modification d'événement refusée par Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun deleteEvent(familyId: String?, eventId: String): Result<Unit> {
        if (familyId.isNullOrBlank()) return Result.failure(IllegalStateException("Créez d'abord votre famille."))
        return try {
            firestore.collection("families").document(familyId)
                .collection("events").document(eventId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Suppression d'événement refusée par Firestore", e)
            Result.failure(e)
        }
    }
}
