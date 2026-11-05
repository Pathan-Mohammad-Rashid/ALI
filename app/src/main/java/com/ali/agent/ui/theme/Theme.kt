package com.ali.agent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AliDeepBg = Color(0xFF070B14)
val AliSurface = Color(0xFF111827)
val AliSurfaceVariant = Color(0xFF1E293B)
val AliCyan = Color(0xFF00F0FF)
val AliViolet = Color(0xFF8B5CF6)
val AliEmerald = Color(0xFF10B981)
val AliAmber = Color(0xFFF59E0B)
val AliRose = Color(0xFFF43F5E)
val AliTextPrimary = Color(0xFFF8FAFC)
val AliTextSecondary = Color(0xFF94A3B8)

private val AliDarkColorScheme = darkColorScheme(
    primary = AliCyan,
    onPrimary = AliDeepBg,
    secondary = AliViolet,
    onSecondary = Color.White,
    tertiary = AliEmerald,
    background = AliDeepBg,
    onBackground = AliTextPrimary,
    surface = AliSurface,
    onSurface = AliTextPrimary,
    surfaceVariant = AliSurfaceVariant,
    onSurfaceVariant = AliTextSecondary
)

@Composable
fun AliTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AliDarkColorScheme,
        content = content
    )
}
