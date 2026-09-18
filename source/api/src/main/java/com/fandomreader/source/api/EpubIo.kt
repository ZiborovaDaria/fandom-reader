package com.fandomreader.source.api

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import java.io.File
import java.util.zip.ZipFile

object EpubIo {
    fun readOpf(file: File): Document {
        ZipFile(file).use { zip ->
            val container = zip.getEntry("META-INF/container.xml")
                ?: error("Missing META-INF/container.xml in ${file.name}")
            val containerXml = zip.getInputStream(container).bufferedReader().use { it.readText() }
            val containerDoc = Jsoup.parse(containerXml, "", Parser.xmlParser())
            val opfPath = containerDoc.selectFirst("rootfile")?.attr("full-path")
                ?: error("Missing rootfile in container.xml")
            val opfEntry = zip.getEntry(opfPath) ?: error("Missing OPF at $opfPath")
            val opfXml = zip.getInputStream(opfEntry).bufferedReader().use { it.readText() }
            return Jsoup.parse(opfXml, "", Parser.xmlParser())
        }
    }

    fun readEntryText(file: File, entryName: String): String? {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry(entryName) ?: return null
            return zip.getInputStream(entry).bufferedReader().use { it.readText() }
        }
    }

    fun findFirstEntry(file: File, predicate: (String) -> Boolean): Pair<String, String>? {
        ZipFile(file).use { zip ->
            val match = zip.entries().asSequence()
                .firstOrNull { !it.isDirectory && predicate(it.name) }
                ?: return null
            val text = zip.getInputStream(match).bufferedReader().use { it.readText() }
            return match.name to text
        }
    }

    fun findPrefixedEntry(file: File, vararg nameHints: String): Pair<String, String>? {
        return findFirstEntry(file) { name ->
            val lower = name.lowercase()
            nameHints.any { hint -> lower.contains(hint.lowercase()) } &&
                (lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm"))
        }
    }

    fun dcText(opf: Document, localName: String): String? {
        val value = opf.select("*").firstOrNull { el ->
            val tag = el.tagName().lowercase()
            tag == localName.lowercase() ||
                tag == "dc:${localName.lowercase()}" ||
                tag.endsWith(":${localName.lowercase()}")
        }?.text()?.trim()
        return value?.takeIf { it.isNotEmpty() }
    }

    fun formatOf(file: File): String = BookFormatDetect.detect(file)
}
