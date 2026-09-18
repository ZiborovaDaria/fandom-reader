package com.fandomreader.source.ao3

import com.fandomreader.source.api.BookMetaParser
import com.fandomreader.source.api.ParsedWorkMeta
import com.fandomreader.source.api.SourceKind
import java.io.File

/** Routes AO3 imports to EPUB or damaged-FB2 parsers by file extension. */
class Ao3MetaParser(
    private val epubParser: Ao3EpubParser = Ao3EpubParser(),
    private val fb2Parser: Ao3Fb2Parser = Ao3Fb2Parser(),
) : BookMetaParser {
    override fun supports(kind: SourceKind): Boolean = kind == SourceKind.AO3

    override fun parse(file: File): ParsedWorkMeta {
        return when (file.extension.lowercase()) {
            "epub" -> epubParser.parse(file)
            "fb2" -> fb2Parser.parse(file)
            else -> fb2Parser.parse(file)
        }
    }
}
