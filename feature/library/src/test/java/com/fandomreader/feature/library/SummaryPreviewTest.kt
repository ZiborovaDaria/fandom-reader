package com.fandomreader.feature.library

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SummaryPreviewTest {

    @Test
    fun truncatesAtWordBoundaryWithinLookbackWindow() {
        val summary = "alpha beta gamma ".repeat(80)
        val preview = collapsedSummaryPreview(summary)
        assertThat(summary.startsWith(preview.replace('\u00A0', ' ').trimEnd())).isTrue()
        assertThat(preview.length).isAtMost(COLLAPSED_SUMMARY_PREVIEW_MAX)
        assertThat(preview.length).isLessThan(summary.length)
    }

    @Test
    fun fallsBackToHardCutWhenNoBoundaryInWindow() {
        val summary = "A".repeat(COLLAPSED_SUMMARY_MAX + 50)
        val preview = collapsedSummaryPreview(summary)
        assertThat(preview).hasLength(COLLAPSED_SUMMARY_PREVIEW_MAX)
    }

    @Test
    fun shortSummaryUnchanged() {
        val summary = "Short note"
        assertThat(collapsedSummaryPreview(summary)).isEqualTo(summary)
    }
}
