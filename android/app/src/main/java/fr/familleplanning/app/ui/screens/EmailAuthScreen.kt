package fr.familleplanning.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.familleplanning.app.viewmodel.AuthUiState

@Composable
fun EmailAuthScreen(
    uiState: AuthUiState,
    onSubmit: (String, String, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var register by rememberSaveable { mutableStateOf(false) }
    val busy = uiState is AuthUiState.Loading
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(if (register) "Créer mon compte" else "Connexion e-mail",
            style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Pour les adultes : accès au même foyer que sur Android et sur le Web.",
            style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(value = email, onValueChange = { email = it.trim() },
            label = { Text("Adresse e-mail") }, singleLine = true, enabled = !busy,
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { password = it },
            label = { Text("Mot de passe (6 caractères minimum)") },
            singleLine = true, enabled = !busy,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth())
        if (uiState is AuthUiState.Error) {
            Text(uiState.message, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = { onSubmit(email, password, register) },
            enabled = !busy && email.contains("@") && password.length >= 6,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (busy) CircularProgressIndicator()
            else Text(if (register) "Créer mon compte adulte" else "Me connecter")
        }
        OutlinedButton(onClick = { register = !register }, enabled = !busy) {
            Text(if (register) "J'ai déjà un compte" else "Créer un compte avec e-mail")
        }
        Text("Un message de vérification sera envoyé à toute nouvelle adresse. " +
            "Les enfants de moins de 13 ans n'ont pas de compte : ils sont ajoutés par un parent.")
        TextButton(onClick = onBack) { Text("Retour") }
    }
}
