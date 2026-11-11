package com.aura.music

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF090A10)
val Panel = Color(0xFF171923)
val Raised = Color(0xFF242733)
val Accent = Color(0xFFB8F36B)
val Muted = Color(0xFF9B9EAC)
val Line = Color(0xFF2E3140)

@Composable
fun AuraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ink,
            surface = Panel,
            primary = Accent,
            onPrimary = Ink,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}
