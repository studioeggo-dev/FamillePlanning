package fr.familleplanning.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fr.familleplanning.app.ui.components.AppBottomBar
import fr.familleplanning.app.ui.screens.EmailAuthScreen
import fr.familleplanning.app.ui.screens.InviteScreen
import fr.familleplanning.app.ui.screens.FamilyScreen
import fr.familleplanning.app.ui.screens.FamilySetupScreen
import fr.familleplanning.app.ui.screens.PlanningScreen
import fr.familleplanning.app.ui.screens.SettingsScreen
import fr.familleplanning.app.ui.screens.WelcomeScreen
import fr.familleplanning.app.viewmodel.AuthUiState
import fr.familleplanning.app.viewmodel.AuthViewModel
import fr.familleplanning.app.viewmodel.FamilyUiState
import fr.familleplanning.app.viewmodel.FamilyViewModel
import fr.familleplanning.app.viewmodel.PlanningViewModel
import fr.familleplanning.app.viewmodel.SettingsViewModel

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel(),
    planningViewModel: PlanningViewModel = viewModel(),
    familyViewModel: FamilyViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    onTriggerGoogleSignIn: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Welcome.route

    val authState by authViewModel.uiState.collectAsState()
    val planningState by planningViewModel.uiState.collectAsState()
    val familyState by familyViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val familyCreationState by authViewModel.familyCreationState.collectAsState()
    val familyOperationBusy by familyViewModel.operationBusy.collectAsState()
    val familyOperationMessage by familyViewModel.operationMessage.collectAsState()
    val inviteToken by familyViewModel.inviteToken.collectAsState()
    val pendingInvitationToken by authViewModel.pendingInviteToken.collectAsState()
    val inviteUiState by authViewModel.inviteUiState.collectAsState()

    val authenticated = authState as? AuthUiState.Authenticated
    val activeFamilyId = authenticated?.profile?.activeFamilyId

    // Aucune donnée fictive : synchronisation exclusivement sur un vrai foyer Firebase.
    LaunchedEffect(authenticated?.firebaseUser?.uid, activeFamilyId) {
        if (authenticated != null) {
            planningViewModel.setActiveFamily(activeFamilyId)
            familyViewModel.loadFamilyData(activeFamilyId, authenticated.firebaseUser.uid)
        } else {
            planningViewModel.stopSync()
            familyViewModel.loadFamilyData(null)
        }
    }

    LaunchedEffect(pendingInvitationToken, authenticated?.firebaseUser?.uid, activeFamilyId, currentRoute) {
        if (!pendingInvitationToken.isNullOrBlank() && authenticated != null &&
            currentRoute != Screen.Invite.route) {
            navController.navigate(Screen.Invite.route) { launchSingleTop = true }
        } else if (pendingInvitationToken.isNullOrBlank() &&
            !activeFamilyId.isNullOrBlank() &&
            currentRoute in listOf(Screen.FamilySetup.route, Screen.Invite.route, Screen.EmailAuth.route)) {
            navController.navigate(Screen.Planning.route) {
                popUpTo(Screen.Welcome.route) { inclusive = true }
                launchSingleTop = true
            }
        } else if (authenticated != null && currentRoute == Screen.EmailAuth.route) {
            navController.navigate(
                if (activeFamilyId.isNullOrBlank()) Screen.FamilySetup.route else Screen.Planning.route
            ) {
                popUpTo(Screen.EmailAuth.route) { inclusive = true }
            }
        }
    }

    LaunchedEffect(pendingInvitationToken, currentRoute, authenticated?.firebaseUser?.uid) {
        if (currentRoute == Screen.Invite.route && authenticated != null &&
            !pendingInvitationToken.isNullOrBlank()) {
            authViewModel.previewInvitation()
        }
    }

    val familyName = when (familyState) {
        is FamilyUiState.Success -> (familyState as FamilyUiState.Success).family.name
        else -> "Ma famille"
    }

    LaunchedEffect(authenticated?.firebaseUser?.uid, activeFamilyId, familyName) {
        settingsViewModel.updateUserInfo(
            authenticated?.firebaseUser?.email,
            authenticated?.firebaseUser?.displayName,
            if (!activeFamilyId.isNullOrBlank()) familyName else "Aucun foyer associé",
            activeFamilyId
        )
    }

    val members = when (familyState) {
        is FamilyUiState.Success -> (familyState as FamilyUiState.Success).members
        else -> emptyList()
    }

    val showBottomBar = currentRoute !in listOf(
        Screen.Welcome.route, Screen.FamilySetup.route, Screen.EmailAuth.route, Screen.Invite.route
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Planning.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Welcome.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Écran 1: Bienvenue
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    uiState = authState,
                    onSignInWithGoogleClick = onTriggerGoogleSignIn,
                    onSignInWithEmailClick = { navController.navigate(Screen.EmailAuth.route) },
                    onContinueToAppClick = {
                        val destination = when {
                            !pendingInvitationToken.isNullOrBlank() -> Screen.Invite
                            activeFamilyId.isNullOrBlank() -> Screen.FamilySetup
                            else -> Screen.Planning
                        }
                        if (authenticated != null) {
                            navController.navigate(destination.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    },
                    onDismissError = { authViewModel.dismissError() }
                )
            }

            composable(Screen.EmailAuth.route) {
                EmailAuthScreen(
                    uiState = authState,
                    onSubmit = { email, password, register ->
                        authViewModel.signInWithEmail(email, password, register)
                    },
                    onBack = {
                        authViewModel.dismissError()
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Invite.route) {
                InviteScreen(
                    uiState = inviteUiState,
                    emailVerified = authenticated?.firebaseUser?.isEmailVerified == true,
                    onRefresh = { authViewModel.previewInvitation() },
                    onCheckEmail = { authViewModel.refreshEmailVerification() },
                    onAccept = { name, color -> authViewModel.acceptInvitation(name, color) },
                    onCancel = {
                        authViewModel.cancelInvitation()
                        navController.navigate(
                            if (activeFamilyId.isNullOrBlank()) Screen.FamilySetup.route else Screen.Planning.route
                        ) { popUpTo(Screen.Invite.route) { inclusive = true } }
                    }
                )
            }

            // Création réelle du premier foyer, jamais une démo automatique.
            composable(Screen.FamilySetup.route) {
                FamilySetupScreen(
                    creationState = familyCreationState,
                    onCreateFamily = { name, firstName, color ->
                        authViewModel.createFamily(name, firstName, color)
                    },
                    onRefreshProfile = { authViewModel.refreshProfile() },
                    onSignOut = {
                        planningViewModel.stopSync()
                        authViewModel.signOut()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Écran 2: Planning (avec synchronisation Firestore temps réel et CRUD)
            composable(Screen.Planning.route) {
                PlanningScreen(
                    uiState = planningState,
                    members = members,
                    familyName = familyName,
                    onPreviousDateClick = { planningViewModel.goToPrevious() },
                    onNextDateClick = { planningViewModel.goToNext() },
                    onTodayClick = { planningViewModel.goToToday() },
                    onDateSelected = { planningViewModel.selectDate(it) },
                    onViewModeChange = { planningViewModel.setViewMode(it) },
                    onParticipantToggle = { planningViewModel.toggleParticipantFilter(it) },
                    onCreateEvent = { planningViewModel.createEvent(it) },
                    onUpdateEvent = { planningViewModel.updateEvent(it) },
                    onDeleteEvent = { planningViewModel.deleteEvent(it) }
                )
            }

            // Écran 3: Ma famille
            composable(Screen.Family.route) {
                FamilyScreen(
                    uiState = familyState,
                    operationBusy = familyOperationBusy,
                    operationMessage = familyOperationMessage,
                    inviteToken = inviteToken,
                    onRefreshClick = { familyViewModel.refresh() },
                    onAddChild = { firstName, birthDate, color ->
                        familyViewModel.addChild(firstName, birthDate, color)
                    },
                    onCreateInvite = { role, email -> familyViewModel.createInvite(role, email) },
                    onDismissInvite = { familyViewModel.clearInvite() }
                )
            }

            // Écran 4: Paramètres
            composable(Screen.Settings.route) {
                SettingsScreen(
                    uiState = settingsState,
                    onSignOutClick = {
                        planningViewModel.stopSync()
                        authViewModel.signOut()
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onToggleDefaultViewMode = { mode ->
                        settingsViewModel.setDefaultViewMode(mode)
                    }
                )
            }
        }
    }
}
