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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import fr.familleplanning.app.viewmodel.InviteUiState

@Composable
fun InviteScreen(
    uiState: InviteUiState,
    emailVerified: Boolean,
    onRefresh: () -> Unit,
    onCheckEmail: () -> Unit,
    onAccept: (String, String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var firstName by rememberSaveable { mutableStateOf("") }
    var selectedColor by rememberSaveable { mutableStateOf("#48DBFB") }
    val colors = listOf("#FF6B6B","#48DBFB","#1DD1A1","#FECA57","#A55EEA","#FF9F43")
    Column(
        modifier = modifier.fillMaxSize().padding(26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Rejoindre la famille", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Text("Cette invitation est personnelle, valable 48 heures et utilisable une seule fois.",
            textAlign = TextAlign.Center)
        when (uiState) {
            is InviteUiState.Idle, is InviteUiState.Loading ->
                CircularProgressIndicator()
            is InviteUiState.Error -> {
                Text(uiState.message, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = onRefresh) { Text("Réessayer") }
            }
            is InviteUiState.Accepted -> Text("Bienvenue dans votre famille !")
            is InviteUiState.Preview -> {
                Text(uiState.familyName, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                if (!emailVerified) {
                    Text("Vérifiez votre adresse e-mail pour rejoindre ce foyer.",
                        color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = onCheckEmail) { Text("J'ai vérifié mon e-mail") }
                }
                OutlinedTextField(
                    value = firstName, onValueChange = { firstName = it.take(60) },
                    label = { Text("Votre prénom") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Votre couleur")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier.size(34.dp).clip(CircleShape).background(color)
                                .border(if (selectedColor == hex) 3.dp else 0.dp,
                                    if (selectedColor == hex) Color.Black else Color.Transparent,
                                    CircleShape)
                                .clickable { selectedColor = hex }
                        )
                    }
                }
                Button(
                    onClick = { onAccept(firstName, selectedColor) },
                    enabled = emailVerified && firstName.trim().length >= 2,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Accepter l'invitation") }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onCancel) { Text("Annuler") }
    }
}
