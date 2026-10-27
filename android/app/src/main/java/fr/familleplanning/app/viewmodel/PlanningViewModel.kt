package fr.familleplanning.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.familleplanning.app.data.models.CalendarEvent
import fr.familleplanning.app.data.models.DateTimeUtils
import fr.familleplanning.app.data.repository.EventsSyncState
import fr.familleplanning.app.data.repository.PlanningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class PlanningViewMode {
    WEEK,
    MONTH
}

data class PlanningUiState(
    val selectedDate: LocalDate = DateTimeUtils.todayInParis(),
    val viewMode: PlanningViewMode = PlanningViewMode.WEEK,
    val selectedParticipantId: String? = null,
    val activeFamilyId: String? = null,
    val syncState: EventsSyncState = EventsSyncState.Idle,
    val events: List<CalendarEvent> = emptyList(),
    val filteredEvents: List<CalendarEvent> = emptyList(),
    val weekDays: List<LocalDate> = emptyList(),
    val isOperationPending: Boolean = false,
    val userErrorMessage: String? = null
)

private data class PlanningSelection(
    val date: LocalDate,
    val mode: PlanningViewMode,
    val participantId: String?,
    val familyId: String?
)

class PlanningViewModel(
    private val planningRepository: PlanningRepository = PlanningRepository()
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(DateTimeUtils.todayInParis())
    private val _viewMode = MutableStateFlow(PlanningViewMode.WEEK)
    private val _selectedParticipantId = MutableStateFlow<String?>(null)
    private val _activeFamilyId = MutableStateFlow<String?>(null)
    private val _isOperationPending = MutableStateFlow(false)
    private val _userErrorMessage = MutableStateFlow<String?>(null)

    private val selectionFlow = combine(
        _selectedDate,
        _viewMode,
        _selectedParticipantId,
        _activeFamilyId
    ) { date, mode, participantId, familyId ->
        PlanningSelection(date, mode, participantId, familyId)
    }

    private val operationFlow = combine(
        _isOperationPending,
        _userErrorMessage
    ) { isPending, errorMsg -> Pair(isPending, errorMsg) }

    val uiState: StateFlow<PlanningUiState> = combine(
        selectionFlow,
        planningRepository.syncState,
        operationFlow
    ) { selection, syncState, operation ->
        val (date, mode, participantId, familyId) = selection
        val (isPending, errorMsg) = operation
        val weekDays = DateTimeUtils.getDaysOfWeek(date)

        val currentEvents = when (syncState) {
            is EventsSyncState.DemoMode -> syncState.events
            is EventsSyncState.RealTimeSuccess -> syncState.events
            else -> emptyList()
        }

        val filtered = currentEvents.filter { event ->
            val matchParticipant = participantId == null || event.participantIds.contains(participantId)

            // Filtrage par couverture de la journée (multi-jours et événements réguliers)
            val matchTime = when (mode) {
                PlanningViewMode.WEEK -> weekDays.any { day ->
                    DateTimeUtils.isSpanningDay(event.startInstant, event.endInstant, day)
                }
                PlanningViewMode.MONTH -> {
                    val startZdt = DateTimeUtils.toParisDateTime(event.startInstant)
                    startZdt.year == date.year && startZdt.month == date.month
                }
            }
            matchParticipant && matchTime
        }

        PlanningUiState(
            selectedDate = date,
            viewMode = mode,
            selectedParticipantId = participantId,
            activeFamilyId = familyId,
            syncState = syncState,
            events = currentEvents,
            filteredEvents = filtered,
            weekDays = weekDays,
            isOperationPending = isPending,
            userErrorMessage = errorMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlanningUiState(
            selectedDate = DateTimeUtils.todayInParis(),
            weekDays = DateTimeUtils.getDaysOfWeek(DateTimeUtils.todayInParis())
        )
    )

    /**
     * Définit le foyer actif et démarre l'écouteur Firestore en temps réel.
     */
    fun setActiveFamily(familyId: String?) {
        _activeFamilyId.value = familyId
        planningRepository.startListeningToFamilyEvents(familyId)
    }

    /**
     * Arrête l'écouteur Firestore lors d'un changement de foyer ou d'une déconnexion.
     */
    fun stopSync() {
        planningRepository.stopListening()
        _activeFamilyId.value = null
    }

    /**
     * Création d'un événement
     */
    fun createEvent(event: CalendarEvent) {
        _isOperationPending.value = true
        _userErrorMessage.value = null

        viewModelScope.launch {
            val result = planningRepository.addEvent(_activeFamilyId.value, event)
            _isOperationPending.value = false
            result.onFailure { error ->
                _userErrorMessage.value = "Échec de création de l'événement: ${error.localizedMessage}"
            }
        }
    }

    /**
     * Modification d'un événement
     */
    fun updateEvent(event: CalendarEvent) {
        _isOperationPending.value = true
        _userErrorMessage.value = null

        viewModelScope.launch {
            val result = planningRepository.updateEvent(_activeFamilyId.value, event)
            _isOperationPending.value = false
            result.onFailure { error ->
                _userErrorMessage.value = "Échec de mise à jour: ${error.localizedMessage}"
            }
        }
    }

    /**
     * Suppression d'un événement
     */
    fun deleteEvent(eventId: String) {
        _isOperationPending.value = true
        _userErrorMessage.value = null

        viewModelScope.launch {
            val result = planningRepository.deleteEvent(_activeFamilyId.value, eventId)
            _isOperationPending.value = false
            result.onFailure { error ->
                _userErrorMessage.value = "Échec de suppression: ${error.localizedMessage}"
            }
        }
    }

    fun dismissError() {
        _userErrorMessage.value = null
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setViewMode(mode: PlanningViewMode) {
        _viewMode.value = mode
    }

    fun toggleParticipantFilter(participantId: String) {
        _selectedParticipantId.value = if (_selectedParticipantId.value == participantId) null else participantId
    }

    fun clearParticipantFilter() {
        _selectedParticipantId.value = null
    }

    fun goToToday() {
        _selectedDate.value = DateTimeUtils.todayInParis()
    }

    fun goToPrevious() {
        _selectedDate.value = when (_viewMode.value) {
            PlanningViewMode.WEEK -> _selectedDate.value.minusWeeks(1)
            PlanningViewMode.MONTH -> _selectedDate.value.minusMonths(1)
        }
    }

    fun goToNext() {
        _selectedDate.value = when (_viewMode.value) {
            PlanningViewMode.WEEK -> _selectedDate.value.plusWeeks(1)
            PlanningViewMode.MONTH -> _selectedDate.value.plusMonths(1)
        }
    }

    override fun onCleared() {
        super.onCleared()
        planningRepository.stopListening()
    }
}
