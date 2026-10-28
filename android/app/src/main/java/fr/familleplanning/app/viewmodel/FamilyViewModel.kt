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
        val isRemoteSource: Boolean,
        val accessRole: String
    ) : FamilyUiState()
    data class Error(val message: String) : FamilyUiState()
}

class FamilyViewModel(
    private val familyRepository: FamilyRepository = FamilyRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<FamilyUiState>(FamilyUiState.NoFamily)
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    private val _operationBusy = MutableStateFlow(false)
    val operationBusy: StateFlow<Boolean> = _operationBusy.asStateFlow()
    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()
    private val _inviteToken = MutableStateFlow<String?>(null)
    val inviteToken: StateFlow<String?> = _inviteToken.asStateFlow()

    private var familyId: String? = null
    private var currentUid: String? = null

    fun loadFamilyData(id: String?, uid: String? = null) {
        familyId = id
        currentUid = uid
        if (id.isNullOrBlank() || uid.isNullOrBlank()) {
            _uiState.value = FamilyUiState.NoFamily
            return
        }
        _uiState.value = FamilyUiState.Loading
        viewModelScope.launch {
            val family = familyRepository.getFamily(id)
            val members = familyRepository.getFamilyMembers(id)
            val access = familyRepository.getFamilyAccess(id, uid)
            if (family.isFailure || members.isFailure || access.isFailure) {
                _uiState.value = FamilyUiState.Error(
                    family.exceptionOrNull()?.localizedMessage
                        ?: members.exceptionOrNull()?.localizedMessage
                        ?: access.exceptionOrNull()?.localizedMessage
                        ?: "Impossible de récupérer votre famille"
                )
                return@launch
            }
            val home = family.getOrNull()
            if (home == null) {
                _uiState.value = FamilyUiState.Error("Famille introuvable")
                return@launch
            }
            val role = access.getOrNull()?.role
                ?: if (home.ownerId == uid) "owner" else "viewer"
            _uiState.value = FamilyUiState.Success(home, members.getOrNull() ?: emptyList(), true, role)
        }
    }

    fun refresh() = loadFamilyData(familyId, currentUid)

    fun addChild(name: String, birthDate: String?, color: String) {
        val id = familyId ?: return
        if (_operationBusy.value) return
        _operationBusy.value = true
        _operationMessage.value = null
        viewModelScope.launch {
            familyRepository.addChild(id, name, birthDate, color)
                .onSuccess {
                    _operationMessage.value = "Profil enfant ajouté à la famille."
                    loadFamilyData(id, currentUid)
                }
                .onFailure {
                    _operationMessage.value = "Ajout impossible : ${it.localizedMessage ?: "vérifie les fonctions Firebase"}"
                }
            _operationBusy.value = false
        }
    }

    fun createInvite(role: String, email: String?) {
        val id = familyId ?: return
        if (_operationBusy.value) return
        _operationBusy.value = true
        _operationMessage.value = null
        _inviteToken.value = null
        viewModelScope.launch {
            familyRepository.createInvite(id, role, email)
                .onSuccess { _inviteToken.value = it }
                .onFailure {
                    _operationMessage.value = "Invitation impossible : ${it.localizedMessage ?: "fonctions Firebase non déployées"}"
                }
            _operationBusy.value = false
        }
    }

    fun clearInvite() { _inviteToken.value = null }
    fun clearMessage() { _operationMessage.value = null }
}
