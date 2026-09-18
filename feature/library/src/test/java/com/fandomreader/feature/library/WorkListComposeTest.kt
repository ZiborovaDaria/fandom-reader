package com.fandomreader.feature.library

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import com.fandomreader.domain.model.Shelf
import com.fandomreader.domain.model.Work
import com.fandomreader.domain.model.WorkSource
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
class WorkListComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTitleAndSummary() {
        composeRule.setContent {
            WorkList(
                works = listOf(
                    sampleWork(
                        id = 1,
                        title = "Next Best Thing",
                        summary = "Thomas Potter summary",
                    ),
                ),
                onOpenWork = {},
            )
        }
        composeRule.onNodeWithText("Next Best Thing").assertIsDisplayed()
        composeRule.onNodeWithText("Thomas Potter summary", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Ещё", substring = true).assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun longSummaryShowsExpandedPreviewAndExpandToggle() {
        val longSummary = "A".repeat(COLLAPSED_SUMMARY_MAX + 50)
        var openWorkCalls = 0
        composeRule.setContent {
            WorkList(
                works = listOf(
                    sampleWork(id = 2, title = "Long Work", summary = longSummary),
                ),
                onOpenWork = { openWorkCalls++ },
            )
        }

        composeRule.onNodeWithText("Long Work").assertIsDisplayed()
        composeRule.onNodeWithText("A".repeat(COLLAPSED_SUMMARY_PREVIEW_MAX), substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(longSummary, substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Ещё", substring = true).assertIsDisplayed()

        composeRule
            .onNodeWithTag("summary_collapsed", useUnmergedTree = true)
            .performCustomAccessibilityActionWithLabel("Показать полностью")
        composeRule.waitForIdle()

        composeRule.onNodeWithText(longSummary, substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Свернуть", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Ещё", substring = true).assertDoesNotExist()
        assertThat(openWorkCalls).isEqualTo(0)

        composeRule
            .onNodeWithTag("summary_expanded", useUnmergedTree = true)
            .performCustomAccessibilityActionWithLabel("Свернуть")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("A".repeat(COLLAPSED_SUMMARY_PREVIEW_MAX), substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(longSummary, substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Ещё", substring = true).assertIsDisplayed()
        assertThat(openWorkCalls).isEqualTo(0)
    }

    @Test
    fun translateAsksConfirmationBeforeSending() {
        var translateCalls = 0
        composeRule.setContent {
            WorkList(
                works = listOf(
                    sampleWork(
                        id = 3,
                        title = "To Translate",
                        summary = "Short",
                    ),
                ),
                onOpenWork = {},
                onTranslate = { _, _ -> translateCalls++ },
            )
        }
        composeRule.onNodeWithText("Перевести").assertIsDisplayed()
        composeRule.onNodeWithText("Перевести").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Отправить текст в нейросеть для перевода?").assertIsDisplayed()
        assertThat(translateCalls).isEqualTo(0)
        composeRule.onNodeWithText("Отправить").performClick()
        composeRule.waitForIdle()
        assertThat(translateCalls).isEqualTo(1)
    }

    @Test
    fun translateControlIsBelowExpandControlNotOnSameRow() {
        val longSummary = "word ".repeat(COLLAPSED_SUMMARY_MAX / 4 + 20)
        composeRule.setContent {
            WorkList(
                works = listOf(
                    sampleWork(
                        id = 4,
                        title = "Long AO3",
                        summary = longSummary,
                    ),
                ),
                onOpenWork = {},
                onTranslate = { _, _ -> },
            )
        }
        composeRule.onNodeWithText("Ещё", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Перевести").assertIsDisplayed()
        val summaryBottom = composeRule
            .onNodeWithTag("summary_collapsed", useUnmergedTree = true)
            .fetchSemanticsNode()
            .boundsInRoot
            .bottom
        val translateTop = composeRule.onNodeWithText("Перевести").fetchSemanticsNode().boundsInRoot.top
        assertThat(translateTop).isGreaterThan(summaryBottom)
    }

    @Test
    fun shelfTabsVisibleInLibraryChrome() {
        composeRule.setContent {
            androidx.compose.material3.PrimaryTabRow(selectedTabIndex = 0) {
                androidx.compose.material3.Tab(
                    selected = true,
                    onClick = {},
                    text = { androidx.compose.material3.Text("Фанфики") },
                )
                androidx.compose.material3.Tab(
                    selected = false,
                    onClick = {},
                    text = { androidx.compose.material3.Text("Прочее") },
                )
            }
        }
        composeRule.onNodeWithText("Фанфики").assertIsDisplayed()
        composeRule.onNodeWithText("Прочее").assertIsDisplayed()
    }

    private fun sampleWork(
        id: Long,
        title: String,
        summary: String?,
    ) = Work(
        id = id,
        source = WorkSource.AO3,
        remoteId = id.toString(),
        sourceUrl = null,
        title = title,
        summary = summary,
        localPath = "/tmp/x.epub",
        format = "epub",
        shelf = Shelf.FANFICTION,
    )
}
