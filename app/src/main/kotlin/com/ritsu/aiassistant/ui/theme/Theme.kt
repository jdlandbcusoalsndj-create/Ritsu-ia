package com.ritsu.aiassistant.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Paleta de colores inspirada en diseño anime moderno
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE91E63), // Rosa vibrante (Ritsu)
    onPrimary = Color.White,
    primaryContainer = Color(0xFF880E4F),
    onPrimaryContainer = Color(0xFFFFD6E8),
    
    secondary = Color(0xFF64B5F6), // Azul claro
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1976D2),
    onSecondaryContainer = Color(0xFFE1F5FE),
    
    tertiary = Color(0xFFAB47BC), // Púrpura
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF4A148C),
    onTertiaryContainer = Color(0xFFF3E5F5),
    
    error = Color(0xFFCF6679),
    onError = Color.Black,
    errorContainer = Color(0xFFB00020),
    onErrorContainer = Color(0xFFFFDAD6),
    
    background = Color(0xFF121212), // Fondo oscuro suave
    onBackground = Color(0xFFE0E0E0),
    
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFB0B0B0),
    
    outline = Color(0xFF404040),
    outlineVariant = Color(0xFF333333)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFE91E63), // Rosa vibrante (Ritsu)
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD6E8),
    onPrimaryContainer = Color(0xFF880E4F),
    
    secondary = Color(0xFF2196F3), // Azul
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1F5FE),
    onSecondaryContainer = Color(0xFF0D47A1),
    
    tertiary = Color(0xFF9C27B0), // Púrpura
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E5F5),
    onTertiaryContainer = Color(0xFF4A148C),
    
    error = Color(0xFFB00020),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    
    background = Color(0xFFFAFAFA), // Fondo claro suave
    onBackground = Color(0xFF1C1B1F),
    
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF424242),
    
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE0E0E0)
)

@Composable
fun RitsuAIAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Deshabilitado para mantener la estética anime
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
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Colores adicionales específicos de Ritsu
object RitsuColors {
    val RitsuPink = Color(0xFFE91E63)
    val RitsuBlue = Color(0xFF64B5F6)
    val RitsuPurple = Color(0xFFAB47BC)
    val RitsuGreen = Color(0xFF66BB6A)
    val RitsuOrange = Color(0xFFFF9800)
    
    // Colores de avatar
    val SkinTone = Color(0xFFFFDBB5)
    val HairBrown = Color(0xFF8B4513)
    val EyeBlue = Color(0xFF4169E1)
    val BlushPink = Color(0xFFFF69B4)
    
    // Estados de actividad
    val Active = Color(0xFF4CAF50)
    val Listening = Color(0xFFFF5722)
    val Speaking = Color(0xFF2196F3)
    val Thinking = Color(0xFF9C27B0)
    val Idle = Color(0xFF757575)
}