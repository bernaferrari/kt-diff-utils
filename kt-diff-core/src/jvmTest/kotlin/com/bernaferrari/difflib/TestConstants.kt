package com.bernaferrari.difflib

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object TestConstants {
    private val resourceDir: Path = sequenceOf(
        Paths.get("kt-diff-core/build/resources/test"),
        Paths.get("build/resources/test"),
        Paths.get("target/test-classes"),
        Paths.get("kt-diff-core/src/test/resources"),
        Paths.get("src/test/resources")
    ).firstOrNull { Files.exists(it) } ?: Paths.get("src/test/resources")

    val BASE_FOLDER_RESOURCES: String = resourceDir.toString() + File.separator
    val MOCK_FOLDER: String = resourceDir.resolve("mocks").toString() + File.separator

    fun path(relative: String): Path = resourceDir.resolve(relative)
}
