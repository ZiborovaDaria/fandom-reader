package com.fandomreader.domain.model

object TagKeys {
    private val whitespace = Regex("\\s+")

    /** Stable filter key; does not rewrite & into /. */
    fun canonicalKey(displayName: String): String {
        return displayName
            .trim()
            .lowercase()
            .replace(whitespace, " ")
            .replace(Regex("[«»\"']"), "")
            .trim()
    }
}
