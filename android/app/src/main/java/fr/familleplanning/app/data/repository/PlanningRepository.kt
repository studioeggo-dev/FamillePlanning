package fr.familleplanning.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import fr.familleplanning.app.data.models.CalendarEvent
import fr.familleplanning.app.data.models.DateTimeUtils
import fr.familleplanning.app.data.models.InvalidDocumentException
import fr.familleplanning.app.firebase.FirebaseManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime

sealed class EventsSyncState {
    object Idle : EventsSyncState()
    object Loading : EventsSyncState()
    data class DemoMode(val events: List<CalendarEvent>) : EventsSyncState()
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
 * 5. Isolation stricte du mode démonstration local.
 */
class PlanningRepository(
    private val firestore: FirebaseFirestore = FirebaseManager.getFirestore()
) {
    private val TAG = "PlanningRepository"

    private val _syncState = MutableStateFlow<EventsSyncState>(EventsSyncState.Idle)
    val syncState: StateFlow<EventsSyncState> = _syncState.asStateFlow()

    private var activeListenerRegistration: ListenerRegistration? = null
    private var activeFamilyId: String? = null

    // Cache local pour le mode démonstration
    private val demoEventsList = mutableListOf<CalendarEvent>()

    init {
        initDemoEvents()
    }

    /**
     * Active l'écouteur en temps réel sur la collection d'événements du foyer.
     * Seul "local_demo" active le mode démo; null signifie aucun foyer réel associé.
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
        if (familyId == "local_demo") {
            _syncState.value = EventsSyncState.DemoMode(demoEventsList.toList())
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
     * Ajoute un événement dans Firestore (base nommée) ou en mode démonstration local.
     */
    suspend fun addEvent(familyId: String?, event: CalendarEvent): Result<Unit> {
        return if (familyId == "local_demo") {
            demoEventsList.add(event)
            _syncState.value = EventsSyncState.DemoMode(demoEventsList.toList())
            Result.success(Unit)
        } else if (familyId.isNullOrBlank()) {
            Result.failure(IllegalStateException("Aucun foyer associé. Créez votre famille avant d'ajouter un événement."))
        } else {
            try {
                firestore
                    .collection("families")
                    .document(familyId)
                    .collection("events")
                    .add(event.toFirestoreMap())
                    .await()

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Échec de création d'événement dans Firestore", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Met à jour un événement existant dans Firestore ou localement.
     */
    suspend fun updateEvent(familyId: String?, event: CalendarEvent): Result<Unit> {
        return if (familyId == "local_demo") {
            val index = demoEventsList.indexOfFirst { it.id == event.id }
            if (index != -1) {
                demoEventsList[index] = event
                _syncState.value = EventsSyncState.DemoMode(demoEventsList.toList())
            }
            Result.success(Unit)
        } else if (familyId.isNullOrBlank()) {
            Result.failure(IllegalStateException("Aucun foyer associé. Créez votre famille avant d'ajouter un événement."))
        } else {
            try {
                firestore
                    .collection("families")
                    .document(familyId)
                    .collection("events")
                    .document(event.id)
                    .set(event.toFirestoreMap(), SetOptions.merge())
                    .await()

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Échec de mise à jour d'événement ${event.id} dans Firestore", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Supprime un événement de Firestore ou localement.
     */
    suspend fun deleteEvent(familyId: String?, eventId: String): Result<Unit> {
        return if (familyId == "local_demo") {
            demoEventsList.removeAll { it.id == eventId }
            _syncState.value = EventsSyncState.DemoMode(demoEventsList.toList())
            Result.success(Unit)
        } else if (familyId.isNullOrBlank()) {
            Result.failure(IllegalStateException("Aucun foyer associé. Créez votre famille avant d'ajouter un événement."))
        } else {
            try {
                firestore
                    .collection("families")
                    .document(familyId)
                    .collection("events")
                    .document(eventId)
                    .delete()
                    .await()

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Échec de suppression d'événement $eventId dans Firestore", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Initialise le jeu de démonstration local isolée
     */
    private fun initDemoEvents() {
        val today = DateTimeUtils.todayInParis()
        val monday = DateTimeUtils.getMondayOfWeek(today)

        demoEventsList.clear()
        demoEventsList.addAll(
            listOf(
                CalendarEvent(
                    id = "demo_1",
                    familyId = "local_demo",
                    title = "Entraînement de Judo",
                    description = "Kimono propre et gourde",
                    startInstant = DateTimeUtils.toInstantFromParis(monday, LocalTime.of(17, 30)),
                    endInstant = DateTimeUtils.toInstantFromParis(monday, LocalTime.of(19, 0)),
                    participantIds = listOf("member_leo"),
                    category = "sport",
                    colorHex = "#1DD1A1",
                    location = "Dojo Municipal"
                ),
                CalendarEvent(
                    id = "demo_2",
                    familyId = "local_demo",
                    title = "Rendez-vous Pédiatre",
                    description = "Visite des 6 ans",
                    startInstant = DateTimeUtils.toInstantFromParis(monday.plusDays(1), LocalTime.of(10, 0)),
                    endInstant = DateTimeUtils.toInstantFromParis(monday.plusDays(1), LocalTime.of(10, 45)),
                    participantIds = listOf("member_emma", "member_sophie"),
                    category = "health",
                    colorHex = "#FF6B6B",
                    location = "Cabinet Dr. Martin"
                ),
                CalendarEvent(
                    id = "demo_3",
                    familyId = "local_demo",
                    title = "Cours de Piano",
                    description = "Morceau 4 à réviser",
                    startInstant = DateTimeUtils.toInstantFromParis(monday.plusDays(2), LocalTime.of(14, 0)),
                    endInstant = DateTimeUtils.toInstantFromParis(monday.plusDays(2), LocalTime.of(15, 0)),
                    participantIds = listOf("member_emma"),
                    category = "school",
                    colorHex = "#A55EEA",
                    location = "Conservatoire"
                )
            )
        )
    }
}
