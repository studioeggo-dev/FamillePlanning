package fr.familleplanning.app.viewmodel

import androidx.lifecycle.ViewModel
import fr.familleplanning.app.data.models.DateTimeUtils
import fr.familleplanning.app.firebase.FirebaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val userEmail: String? = null,
    val userName: String? = null,
    val activeFamilyName: String = "Aucun foyer associé",
    val activeFamilyId: String? = null,
    val defaultViewMode: PlanningViewMode = PlanningViewMode.WEEK,
    val firstDayOfWeek: String = "Lundi",
    val timezone: String = "Europe/Paris",
    val appVersion: String = "1.0.1 (Foyer familial)",
    val firebaseProjectId: String = FirebaseManager.PROJECT_ID,
    val firebaseDatabaseId: String = FirebaseManager.DATABASE_ID
)

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateUserInfo(email: String?, name: String?, familyName: String?, familyId: String?) {
        _uiState.value = _uiState.value.copy(
            userEmail = email,
            userName = name,
            activeFamilyName = familyName ?: "Aucun foyer associé",
            activeFamilyId = familyId
        )
    }

    fun setDefaultViewMode(mode: PlanningViewMode) {
        _uiState.value = _uiState.value.copy(defaultViewMode = mode)
    }
}
