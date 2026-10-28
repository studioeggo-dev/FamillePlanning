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

    LaunchedEffect(authenticated?.firebaseUser?.uid, activeFamilyId, currentRoute) {
        if (!activeFamilyId.isNullOrBlank() && currentRoute == Screen.FamilySetup.route) {
            navController.navigate(Screen.Planning.route) {
                popUpTo(Screen.FamilySetup.route) { inclusive = true }
                launchSingleTop = true
            }
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

    val showBottomBar = currentRoute != Screen.Welcome.route && currentRoute != Screen.FamilySetup.route

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
                    onContinueToAppClick = {
                        val destination = if (activeFamilyId.isNullOrBlank()) Screen.FamilySetup else Screen.Planning
                        if (authenticated != null) {
                            navController.navigate(destination.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    },
                    onDismissError = { authViewModel.dismissError() }
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
