package com.fandomreader.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Paper = Color(0xFFF7F3EC)
val Ink = Color(0xFF1C1B19)
val Accent = Color(0xFF2F5D50)
val AccentSoft = Color(0xFFD8E6E0)
val Muted = Color(0xFF6B675F)

/** Warm night shell — not pure black / pure white. */
val NightBoard = Color(0xFF1C1B19)
val NightInk = Color(0xFFE8E2D6)
val NightAccent = Color(0xFF8FBFB0)
val NightMuted = Color(0xFFA39E94)
val NightSurface = Color(0xFF262421)

val SepiaPaper = Color(0xFFF4E8D0)
val SepiaInk = Color(0xFF3E3428)

enum class ReaderSurfaceTheme {
    Paper,
    Sepia,
    Night,
}

@Immutable
data class ReaderSurfaceColors(
    val background: Color,
    val foreground: Color,
)

fun readerSurfaceColors(theme: ReaderSurfaceTheme): ReaderSurfaceColors =
    when (theme) {
        ReaderSurfaceTheme.Paper -> ReaderSurfaceColors(Paper, Ink)
        ReaderSurfaceTheme.Sepia -> ReaderSurfaceColors(SepiaPaper, SepiaInk)
        ReaderSurfaceTheme.Night -> ReaderSurfaceColors(NightBoard, NightInk)
    }

val LocalReaderSurfaceColors = staticCompositionLocalOf {
    readerSurfaceColors(ReaderSurfaceTheme.Paper)
}
