package fr.familleplanning.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.familleplanning.app.data.models.Family
import fr.familleplanning.app.data.models.FamilyMember
import fr.familleplanning.app.data.repository.FamilyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FamilyUiState {
    object Loading : FamilyUiState()
    object NoFamily : FamilyUiState()
    data class Success(
        val family: Family,
        val members: List<FamilyMember>,
        val isRemoteSource: Boolean
    ) : FamilyUiState()
    data class Error(val message: String) : FamilyUiState()
}

class FamilyViewModel(
    private val familyRepository: FamilyRepository = FamilyRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<FamilyUiState>(FamilyUiState.Loading)
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = FamilyUiState.NoFamily
    }

    fun loadFamilyData(familyId: String?) {
        _uiState.value = FamilyUiState.Loading

        if (familyId.isNullOrBlank()) {
            _uiState.value = FamilyUiState.NoFamily
            return
        }

        viewModelScope.launch {
            val familyResult = familyRepository.getFamily(familyId)
            val membersResult = familyRepository.getFamilyMembers(familyId)

            if (familyResult.isSuccess && membersResult.isSuccess) {
                val family = familyResult.getOrNull()
                val members = membersResult.getOrNull() ?: emptyList()

                if (family != null) {
                    _uiState.value = FamilyUiState.Success(
                        family = family,
                        members = members,
                        isRemoteSource = true
                    )
                } else {
                    _uiState.value = FamilyUiState.Error("Foyer introuvable sur Cloud Firestore")
                }
            } else {
                val errorMsg = familyResult.exceptionOrNull()?.localizedMessage
                    ?: membersResult.exceptionOrNull()?.localizedMessage
                    ?: "Erreur lors de la récupération des données familiales"
                _uiState.value = FamilyUiState.Error(errorMsg)
            }
        }
    }

}
