package fr.familleplanning.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import fr.familleplanning.app.data.models.User
import fr.familleplanning.app.data.repository.AuthRepository
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

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Restauration automatique de session au démarrage
        checkExistingSession()
    }

    private fun checkExistingSession() {
        val currentFirebaseUser = authRepository.currentUser
        if (currentFirebaseUser != null) {
            _uiState.value = AuthUiState.Loading
            viewModelScope.launch {
                val profileResult = authRepository.getUserProfile(currentFirebaseUser.uid)
                _uiState.value = AuthUiState.Authenticated(
                    firebaseUser = currentFirebaseUser,
                    profile = profileResult.getOrNull()
                )
            }
        } else {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun signInWithGoogle(credential: AuthCredential) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signInWithGoogleCredential(credential)
            result.onSuccess { firebaseUser ->
                val profileResult = authRepository.getUserProfile(firebaseUser.uid)
                _uiState.value = AuthUiState.Authenticated(
                    firebaseUser = firebaseUser,
                    profile = profileResult.getOrNull()
                )
            }.onFailure { error ->
                _uiState.value = AuthUiState.Error(
                    error.localizedMessage ?: "Échec de connexion avec le compte Google"
                )
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = AuthUiState.Idle
    }

    fun dismissError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
