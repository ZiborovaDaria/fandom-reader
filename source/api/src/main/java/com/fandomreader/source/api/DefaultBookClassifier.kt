package com.fandomreader.source.api

import java.io.File
import java.util.zip.ZipFile

class DefaultBookClassifier : BookClassifier {
    override fun classify(file: File): SourceKind {
        val sniff = sniffText(file)
        return when {
            "archiveofourown.org" in sniff || "archive of our own" in sniff -> SourceKind.AO3
            "ficbook.net" in sniff -> SourceKind.FICBOOK
            BookFormatDetect.detect(file) == "fb2" &&
                ("фэндом:" in sniff || "отношения:" in sniff) -> SourceKind.AO3
            else -> SourceKind.OTHER
        }
    }

    private fun sniffText(file: File): String {
        return try {
            when (BookFormatDetect.detect(file)) {
                "epub" -> {
                    ZipFile(file).use { zip ->
                        buildString {
                            zip.entries().asSequence().take(40).forEach { entry ->
                                if (!entry.isDirectory && (
                                        entry.name.endsWith(".opf", true) ||
                                            entry.name.endsWith(".xhtml", true) ||
                                            entry.name.endsWith(".html", true)
                                        )
                                ) {
                                    zip.getInputStream(entry).bufferedReader().use { reader ->
                                        appendLine(reader.readText().take(20_000))
                                    }
                                }
                            }
                        }.lowercase()
                    }
                }
                "fb2" ->
                    file.readText(Charsets.UTF_8).take(40_000).lowercase()
                else -> file.readText(Charsets.UTF_8).take(10_000).lowercase()
            }
        } catch (_: Exception) {
            ""
        }
    }
}
