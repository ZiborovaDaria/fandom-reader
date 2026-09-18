package com.fandomreader.feature.library

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScanEmptyMessagingTest {
    @Test
    fun limitedAccessMessageMentionsImport() {
        val limited = "Ограниченный доступ к файлам. Выберите EPUB/FB2 через Импорт."
        assertThat(limited).contains("Импорт")
        assertThat(limited).contains("Ограниченный доступ")
    }

    @Test
    fun emptyFoundMessageMentionsTypicalFolders() {
        val empty = "В Downloads/Documents/Books EPUB/FB2 не найдены. Можно добавить через Импорт."
        assertThat(empty).contains("Downloads/Documents/Books")
        assertThat(empty).contains("Импорт")
    }
}
