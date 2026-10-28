package fr.familleplanning.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.familleplanning.app.ui.theme.CoralPrimary
import fr.familleplanning.app.ui.theme.TextPrimary
import fr.familleplanning.app.ui.theme.TextSecondary
import fr.familleplanning.app.viewmodel.FamilyCreationState

/**
 * L'onboarding est réservé aux comptes authentifiés sans foyer associé.
 * Il ne crée rien dans Firestore avant validation explicite de l'utilisateur.
 */
@Composable
fun FamilySetupScreen(
    creationState: FamilyCreationState,
    onCreateFamily: (String, String, String) -> Unit,
    onRefreshProfile: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var familyName by rememberSaveable { mutableStateOf("") }
    var firstName by rememberSaveable { mutableStateOf("") }
    var selectedColor by rememberSaveable { mutableStateOf("#FF6B6B") }
    val colors = listOf("#FF6B6B", "#48DBFB", "#1DD1A1", "#FECA57", "#A55EEA", "#FF9F43")
    val saving = creationState is FamilyCreationState.Saving

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Créons votre famille !",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Bienvenue sur FamillePlanning. Un premier foyer permet de retrouver votre planning sur le Web et Android.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = familyName,
                onValueChange = { familyName = it.take(80) },
                label = { Text("Nom de la famille") },
                placeholder = { Text("Ex. : La tribu des chouettes") },
                singleLine = true,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it.take(60) },
                label = { Text("Votre prénom") },
                singleLine = true,
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Text(
                text = "Votre couleur dans le planning",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                colors.forEach { hex ->
                    val color = Color(android.graphics.Color.parseColor(hex))
                    Box(
                        modifier = Modifier.size(38.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (selectedColor == hex) 3.dp else 0.dp,
                                color = if (selectedColor == hex) TextPrimary else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable(enabled = !saving) { selectedColor = hex }
                    )
                }
            }
            if (creationState is FamilyCreationState.Error) {
                Text(
                    text = creationState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = { onCreateFamily(familyName, firstName, selectedColor) },
                enabled = !saving && familyName.trim().length >= 2 && firstName.trim().length >= 2,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (saving) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("Créer ma famille")
            }
            Text(
                text = "Vous aviez déjà une famille sur le Web ? Utilisez le même compte Google puis actualisez.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = TextSecondary
            )
            OutlinedButton(onClick = onRefreshProfile, enabled = !saving) {
                Text("Retrouver ma famille")
            }
            OutlinedButton(onClick = onSignOut, enabled = !saving) {
                Text("Changer de compte Google")
            }
        }
    }
}
