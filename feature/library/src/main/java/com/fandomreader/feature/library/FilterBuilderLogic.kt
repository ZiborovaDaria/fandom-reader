package com.fandomreader.feature.library

import com.fandomreader.domain.model.DisplayTagGroups
import com.fandomreader.domain.model.Fandom
import com.fandomreader.domain.model.Pairing

internal enum class FilterTab(val title: String) {
    FANDOMS("Фэндомы"),
    PAIRINGS("Пейринги"),
    CHARACTERS("Персонажи"),
    RATING("Рейтинг"),
    WARNINGS("Предупреждения"),
    ADDITIONAL("Доп. метки"),
    CATEGORY("Категории"),
    GENRE("Жанры"),
}

internal fun FilterTab.displayTagGroup(): String? = when (this) {
    FilterTab.FANDOMS, FilterTab.PAIRINGS -> null
    FilterTab.CHARACTERS -> DisplayTagGroups.CHARACTERS
    FilterTab.RATING -> DisplayTagGroups.RATING
    FilterTab.WARNINGS -> DisplayTagGroups.WARNINGS
    FilterTab.ADDITIONAL -> DisplayTagGroups.ADDITIONAL
    FilterTab.CATEGORY -> DisplayTagGroups.CATEGORY
    FilterTab.GENRE -> DisplayTagGroups.GENRE
}

private val TAG_FILTER_TABS: List<FilterTab> =
    FilterTab.entries.filter { it != FilterTab.FANDOMS && it != FilterTab.PAIRINGS }

internal fun buildFilterAvailableTabs(
    fandoms: List<Fandom>,
    pairings: List<Pairing>,
    catalogTagGroups: Set<String>,
    selectedTagGroups: Set<String>,
): List<FilterTab> {
    val tabs = mutableListOf<FilterTab>()
    if (fandoms.isNotEmpty()) tabs.add(FilterTab.FANDOMS)
    if (pairings.isNotEmpty()) tabs.add(FilterTab.PAIRINGS)
    for (tab in TAG_FILTER_TABS) {
        val group = tab.displayTagGroup()?.lowercase() ?: continue
        if (group in catalogTagGroups || group in selectedTagGroups) {
            tabs.add(tab)
        }
    }
    return tabs.ifEmpty { listOf(FilterTab.FANDOMS) }
}

internal fun resolveFilterTab(selected: FilterTab, available: List<FilterTab>): FilterTab =
    if (selected in available) selected else available.firstOrNull() ?: FilterTab.FANDOMS
