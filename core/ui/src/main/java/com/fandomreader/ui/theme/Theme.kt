package com.fandomreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val WarmPaperLight = lightColorScheme(
    primary = Accent,
    onPrimary = Paper,
    primaryContainer = AccentSoft,
    onPrimaryContainer = Ink,
    secondary = Muted,
    onSecondary = Paper,
    tertiary = Accent,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = AccentSoft,
    onSurfaceVariant = Muted,
    outline = Muted,
)

private val WarmPaperNight = darkColorScheme(
    primary = NightAccent,
    onPrimary = NightBoard,
    primaryContainer = NightSurface,
    onPrimaryContainer = NightInk,
    secondary = NightMuted,
    onSecondary = NightBoard,
    tertiary = NightAccent,
    background = NightBoard,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightSurface,
    onSurfaceVariant = NightMuted,
    outline = NightMuted,
)

@Composable
fun FandomReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    readerSurfaceTheme: ReaderSurfaceTheme = ReaderSurfaceTheme.Paper,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) WarmPaperNight else WarmPaperLight
    SystemBarAppearance(darkIcons = !darkTheme)
    CompositionLocalProvider(
        LocalReaderSurfaceColors provides readerSurfaceColors(readerSurfaceTheme),
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = FandomTypography,
            content = content,
        )
    }
}
