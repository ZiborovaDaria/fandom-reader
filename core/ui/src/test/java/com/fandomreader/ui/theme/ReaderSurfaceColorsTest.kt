package com.fandomreader.ui.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReaderSurfaceColorsTest {
    @Test
    fun threeThemesHaveDistinctBackgrounds() {
        val paper = readerSurfaceColors(ReaderSurfaceTheme.Paper)
        val sepia = readerSurfaceColors(ReaderSurfaceTheme.Sepia)
        val night = readerSurfaceColors(ReaderSurfaceTheme.Night)
        assertThat(setOf(paper.background, sepia.background, night.background)).hasSize(3)
        assertThat(setOf(paper.foreground, sepia.foreground, night.foreground).size)
            .isAtLeast(2)
    }

    @Test
    fun paperDefaultsMatchBrandTokens() {
        val colors = readerSurfaceColors(ReaderSurfaceTheme.Paper)
        assertThat(colors.background).isEqualTo(Paper)
        assertThat(colors.foreground).isEqualTo(Ink)
    }
}
