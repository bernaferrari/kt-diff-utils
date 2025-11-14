package com.bernaferrari.difflib.examples

import com.bernaferrari.difflib.TestConstants
import com.bernaferrari.difflib.UnifiedDiffUtils
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class OriginalAndDiffTest {

    @Test
    fun generateOriginalAndDiff() {
        val origLines: List<String>
        val revLines: List<String>
        try {
            origLines = fileToLines(TestConstants.MOCK_FOLDER + "original.txt")
            revLines = fileToLines(TestConstants.MOCK_FOLDER + "revised.txt")
        } catch (e: IOException) {
            fail<Unit>(e.message ?: "Failed to read original files")
            return
        }

        val originalAndDiff = UnifiedDiffUtils.generateOriginalAndDiff(origLines, revLines)
        println(originalAndDiff.joinToString("\n"))
    }

    @Test
    fun generateOriginalAndDiffFirstLineChange() {
        val origLines: List<String>
        val revLines: List<String>
        try {
            origLines = fileToLines(TestConstants.MOCK_FOLDER + "issue_170_original.txt")
            revLines = fileToLines(TestConstants.MOCK_FOLDER + "issue_170_revised.txt")
        } catch (e: IOException) {
            fail<Unit>(e.message ?: "Failed to read issue 170 files")
            return
        }

        val originalAndDiff = UnifiedDiffUtils.generateOriginalAndDiff(origLines, revLines)
        println(originalAndDiff.joinToString("\n"))
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun fileToLines(filename: String): List<String> {
            val lines = mutableListOf<String>()
            BufferedReader(FileReader(filename)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    lines.add(line!!)
                }
            }
            return lines
        }
    }
}
