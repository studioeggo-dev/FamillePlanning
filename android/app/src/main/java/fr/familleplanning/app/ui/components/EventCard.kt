package fr.familleplanning.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.familleplanning.app.data.models.CalendarEvent
import fr.familleplanning.app.data.models.DateTimeUtils
import fr.familleplanning.app.ui.theme.TextPrimary
import fr.familleplanning.app.ui.theme.TextSecondary

@Composable
fun EventCard(
    event: CalendarEvent,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val eventColor = try {
        Color(android.graphics.Color.parseColor(event.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val startZdt = DateTimeUtils.toParisDateTime(event.startInstant)
    val endZdt = DateTimeUtils.toParisDateTime(event.endInstant)
    val timeLabel = "${DateTimeUtils.formatTime(startZdt.toLocalTime())} - ${DateTimeUtils.formatTime(endZdt.toLocalTime())}"

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Bandeau de couleur latérale chaleureux
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(96.dp)
                    .background(eventColor)
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .weight(1f)
            ) {
                // Heure et Badge catégorie
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = eventColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = timeLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = eventColor
                        )
                    }

                    // Pastille de catégorie
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(eventColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = when (event.category) {
                                "sport" -> "Sport"
                                "school" -> "École"
                                "health" -> "Santé"
                                "outing" -> "Sortie"
                                else -> "Activité"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = eventColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Titre
                Text(
                    text = event.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Lieu facultatif
                if (!event.location.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Place,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = event.location,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
