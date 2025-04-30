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
    primary = Color(0xFF0A84FF),   // Azul iOS moderno
    secondary = Color(0xFF5E5CE6), // Roxo suave
    tertiary = Color(0xFF30D158),  // Verde vibrante para destaques

    background = Color(0xFF1C1C1E),  // Preto suave (evita contraste extremo)
    surface = Color(0xFF2C2C2E),     // Cinza escuro elegante
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,        // Melhor contraste no verde
    onBackground = Color(0xFFE5E5EA), // Texto cinza claro
    onSurface = Color(0xFFD1D1D6)    // Texto levemente mais escuro que o background
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),   // Azul vibrante Apple
    secondary = Color(0xFF5856D6), // Roxo intenso
    tertiary = Color(0xFF34C759),  // Verde destaque

    background = Color(0xFFF2F2F7), // Cinza muito claro (quase branco)
    surface = Color(0xFFFFFFFF),    // Branco puro para cards e elementos de destaque
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF1C1C1E), // Texto escuro no fundo claro
    onSurface = Color(0xFF3A3A3C)    // Texto secundário
)


@Composable
fun LumosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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