package fr.familleplanning.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Formes douces et accueillantes (grandes cartes arrondies, boutons généreux)
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),       // Grandes cartes FamillePlanning
    extraLarge = RoundedCornerShape(32.dp)  // Boutons d'action et modales
)
