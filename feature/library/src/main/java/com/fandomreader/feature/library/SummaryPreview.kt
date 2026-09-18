package com.fandomreader.feature.library

internal const val COLLAPSED_SUMMARY_MAX = 440
internal const val SUMMARY_EXPAND_LINK = "summary_expand"
internal const val SUMMARY_COLLAPSE_LINK = "summary_collapse"
internal const val SUMMARY_EXPAND_SUFFIX = "\u00A0Ещё"
internal const val SUMMARY_COLLAPSE_SUFFIX = "\u00A0Свернуть"
internal const val SUMMARY_EXPAND_SUFFIX_CHAR_RESERVE = 4
internal const val COLLAPSED_SUMMARY_PREVIEW_MAX =
    COLLAPSED_SUMMARY_MAX - SUMMARY_EXPAND_SUFFIX_CHAR_RESERVE

internal fun collapsedSummaryPreview(
    fullSummary: String,
    maxChars: Int = COLLAPSED_SUMMARY_PREVIEW_MAX,
    wordBoundaryLookback: Int = 20,
): String {
    if (fullSummary.length <= maxChars) return fullSummary
    val hardCut = fullSummary.take(maxChars)
    val windowStart = (maxChars - wordBoundaryLookback).coerceAtLeast(0)
    val slice = hardCut.substring(windowStart)
    val breakInWindow = maxOf(slice.lastIndexOf(' '), slice.lastIndexOf('\n'))
    val preview = if (breakInWindow >= 0) {
        hardCut.take(windowStart + breakInWindow).trimEnd()
    } else {
        hardCut
    }
    return glueLastWordForInlineSuffix(preview)
}

internal fun glueLastWordForInlineSuffix(preview: String): String {
    val lastSpace = preview.lastIndexOf(' ')
    return if (lastSpace >= 0) {
        preview.substring(0, lastSpace) + '\u00A0' + preview.substring(lastSpace + 1)
    } else {
        preview
    }
}
