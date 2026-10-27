package fr.familleplanning.app

import android.os.Bundle
import android.app.Activity
import android.util.Log
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

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                authViewModel.reportGoogleSignInError(
                    "Google n'a pas transmis les informations de connexion. Vérifiez la configuration de votre compte."
                )
            } else {
                authViewModel.signInWithGoogle(GoogleAuthProvider.getCredential(idToken, null))
            }
        } catch (e: ApiException) {
            Log.e("FamillePlanningAuth", "Google Sign-In status code: ${e.statusCode}", e)
            if (e.statusCode == 12501 && result.resultCode == Activity.RESULT_CANCELED) {
                authViewModel.cancelGoogleSignIn()
            } else {
                authViewModel.reportGoogleSignInError(
                    "Connexion Google impossible (code ${e.statusCode}). Vérifiez la configuration de l'application."
                )
            }
        } catch (e: Exception) {
            Log.e("FamillePlanningAuth", "Google Sign-In unexpected error", e)
            authViewModel.reportGoogleSignInError(
                "Impossible de terminer la connexion Google. Réessayez."
            )
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
        // The Google Services Gradle plugin generates this resource from the
        // real OAuth Web client (client_type = 3) in google-services.json.
        val clientResId = resources.getIdentifier(
            "default_web_client_id", "string", packageName
        )
        if (clientResId == 0) {
            authViewModel.reportGoogleSignInError(
                "La connexion Google n'est pas encore configurée pour cette application."
            )
            return
        }
        val webClientId = getString(clientResId)
        if (webClientId.isBlank()) {
            authViewModel.reportGoogleSignInError(
                "La configuration de connexion Google est incomplète."
            )
            return
        }
        authViewModel.beginGoogleSignIn()
        try {
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(this, options)
            googleSignInLauncher.launch(client.signInIntent)
        } catch (e: Exception) {
            Log.e("FamillePlanningAuth", "Unable to launch Google Sign-In", e)
            authViewModel.reportGoogleSignInError(
                "Impossible d'ouvrir la connexion Google sur ce téléphone."
            )
        }
    }
}
