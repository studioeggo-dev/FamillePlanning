package fr.familleplanning.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import fr.familleplanning.app.navigation.AppNavHost
import fr.familleplanning.app.ui.theme.FamillePlanningTheme
import fr.familleplanning.app.viewmodel.AuthViewModel

/**
 * Activité unique (Single Activity Architecture) de FamillePlanning.
 */
class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    // Enregistrement du résultat du flux Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                authViewModel.signInWithGoogle(credential)
            }
        } catch (e: Exception) {
            // L'erreur est prise en charge par AuthViewModel
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FamillePlanningTheme {
                AppNavHost(
                    authViewModel = authViewModel,
                    onTriggerGoogleSignIn = { launchGoogleSignIn() }
                )
            }
        }
    }

    private fun launchGoogleSignIn() {
        // Configuration Google Sign-In avec web client id de Firebase
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("763272373321-familleplanning-web.apps.googleusercontent.com")
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }
}
