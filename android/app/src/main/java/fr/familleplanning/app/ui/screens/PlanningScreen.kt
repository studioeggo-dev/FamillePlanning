package fr.familleplanning.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import fr.familleplanning.app.data.models.CalendarEvent
import fr.familleplanning.app.data.models.DateTimeUtils
import fr.familleplanning.app.data.models.FamilyMember
import fr.familleplanning.app.data.repository.EventsSyncState
import fr.familleplanning.app.ui.components.EventCard
import fr.familleplanning.app.ui.components.EventEditorDialog
import fr.familleplanning.app.ui.components.FamilyHeader
import fr.familleplanning.app.ui.components.MemberChip
import fr.familleplanning.app.ui.theme.CoralPrimary
import fr.familleplanning.app.ui.theme.MintAccent
import fr.familleplanning.app.ui.theme.TextPrimary
import fr.familleplanning.app.ui.theme.TextSecondary
import fr.familleplanning.app.viewmodel.PlanningUiState
import fr.familleplanning.app.viewmodel.PlanningViewMode
import java.time.LocalDate

@Composable
fun PlanningScreen(
    uiState: PlanningUiState,
    members: List<FamilyMember>,
    familyName: String,
    onPreviousDateClick: () -> Unit,
    onNextDateClick: () -> Unit,
    onTodayClick: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onViewModeChange: (PlanningViewMode) -> Unit,
    onParticipantToggle: (String) -> Unit,
    onCreateEvent: (CalendarEvent) -> Unit,
    onUpdateEvent: (CalendarEvent) -> Unit,
    onDeleteEvent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDialogOpen by remember { mutableStateOf(false) }
    var selectedEventForEdit by remember { mutableStateOf<CalendarEvent?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedEventForEdit = null
                    isDialogOpen = true
                },
                containerColor = CoralPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(62.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Ajouter un événement",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. En-tête familial avec bascule de vue et dates
            FamilyHeader(
                familyName = familyName,
                selectedDate = uiState.selectedDate,
                viewMode = uiState.viewMode,
                onPreviousClick = onPreviousDateClick,
                onNextClick = onNextDateClick,
                onTodayClick = onTodayClick,
                onViewModeChange = onViewModeChange
            )

            // 2. Jours de la semaine (Vue Semaine)
            if (uiState.viewMode == PlanningViewMode.WEEK) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.weekDays) { day ->
                        val isSelected = day == uiState.selectedDate
                        val isToday = day == DateTimeUtils.todayInParis()

                        DayBadge(
                            date = day,
                            isSelected = isSelected,
                            isToday = isToday,
                            onClick = { onDateSelected(day) }
                        )
                    }
                }
            }

            // 3. Filtres participants
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(members) { member ->
                    MemberChip(
                        member = member,
                        isSelected = uiState.selectedParticipantId == member.id,
                        onClick = { onParticipantToggle(member.id) }
                    )
                }
            }

            // 4. Indicateur de statut de synchronisation Firestore temps réel
            Spacer(modifier = Modifier.height(10.dp))
            SyncStateBadge(syncState = uiState.syncState)

            // 5. Indicateur d'opération en cours
            if (uiState.isOperationPending) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CoralPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mise à jour Firestore en cours...", fontSize = 12.sp, color = TextSecondary)
                }
            }

            // 6. Liste des cartes d'événements
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.filteredEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Aucun événement prévu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Appuyez sur « + » pour planifier une activité.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.filteredEvents, key = { it.id }) { event ->
                        EventCard(
                            event = event,
                            onClick = {
                                selectedEventForEdit = event
                                isDialogOpen = true
                            }
                        )
                    }
                }
            }
        }

        // Dialogue de création / édition / suppression d'événement
        if (isDialogOpen) {
            EventEditorDialog(
                initialEvent = selectedEventForEdit,
                members = members,
                selectedDate = uiState.selectedDate,
                onSave = { event ->
                    if (selectedEventForEdit == null) {
                        onCreateEvent(event)
                    } else {
                        onUpdateEvent(event)
                    }
                    isDialogOpen = false
                },
                onDelete = { eventId ->
                    onDeleteEvent(eventId)
                    isDialogOpen = false
                },
                onDismiss = { isDialogOpen = false }
            )
        }
    }
}

@Composable
private fun SyncStateBadge(syncState: EventsSyncState) {
    val (bg, icon, text, tint) = when (syncState) {
        is EventsSyncState.RealTimeSuccess -> Quadruple(
            MintAccent.copy(alpha = 0.12f),
            Icons.Rounded.CloudDone,
            "Synchronisé en temps réel avec Cloud Firestore Web & Android",
            MintAccent
        )
        is EventsSyncState.Loading -> Quadruple(
            CoralPrimary.copy(alpha = 0.12f),
            Icons.Rounded.Sync,
            "Connexion au flux Firestore...",
            CoralPrimary
        )
        is EventsSyncState.Error -> Quadruple(
            CoralPrimary.copy(alpha = 0.15f),
            Icons.Rounded.ErrorOutline,
            "Erreur de synchronisation : ${syncState.message}",
            CoralPrimary
        )
        else -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            Icons.Rounded.Info,
            "Initialisation du planning...",
            TextSecondary
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tint
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            lineHeight = 14.sp
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun DayBadge(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val dayLetter = when (date.dayOfWeek.value) {
        1 -> "Lun"
        2 -> "Mar"
        3 -> "Mer"
        4 -> "Jeu"
        5 -> "Ven"
        6 -> "Sam"
        else -> "Dim"
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) CoralPrimary else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isSelected) 3.dp else 1.dp,
        modifier = Modifier.width(48.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayLetter,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color.White.copy(alpha = 0.9f) else TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else TextPrimary
            )
            if (isToday && !isSelected) {
                Spacer(modifier = Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(CoralPrimary)
                )
            }
        }
    }
}
