package fr.familleplanning.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.familleplanning.app.data.models.FamilyMember
import fr.familleplanning.app.ui.theme.CoralPrimary
import fr.familleplanning.app.ui.theme.TextPrimary
import fr.familleplanning.app.ui.theme.TextSecondary
import fr.familleplanning.app.viewmodel.FamilyUiState

@Composable
fun FamilyScreen(
    uiState: FamilyUiState,
    operationBusy: Boolean,
    operationMessage: String?,
    inviteToken: String?,
    onRefreshClick: () -> Unit,
    onAddChild: (String, String?, String) -> Unit,
    onCreateInvite: (String, String?) -> Unit,
    onDismissInvite: () -> Unit,
    modifier: Modifier = Modifier
) {
    var childDialog by remember { mutableStateOf(false) }
    var inviteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val state = uiState as? FamilyUiState.Success
    val isParent = state?.accessRole in listOf("owner", "admin", "parent")
    val parentCount = state?.members?.count {
        it.role in listOf("owner","admin","parent") ||
            (it.userId != null && it.roleName == "Parent titulaire")
    } ?: 0
    val canInviteSecondParent = parentCount < 2

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Ma famille", style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(state?.family?.name ?: "Le foyer partagé", color = TextSecondary)
                    }
                    IconButton(onClick = onRefreshClick) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Actualiser", tint = CoralPrimary)
                    }
                }
            }
            if (operationMessage != null) {
                Text(operationMessage,
                    color = if (operationMessage.startsWith("Ajout impossible") ||
                        operationMessage.startsWith("Invitation impossible"))
                        MaterialTheme.colorScheme.error else TextPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(16.dp))
            }
            when (uiState) {
                is FamilyUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = CoralPrimary) }
                is FamilyUiState.NoFamily -> Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center
                ) {
                    Text("Aucun foyer associé. Crée ta famille depuis l'accueil.", color = TextSecondary)
                }
                is FamilyUiState.Error -> Column(modifier = Modifier.padding(24.dp)) {
                    Text("Impossible de charger la famille", color = MaterialTheme.colorScheme.error)
                    Text(uiState.message, color = TextSecondary)
                    Button(onClick = onRefreshClick) { Text("Réessayer") }
                }
                is FamilyUiState.Success -> {
                    if (isParent) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Button(
                                onClick = { childDialog = true },
                                enabled = !operationBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("＋ Ajouter un enfant") }
                            OutlinedButton(
                                onClick = { inviteDialog = true },
                                enabled = !operationBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Inviter un adulte par lien") }
                            Text(
                                "Parents titulaires : ${parentCount.coerceAtLeast(1)}/2. " +
                                    "Les enfants n'ont pas besoin de compte.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        Text("Les parents titulaires gèrent les profils enfants et les invitations.",
                            modifier = Modifier.padding(16.dp), color = TextSecondary)
                    }
                    LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.members, key = { it.id }) { member ->
                            FamilyMemberCard(member)
                        }
                    }
                }
            }
        }
    }

    if (childDialog) {
        var name by rememberSaveable { mutableStateOf("") }
        var birthDate by rememberSaveable { mutableStateOf("") }
        var color by rememberSaveable { mutableStateOf("#A55EEA") }
        AlertDialog(
            onDismissRequest = { if (!operationBusy) childDialog = false },
            title = { Text("Ajouter un enfant") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Un profil dans le planning, sans adresse e-mail ni compte de connexion.")
                    OutlinedTextField(value = name, onValueChange = { name = it.take(60) },
                        label = { Text("Prénom") }, singleLine = true)
                    OutlinedTextField(value = birthDate,
                        onValueChange = { birthDate = it.take(10) },
                        label = { Text("Naissance (AAAA-MM-JJ) · facultatif") }, singleLine = true)
                    ColorChooser(color) { color = it }
                }
            },
            confirmButton = { TextButton(
                enabled = !operationBusy && name.trim().length >= 2,
                onClick = { onAddChild(name, birthDate.ifBlank { null }, color); childDialog = false }
            ) { Text("Ajouter") } },
            dismissButton = { TextButton(onClick = { childDialog = false }) { Text("Annuler") } }
        )
    }

    if (inviteDialog) {
        var role by rememberSaveable { mutableStateOf("adult") }
        var email by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!operationBusy) inviteDialog = false },
            title = { Text("Inviter un adulte") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Lien privé à usage unique, valable 48 heures. Le destinataire doit se connecter.")
                    OutlinedButton(
                        onClick = { role = "adult" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (role == "adult") "✓ Adulte (sans administration)" else "Adulte (sans administration)") }
                    OutlinedButton(
                        onClick = { role = "parent" },
                        enabled = canInviteSecondParent,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (role == "parent") "✓ Parent titulaire (2ᵉ)" else "Parent titulaire (2ᵉ)") }
                    OutlinedTextField(value = email, onValueChange = { email = it.take(254) },
                        label = { Text("Adresse e-mail réservée · facultatif") }, singleLine = true)
                }
            },
            confirmButton = { TextButton(
                enabled = !operationBusy,
                onClick = { onCreateInvite(role, email.ifBlank { null }); inviteDialog = false }
            ) { Text("Créer le lien") } },
            dismissButton = { TextButton(onClick = { inviteDialog = false }) { Text("Annuler") } }
        )
    }

    if (!inviteToken.isNullOrBlank()) {
        val deepLink = "familleplanning://invite?token=${inviteToken}"
        AlertDialog(
            onDismissRequest = onDismissInvite,
            title = { Text("Invitation créée") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ce lien s'ouvre sur Android. Pour le Web, le code peut être saisi sur la page « Rejoindre une famille ».")
                    Text(deepLink, style = MaterialTheme.typography.bodySmall)
                    Text("À usage unique · expiration après 48 heures.", color = TextSecondary)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Rejoins notre FamillePlanning : ${deepLink}")
                    }
                    context.startActivity(Intent.createChooser(intent, "Envoyer l'invitation"))
                    onDismissInvite()
                }) { Text("Partager") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Invitation FamillePlanning", deepLink))
                    onDismissInvite()
                }) { Text("Copier") }
            }
        )
    }
}

@Composable
private fun ColorChooser(selected: String, onSelect: (String) -> Unit) {
    val colors = listOf("#FF6B6B", "#48DBFB", "#1DD1A1", "#FECA57", "#A55EEA", "#FF9F43")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { hex ->
            val c = Color(android.graphics.Color.parseColor(hex))
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(c)
                    .border(if (hex == selected) 3.dp else 0.dp,
                        if (hex == selected) TextPrimary else Color.Transparent, CircleShape)
                    .clickable { onSelect(hex) }
            )
        }
    }
}

@Composable
private fun FamilyMemberCard(member: FamilyMember) {
    val color = try { Color(android.graphics.Color.parseColor(member.colorHex)) }
        catch (_: Exception) { CoralPrimary }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(color.copy(alpha = 0.16f))
                    .border(2.dp, color, CircleShape), contentAlignment = Alignment.Center
            ) {
                Text(member.firstName.take(1).uppercase(), color = color,
                    fontWeight = FontWeight.Bold, fontSize = 21.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.firstName, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(member.roleName ?: if (member.isChild) "Enfant" else "Adulte",
                    color = color, fontSize = 13.sp)
                if (member.isChild) {
                    Text("Profil sans connexion", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}
