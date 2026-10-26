package fr.familleplanning.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.familleplanning.app.data.models.FamilyMember
import fr.familleplanning.app.ui.theme.CoralPrimary
import fr.familleplanning.app.ui.theme.MintAccent
import fr.familleplanning.app.ui.theme.TextPrimary
import fr.familleplanning.app.ui.theme.TextSecondary
import fr.familleplanning.app.viewmodel.FamilyUiState

@Composable
fun FamilyScreen(
    uiState: FamilyUiState,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // En-tête de l'écran Ma Famille
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ma famille",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Membres du foyer et rôles",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }

                    IconButton(onClick = onRefreshClick) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Actualiser depuis Firestore",
                            tint = CoralPrimary
                        )
                    }
                }
            }

            // Corps de l'écran selon l'état de chargement
            when (uiState) {
                is FamilyUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CoralPrimary)
                    }
                }

                is FamilyUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Impossible de charger le foyer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CoralPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onRefreshClick) {
                                Text("Réessayer")
                            }
                        }
                    }
                }

                is FamilyUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Badge indiquant la source des données
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (uiState.isRemoteSource) MintAccent.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (uiState.isRemoteSource) Icons.Rounded.CloudDone else Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (uiState.isRemoteSource) MintAccent else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isRemoteSource)
                                    "Synchronisé avec Firestore (base : ai-studio-familleplanning-...)"
                                else
                                    "Aperçu local — Connectez votre compte Google pour lier votre foyer réel",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }

                        // Liste des membres du foyer
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.members, key = { it.id }) { member ->
                                MemberCard(member = member)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberCard(
    member: FamilyMember,
    modifier: Modifier = Modifier
) {
    val memberColor = try {
        Color(android.graphics.Color.parseColor(member.colorHex))
    } catch (e: Exception) {
        CoralPrimary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar expressif avec contour de couleur personnelle
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(memberColor.copy(alpha = 0.15f))
                    .border(2.5.dp, memberColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.firstName.take(1).uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = memberColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Prénom, rôle et informations
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.firstName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                if (!member.roleName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = member.roleName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = memberColor
                    )
                }

                if (member.isChild && !member.birthDate.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Né(e) le ${member.birthDate}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // Pastille indicatrice de la couleur personnelle
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(memberColor)
            )
        }
    }
}
