package fr.familleplanning.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// FamillePlanning Light Color Scheme (Palette claire, joyeuse et contrastée)
private val FamillePlanningLightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = TextOnColor,
    primaryContainer = CoralPrimary.copy(alpha = 0.15f),
    onPrimaryContainer = CoralDark,

    secondary = SkyBlueSecondary,
    onSecondary = TextOnColor,
    secondaryContainer = SkyBlueSecondary.copy(alpha = 0.15f),
    onSecondaryContainer = SkyBlueDark,

    tertiary = MintAccent,
    onTertiary = TextOnColor,
    tertiaryContainer = MintAccent.copy(alpha = 0.15f),
    onTertiaryContainer = MintDark,

    background = BackgroundSoft,
    onBackground = TextPrimary,

    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCardSubtle,
    onSurfaceVariant = TextSecondary,

    outline = BorderSoft,
    outlineVariant = BorderSoft.copy(alpha = 0.6f)
)

@Composable
fun FamillePlanningTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = FamillePlanningLightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = BackgroundSoft.toArgb()
                window.navigationBarColor = SurfaceCard.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
