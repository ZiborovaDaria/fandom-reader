package com.fandomreader.feature.library

import com.fandomreader.domain.model.DisplayTagGroups
import com.fandomreader.domain.model.Fandom
import com.fandomreader.domain.model.Pairing
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FilterBuilderLogicTest {

    @Test
    fun resolveFilterTabKeepsCategoryWhenTabListShifts() {
        val before = listOf(
            FilterTab.FANDOMS,
            FilterTab.PAIRINGS,
            FilterTab.CATEGORY,
        )
        val after = listOf(
            FilterTab.FANDOMS,
            FilterTab.PAIRINGS,
            FilterTab.CHARACTERS,
            FilterTab.CATEGORY,
        )
        assertThat(resolveFilterTab(FilterTab.CATEGORY, before)).isEqualTo(FilterTab.CATEGORY)
        assertThat(resolveFilterTab(FilterTab.CATEGORY, after)).isEqualTo(FilterTab.CATEGORY)
    }

    @Test
    fun buildAvailableTabsKeepsTabWhenSelectionExistsWithoutCatalogGroup() {
        val tabs = buildFilterAvailableTabs(
            fandoms = listOf(Fandom(canonicalKey = "hp", displayName = "Harry Potter")),
            pairings = emptyList(),
            catalogTagGroups = setOf(DisplayTagGroups.RATING),
            selectedTagGroups = setOf(DisplayTagGroups.CATEGORY),
        )
        assertThat(tabs).contains(FilterTab.CATEGORY)
        assertThat(tabs.map { it.title }).doesNotContain("Размер")
    }

    @Test
    fun buildAvailableTabsDoesNotIncludeSizeTab() {
        val tabs = buildFilterAvailableTabs(
            fandoms = emptyList(),
            pairings = emptyList(),
            catalogTagGroups = setOf(DisplayTagGroups.SIZE, DisplayTagGroups.CHARACTERS),
            selectedTagGroups = emptySet(),
        )
        assertThat(tabs).contains(FilterTab.CHARACTERS)
        assertThat(tabs.map { it.title }).doesNotContain("Размер")
    }

    @Test
    fun tabIndexShiftWouldHaveChangedTabUnderOldIndexModel() {
        val before = buildFilterAvailableTabs(
            fandoms = listOf(Fandom(canonicalKey = "hp", displayName = "HP")),
            pairings = listOf(Pairing(canonicalKey = "a/b", displayName = "A/B")),
            catalogTagGroups = setOf(DisplayTagGroups.CATEGORY),
            selectedTagGroups = emptySet(),
        )
        val after = buildFilterAvailableTabs(
            fandoms = listOf(Fandom(canonicalKey = "hp", displayName = "HP")),
            pairings = listOf(Pairing(canonicalKey = "a/b", displayName = "A/B")),
            catalogTagGroups = setOf(DisplayTagGroups.CHARACTERS, DisplayTagGroups.CATEGORY),
            selectedTagGroups = emptySet(),
        )
        val categoryIndexBefore = before.indexOf(FilterTab.CATEGORY)
        val categoryIndexAfter = after.indexOf(FilterTab.CATEGORY)
        assertThat(categoryIndexBefore).isNotEqualTo(categoryIndexAfter)
        assertThat(resolveFilterTab(FilterTab.CATEGORY, before)).isEqualTo(FilterTab.CATEGORY)
        assertThat(resolveFilterTab(FilterTab.CATEGORY, after)).isEqualTo(FilterTab.CATEGORY)
    }
}
