package com.fandomreader.data.db

import com.fandomreader.domain.model.DisplayTag

/**
 * Line-based codec: `group\\tvalue` or `group\\tvalue\\tvalueRu` per line.
 * Legacy records without the third field decode with [DisplayTag.valueRu] = null.
 */
object DisplayTagsCodec {
    fun encode(tags: List<DisplayTag>): String? {
        if (tags.isEmpty()) return null
        return tags.joinToString("\n") { tag ->
            val value = tag.value.replace('\n', ' ').replace('\r', ' ')
            val ru = tag.valueRu?.replace('\n', ' ')?.replace('\r', ' ')?.takeIf { it.isNotBlank() }
            if (ru == null) {
                "${tag.group}\t$value"
            } else {
                "${tag.group}\t$value\t$ru"
            }
        }
    }

    fun decode(raw: String?): List<DisplayTag> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            val first = line.indexOf('\t')
            if (first <= 0) return@mapNotNull null
            val group = line.substring(0, first).trim()
            val rest = line.substring(first + 1)
            val second = rest.indexOf('\t')
            val value: String
            val valueRu: String?
            if (second < 0) {
                value = rest.trim()
                valueRu = null
            } else {
                value = rest.substring(0, second).trim()
                valueRu = rest.substring(second + 1).trim().takeIf { it.isNotEmpty() }
            }
            if (group.isEmpty() || value.isEmpty()) null else DisplayTag(group, value, valueRu)
        }.toList()
    }
}
