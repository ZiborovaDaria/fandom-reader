package com.fandomreader.source.ficbook

import java.io.File

internal object FixturePaths {
    fun resolve(vararg relative: String): File {
        val joined = relative.joinToString(File.separator)
        System.getProperty("fandom.fixtures")?.let { root ->
            return File(root, joined)
        }
        val roots = listOf(
            File("fixtures"),
            File("../fixtures"),
            File("../../fixtures"),
            File("../../../fixtures"),
            File("../../../../fixtures"),
        )
        val root = roots.firstOrNull { it.isDirectory }
            ?: error("fixtures/ directory not found (cwd=${System.getProperty("user.dir")})")
        return File(root, joined)
    }
}
