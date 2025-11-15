package com.bernaferrari.difflib.examples

import com.bernaferrari.difflib.DiffUtils
import com.bernaferrari.difflib.TestConstants
import java.io.File
import java.io.IOException
import java.nio.file.Files

object ComputeDifference {
    private val ORIGINAL = TestConstants.MOCK_FOLDER + "original.txt"
    private val REVISED = TestConstants.MOCK_FOLDER + "revised.txt"

    @JvmStatic
    @Throws(IOException::class)
    fun main(args: Array<String>) {
        val original = Files.readAllLines(File(ORIGINAL).toPath())
        val revised = Files.readAllLines(File(REVISED).toPath())

        val patch = DiffUtils.diff(original, revised)
        patch.deltas.forEach { println(it) }
    }
}
