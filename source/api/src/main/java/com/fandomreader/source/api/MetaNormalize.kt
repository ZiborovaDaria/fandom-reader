package com.fandomreader.source.api

import com.fandomreader.domain.model.Fandom
import com.fandomreader.domain.model.Pairing
import com.fandomreader.domain.model.RelationshipType
import com.fandomreader.domain.model.TagKeys
import org.jsoup.Jsoup

object MetaNormalize {
    fun canonicalKey(displayName: String): String = TagKeys.canonicalKey(displayName)

    fun fandom(displayName: String): Fandom {
        val name = displayName.trim()
        return Fandom(canonicalKey = TagKeys.canonicalKey(name), displayName = name)
    }

    fun pairing(displayName: String, type: RelationshipType = relationshipType(displayName)): Pairing {
        val name = displayName.trim()
        return Pairing(
            canonicalKey = TagKeys.canonicalKey(name),
            displayName = name,
            type = type,
        )
    }

    fun relationshipType(tag: String): RelationshipType {
        return when {
            tag.contains('/') -> RelationshipType.ROMANTIC
            tag.contains('&') -> RelationshipType.PLATONIC
            else -> RelationshipType.OTHER
        }
    }

    fun stripHtml(html: String?): String? {
        if (html.isNullOrBlank()) return null
        val text = Jsoup.parse(html).text().trim()
        return text.ifBlank { null }
    }

    fun decodeXmlEntities(raw: String): String {
        return raw
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
    }
}
