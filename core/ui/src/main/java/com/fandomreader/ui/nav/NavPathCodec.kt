package com.fandomreader.ui.nav

import android.net.Uri

/**
 * Percent-encodes navigation path arguments so keys with `/` or spaces
 * stay a single path segment. Uses [Uri.encode] with a null allow-list
 * so `/` is encoded (default allow-list would leave it intact).
 */
object NavPathCodec {
    fun encode(value: String): String = Uri.encode(value, null)

    fun decode(value: String): String = Uri.decode(value)
}
