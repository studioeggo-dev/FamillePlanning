package fr.familleplanning.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Destinations de navigation de l'application FamillePlanning.
 */
sealed class Screen(val route: String, val title: String = "", val icon: ImageVector? = null) {
    object Welcome : Screen("welcome", "Bienvenue")
    object Planning : Screen("planning", "Planning", Icons.Rounded.CalendarMonth)
    object Family : Screen("family", "Ma famille", Icons.Rounded.Group)
    object Settings : Screen("settings", "Paramètres", Icons.Rounded.Settings)

    companion object {
        val bottomNavItems = listOf(Planning, Family, Settings)
    }
}
