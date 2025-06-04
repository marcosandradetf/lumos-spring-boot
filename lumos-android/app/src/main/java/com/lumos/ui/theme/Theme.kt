package com.lumos.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF0A84FF),       // Azul iOS – manter
    secondary = Color(0xFF5E5CE6),     // Roxo suave – ok
    tertiary = Color(0xFF30D158),      // Verde vibrante – ok

    background = Color(0xFF000000),    // Preto real, mais fiel ao dark mode do iOS
    surface = Color(0xFF1C1C1E),       // "Card" mais destacado
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFFE5E5EA),  // Texto claro
    onSurface = Color(0xFFB0B0B5)      // Cinza claro suave (menos contraste)
)


private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),       // Azul padrão iOS
    secondary = Color(0xFF5E5CE6),     // Roxo mais alinhado com o dark
    tertiary = Color(0xFF34C759),      // Verde destaque – ok

    background = Color(0xFFF9F9F9),    // Ligeiramente mais quente que o `F2F2F7`
    surface = Color(0xFFFFFFFF),       // Branco – manter
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF1C1C1E),  // Texto principal – ok
    onSurface = Color(0xFF3C3C43)      // Padrão Apple para textos secundários
)



@Composable
fun LumosTheme(
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}