package com.fandomreader.feature.reader

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import java.util.zip.ZipFile

/** Alphanumeric natural compare: chapter_2 before chapter_11. */
fun naturalCompare(a: String, b: String): Int {
    val ra = Regex("""\d+|\D+""")
    val rb = Regex("""\d+|\D+""")
    val pa = ra.findAll(a).map { it.value }.toList()
    val pb = rb.findAll(b).map { it.value }.toList()
    val n = minOf(pa.size, pb.size)
    for (i in 0 until n) {
        val x = pa[i]
        val y = pb[i]
        val cmp = if (x[0].isDigit() && y[0].isDigit()) {
            x.toBigInteger().compareTo(y.toBigInteger())
        } else {
            x.compareTo(y, ignoreCase = true)
        }
        if (cmp != 0) return cmp
    }
    return pa.size.compareTo(pb.size)
}

/**
 * Resolve reading-order hrefs from OPF spine. Returns zip entry paths when possible.
 */
fun readOpfSpineHrefs(file: File): List<String> {
    return runCatching {
        ZipFile(file).use { zip ->
            val container = zip.getEntry("META-INF/container.xml") ?: return emptyList()
            val containerXml = zip.getInputStream(container).bufferedReader().use { it.readText() }
            val containerDoc = Jsoup.parse(containerXml, "", Parser.xmlParser())
            val opfPath = containerDoc.selectFirst("rootfile")?.attr("full-path") ?: return emptyList()
            val opfEntry = zip.getEntry(opfPath) ?: return emptyList()
            val opfXml = zip.getInputStream(opfEntry).bufferedReader().use { it.readText() }
            val opf = Jsoup.parse(opfXml, "", Parser.xmlParser())
            val opfDir = opfPath.substringBeforeLast('/', missingDelimiterValue = "").let {
                if (it.isEmpty()) "" else "$it/"
            }
            val idToHref = opf.select("*").filter { el ->
                val tag = el.tagName().lowercase()
                tag == "item" || tag.endsWith(":item")
            }.associate { el ->
                el.attr("id") to el.attr("href")
            }
            opf.select("*").filter { el ->
                val tag = el.tagName().lowercase()
                tag == "itemref" || tag.endsWith(":itemref")
            }.mapNotNull { el ->
                val idref = el.attr("idref")
                val href = idToHref[idref]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val normalized = (opfDir + href).replace('\\', '/')
                // Resolve relative .. segments lightly
                normalized.split('/').fold(mutableListOf<String>()) { acc, part ->
                    when (part) {
                        "", "." -> acc
                        ".." -> { if (acc.isNotEmpty()) acc.removeAt(acc.lastIndex); acc }
                        else -> { acc += part; acc }
                    }
                }.joinToString("/")
            }
        }
    }.getOrDefault(emptyList())
}

/**
 * Order HTML entry names: spine order first, then natural-sorted leftovers.
 */
fun orderEpubHtmlEntries(entryNames: List<String>, spineHrefs: List<String>): List<String> {
    val byBase = entryNames.associateBy { it.substringAfterLast('/') }
    val ordered = linkedSetOf<String>()
    for (href in spineHrefs) {
        val base = href.substringAfterLast('/')
        val match = entryNames.firstOrNull { it == href || it.endsWith("/$base") || it.substringAfterLast('/') == base }
            ?: byBase[base]
        if (match != null) ordered += match
    }
    val rest = entryNames.filter { it !in ordered }.sortedWith { a, b -> naturalCompare(a, b) }
    return ordered.toList() + rest
}
