package fr.familleplanning.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import fr.familleplanning.app.data.models.CalendarEvent
import fr.familleplanning.app.data.models.DateTimeUtils
import fr.familleplanning.app.data.models.FamilyMember
import fr.familleplanning.app.ui.theme.CoralDark
import fr.familleplanning.app.ui.theme.CoralPrimary
import fr.familleplanning.app.ui.theme.FamilyMemberColors
import fr.familleplanning.app.ui.theme.TextPrimary
import fr.familleplanning.app.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.LocalTime

/**
 * Boîte de dialogue permettant la création et l'édition d'événements familiaux.
 * Supporte le choix des participants multiples, du lieu, de la couleur et de la suppression avec confirmation.
 */
@Composable
fun EventEditorDialog(
    initialEvent: CalendarEvent?,
    members: List<FamilyMember>,
    selectedDate: LocalDate,
    onSave: (CalendarEvent) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var description by remember { mutableStateOf(initialEvent?.description ?: "") }
    var location by remember { mutableStateOf(initialEvent?.location ?: "") }
    var category by remember { mutableStateOf(initialEvent?.category ?: "general") }
    var isAllDay by remember { mutableStateOf(initialEvent?.isAllDay ?: false) }
    var selectedColorHex by remember { mutableStateOf(initialEvent?.colorHex ?: "#FF6B6B") }
    var selectedParticipants by remember { mutableStateOf(initialEvent?.participantIds?.toSet() ?: emptySet()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialEvent == null) "Planifier une activité" else "Modifier l'événement",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Champ Titre
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de l'activité *") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Champ Lieu
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Lieu (ex: Dojo, École, Parc)") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Choix de la catégorie
                Text(
                    text = "Catégorie",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf("sport" to "Sport", "school" to "École", "health" to "Santé", "outing" to "Sortie")
                    categories.forEach { (catKey, catLabel) ->
                        val isCatSelected = category == catKey
                        Surface(
                            onClick = { category = catKey },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCatSelected) CoralPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = catLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCatSelected) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sélection des participants
                Text(
                    text = "Participants concernés",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    members.forEach { member ->
                        val isChecked = selectedParticipants.contains(member.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedParticipants = if (isChecked) {
                                        selectedParticipants - member.id
                                    } else {
                                        selectedParticipants + member.id
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = CoralPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = member.firstName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Boutons d'action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (initialEvent != null) {
                        TextButton(
                            onClick = { showDeleteConfirmation = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = CoralDark)
                        ) {
                            Icon(imageVector = Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Supprimer")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text("Annuler")
                        }

                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    val startInst = initialEvent?.startInstant
                                        ?: DateTimeUtils.toInstantFromParis(selectedDate, LocalTime.of(10, 0))
                                    val endInst = initialEvent?.endInstant
                                        ?: DateTimeUtils.toInstantFromParis(selectedDate, LocalTime.of(11, 0))

                                    val newEvent = CalendarEvent(
                                        id = initialEvent?.id ?: "",
                                        familyId = initialEvent?.familyId ?: "",
                                        title = title.trim(),
                                        description = description.trim(),
                                        startInstant = startInst,
                                        endInstant = endInst,
                                        isAllDay = isAllDay,
                                        participantIds = selectedParticipants.toList(),
                                        category = category,
                                        colorHex = selectedColorHex,
                                        location = location.trim().ifEmpty { null }
                                    )
                                    onSave(newEvent)
                                }
                            },
                            enabled = title.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary)
                        ) {
                            Text("Enregistrer", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal de confirmation de suppression
    if (showDeleteConfirmation && initialEvent != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Supprimer l'événement ?") },
            text = { Text("Voulez-vous vraiment supprimer « ${initialEvent.title} » ? Cette action est définitive.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(initialEvent.id)
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralDark)
                ) {
                    Text("Supprimer définitivement")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
