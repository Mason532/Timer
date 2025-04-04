package com.timer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color.Green,
    secondary = PurpleGrey80,
    tertiary = Pink80,

    background = darkModeBackground,
    surface = darkModeSurface,
    onSurface = darkModeOnSurface,
    surfaceVariant = darkModeSurfaceVariant,
    onSurfaceVariant = darkModeOnSurfaceVariant,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,

    background = lightModeBackground,
    surface = lightModeSurface,
    onSurface = lightModeOnSurface,
    surfaceVariant = lightModeSurfaceVariant,
    onSurfaceVariant = lightModeOnSurfaceVariant,
)

@Composable
fun TimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    SystemStatusBar(isDarkTheme = darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun SystemStatusBar(isDarkTheme: Boolean) {
    val statusBarColor = darkModeSurface.hashCode()
    val context = LocalActivity.current as ComponentActivity
    DisposableEffect(Unit) {
        context.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                scrim = statusBarColor
            )
        )
        onDispose { }
    }
}