package fr.familleplanning.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import fr.familleplanning.app.data.models.User
import fr.familleplanning.app.data.repository.AuthRepository
import fr.familleplanning.app.data.repository.FamilyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Authenticated(val firebaseUser: FirebaseUser, val profile: User?) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

sealed class FamilyCreationState {
    object Idle : FamilyCreationState()
    object Saving : FamilyCreationState()
    data class Error(val message: String) : FamilyCreationState()
}

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val familyRepository: FamilyRepository = FamilyRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _familyCreationState = MutableStateFlow<FamilyCreationState>(FamilyCreationState.Idle)
    val familyCreationState: StateFlow<FamilyCreationState> = _familyCreationState.asStateFlow()

    init {
        // Restauration automatique de session au démarrage
        checkExistingSession()
    }

    private fun checkExistingSession() {
        val currentFirebaseUser = authRepository.currentUser
        if (currentFirebaseUser != null) {
            _uiState.value = AuthUiState.Loading
            viewModelScope.launch {
                loadProfile(currentFirebaseUser)
            }
        } else {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun beginGoogleSignIn() {
        _uiState.value = AuthUiState.Loading
    }

    fun cancelGoogleSignIn() {
        _uiState.value = AuthUiState.Idle
    }

    fun reportGoogleSignInError(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }

    fun signInWithGoogle(credential: AuthCredential) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signInWithGoogleCredential(credential)
            result.onSuccess { firebaseUser ->
                loadProfile(firebaseUser)
            }.onFailure { error ->
                _uiState.value = AuthUiState.Error(
                    error.localizedMessage ?: "Échec de connexion avec le compte Google"
                )
            }
        }
    }

    private suspend fun loadProfile(user: FirebaseUser) {
        val profileResult = authRepository.getUserProfile(user.uid)
        profileResult.onSuccess { profile ->
            _uiState.value = AuthUiState.Authenticated(user, profile)
        }.onFailure {
            _uiState.value = AuthUiState.Error(
                "Votre compte Google est connecté, mais le profil familial est inaccessible. Vérifiez votre connexion et réessayez."
            )
        }
    }

    fun refreshProfile() {
        val user = authRepository.currentUser ?: return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch { loadProfile(user) }
    }

    fun createFamily(familyName: String, memberName: String, color: String) {
        val auth = _uiState.value as? AuthUiState.Authenticated ?: return
        if (_familyCreationState.value is FamilyCreationState.Saving) return
        if (!auth.profile?.activeFamilyId.isNullOrBlank()) {
            _familyCreationState.value = FamilyCreationState.Error(
                "Ce compte est déjà associé à un foyer. Actualisez vos informations."
            )
            return
        }
        _familyCreationState.value = FamilyCreationState.Saving
        viewModelScope.launch {
            val result = familyRepository.createFirstFamily(auth.firebaseUser, familyName, memberName, color)
            result.onSuccess { newId ->
                val profile = authRepository.getUserProfile(auth.firebaseUser.uid).getOrNull()
                _uiState.value = AuthUiState.Authenticated(
                    auth.firebaseUser,
                    (profile ?: fr.familleplanning.app.data.models.User(
                        uid = auth.firebaseUser.uid,
                        email = auth.firebaseUser.email ?: "",
                        displayName = auth.firebaseUser.displayName ?: memberName
                    )).copy(activeFamilyId = newId)
                )
                _familyCreationState.value = FamilyCreationState.Idle
            }.onFailure { e ->
                _familyCreationState.value = FamilyCreationState.Error(
                    "Impossible de créer ce foyer : ${e.localizedMessage ?: "accès refusé"}. Aucune famille de démonstration n'a été créée."
                )
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _familyCreationState.value = FamilyCreationState.Idle
        _uiState.value = AuthUiState.Idle
    }

    fun dismissError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
