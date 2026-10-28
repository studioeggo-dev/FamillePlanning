package fr.familleplanning.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
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

sealed class InviteUiState {
    object Idle : InviteUiState()
    object Loading : InviteUiState()
    data class Preview(val familyName: String) : InviteUiState()
    data class Error(val message: String) : InviteUiState()
    object Accepted : InviteUiState()
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

    private val _pendingInviteToken = MutableStateFlow<String?>(null)
    val pendingInviteToken: StateFlow<String?> = _pendingInviteToken.asStateFlow()
    private val _inviteUiState = MutableStateFlow<InviteUiState>(InviteUiState.Idle)
    val inviteUiState: StateFlow<InviteUiState> = _inviteUiState.asStateFlow()

    fun setPendingInvite(token: String) {
        if (!Regex("^[A-Za-z0-9_-]{43}$").matches(token)) {
            _inviteUiState.value = InviteUiState.Error("Ce lien d'invitation est invalide.")
            return
        }
        _pendingInviteToken.value = token
        _inviteUiState.value = InviteUiState.Idle
    }

    fun cancelInvitation() {
        _pendingInviteToken.value = null
        _inviteUiState.value = InviteUiState.Idle
    }

    fun previewInvitation() {
        val token = _pendingInviteToken.value ?: return
        if (_uiState.value !is AuthUiState.Authenticated) return
        _inviteUiState.value = InviteUiState.Loading
        viewModelScope.launch {
            familyRepository.previewInvite(token)
                .onSuccess { _inviteUiState.value = InviteUiState.Preview(it) }
                .onFailure {
                    _inviteUiState.value = InviteUiState.Error(
                        "Impossible de lire cette invitation : ${it.localizedMessage ?: "fonction Firebase non disponible"}"
                    )
                }
        }
    }

    fun acceptInvitation(firstName: String, color: String) {
        val token = _pendingInviteToken.value ?: return
        val auth = _uiState.value as? AuthUiState.Authenticated ?: return
        if (!auth.firebaseUser.isEmailVerified) {
            _inviteUiState.value = InviteUiState.Error(
                "Vérifie ton adresse e-mail avant de rejoindre cette famille."
            )
            return
        }
        _inviteUiState.value = InviteUiState.Loading
        viewModelScope.launch {
            familyRepository.acceptInvite(token, firstName, color)
                .onSuccess {
                    _pendingInviteToken.value = null
                    loadProfile(auth.firebaseUser)
                    _inviteUiState.value = InviteUiState.Accepted
                }
                .onFailure {
                    _inviteUiState.value = InviteUiState.Error(
                        "Invitation refusée : ${it.localizedMessage ?: "réessaie"}"
                    )
                }
        }
    }

    fun signInWithEmail(email: String, password: String, register: Boolean) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            authRepository.signInWithEmail(email, password, register)
                .onSuccess { user -> loadProfile(user) }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error(
                        error.localizedMessage ?: "Impossible de se connecter par e-mail."
                    )
                }
        }
    }

    fun refreshEmailVerification() {
        val previous = _uiState.value as? AuthUiState.Authenticated ?: return
        viewModelScope.launch {
            authRepository.reloadCurrentUser().onSuccess {
                _uiState.value = previous.copy(firebaseUser = it)
            }.onFailure {
                _inviteUiState.value = InviteUiState.Error(
                    "Impossible de vérifier l'adresse e-mail. Réessaie."
                )
            }
        }
    }

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
        if (!auth.firebaseUser.isEmailVerified) {
            _familyCreationState.value = FamilyCreationState.Error(
                "Vérifie ton adresse e-mail avant de créer une famille."
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
                    if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        "La connexion Google est active, mais Firestore refuse la création du foyer. " +
                        "Les règles d'accès à la base familiale doivent être vérifiées. Aucune donnée n'a été enregistrée."
                    } else {
                        "Impossible de créer la famille : ${e.localizedMessage ?: "réessayez"}."
                    }
                )
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _pendingInviteToken.value = null
        _inviteUiState.value = InviteUiState.Idle
        _familyCreationState.value = FamilyCreationState.Idle
        _uiState.value = AuthUiState.Idle
    }

    fun dismissError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}
