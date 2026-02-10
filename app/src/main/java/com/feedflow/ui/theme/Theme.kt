package com.feedflow.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkTextPrimary,
    primaryContainer = DarkCard,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkAccent,
    onSecondary = DarkTextPrimary,
    secondaryContainer = DarkInputBackground,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = Info,
    onTertiary = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkCard,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkInputBackground,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkDivider,
    outlineVariant = DarkDivider,
    error = Error,
    onError = DarkTextPrimary,
    errorContainer = Error.copy(alpha = 0.1f),
    onErrorContainer = Error
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightCard,
    primaryContainer = LightCard,
    onPrimaryContainer = LightTextPrimary,
    secondary = LightAccent,
    onSecondary = LightCard,
    secondaryContainer = LightInputBackground,
    onSecondaryContainer = LightTextPrimary,
    tertiary = Info,
    onTertiary = LightCard,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightCard,
    onSurface = LightTextPrimary,
    surfaceVariant = LightInputBackground,
    onSurfaceVariant = LightTextSecondary,
    outline = LightDivider,
    outlineVariant = LightDivider,
    error = Error,
    onError = LightCard,
    errorContainer = Error.copy(alpha = 0.1f),
    onErrorContainer = Error
)

@Composable
fun FeedflowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
