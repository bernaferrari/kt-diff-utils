package com.bernaferrari.difflib.unifieddiff

import com.bernaferrari.difflib.DiffUtils
import com.bernaferrari.difflib.TestConstants
import com.bernaferrari.difflib.patch.Patch
import com.bernaferrari.difflib.patch.PatchFailedException
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.FileReader
import java.io.IOException
import java.io.StringWriter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class UnifiedDiffRoundTripTest {

    @Test
    @Throws(IOException::class)
    fun generateUnified() {
        val origLines = fileToLines(TestConstants.MOCK_FOLDER + "original.txt")
        val revLines = fileToLines(TestConstants.MOCK_FOLDER + "revised.txt")
        verify(origLines, revLines, "original.txt", "revised.txt")
    }

    @Test
    @Throws(IOException::class)
    fun generateUnifiedWithOneDelta() {
        val origLines = fileToLines(TestConstants.MOCK_FOLDER + "one_delta_test_original.txt")
        val revLines = fileToLines(TestConstants.MOCK_FOLDER + "one_delta_test_revised.txt")
        verify(origLines, revLines, "one_delta_test_original.txt", "one_delta_test_revised.txt")
    }

    @Test
    @Throws(IOException::class)
    fun generateUnifiedDiffWithoutAnyDeltas() {
        val test = listOf("abc")
        val patch = DiffUtils.diff(test, test)
        val writer = StringWriter()

        UnifiedDiffWriter.write(
            UnifiedDiff.from("header", "tail", UnifiedDiffFile.from("abc", "abc", patch)),
            { test },
            writer,
            0
        )
        println(writer)
    }

    @Test
    @Throws(IOException::class)
    fun diffIssue10() {
        val baseLines = fileToLines(TestConstants.MOCK_FOLDER + "issue10_base.txt")
        val patchLines = fileToLines(TestConstants.MOCK_FOLDER + "issue10_patch.txt")

        val unifiedDiff = UnifiedDiffReader.parseUnifiedDiff(
            ByteArrayInputStream(patchLines.joinToString("\n").toByteArray())
        )
        val patch = unifiedDiff.getFiles()[0].patch
        try {
            DiffUtils.patch(baseLines, patch)
        } catch (e: PatchFailedException) {
            fail<Unit>(e.message ?: "Patching baseLines failed")
        }
    }

    /**
     * Issue 12
     */
    @Test
    @Disabled
    @Throws(IOException::class)
    fun patchWithNoDeltas() {
        val lines1 = fileToLines(TestConstants.MOCK_FOLDER + "issue11_1.txt")
        val lines2 = fileToLines(TestConstants.MOCK_FOLDER + "issue11_2.txt")
        verify(lines1, lines2, "issue11_1.txt", "issue11_2.txt")
    }

    @Test
    @Throws(IOException::class)
    fun diff5() {
        val lines1 = fileToLines(TestConstants.MOCK_FOLDER + "5A.txt")
        val lines2 = fileToLines(TestConstants.MOCK_FOLDER + "5B.txt")
        verify(lines1, lines2, "5A.txt", "5B.txt")
    }

    /**
     * Issue 19
     */
    @Test
    @Throws(IOException::class)
    fun diffWithHeaderLineInText() {
        val original = mutableListOf("test line1", "test line2", "test line 4", "test line 5")
        val revised = mutableListOf("test line1", "test line2", "@@ -2,6 +2,7 @@", "test line 4", "test line 5")

        val patch = DiffUtils.diff(original, revised)
        val writer = StringWriter()
        UnifiedDiffWriter.write(
            UnifiedDiff.from("header", "tail", UnifiedDiffFile.from("original", "revised", patch)),
            { original },
            writer,
            10
        )
        println(writer.toString())
        UnifiedDiffReader.parseUnifiedDiff(ByteArrayInputStream(writer.toString().toByteArray()))
    }

    @Throws(IOException::class)
    private fun verify(origLines: List<String>, revLines: List<String>, originalFile: String, revisedFile: String) {
        val patch = DiffUtils.diff(origLines, revLines)
        val writer = StringWriter()
        UnifiedDiffWriter.write(
            UnifiedDiff.from("header", "tail", UnifiedDiffFile.from(originalFile, revisedFile, patch)),
            { origLines },
            writer,
            10
        )
        println(writer.toString())

        val unifiedDiff = UnifiedDiffReader.parseUnifiedDiff(ByteArrayInputStream(writer.toString().toByteArray()))

        try {
            val patchedLines = unifiedDiff.applyPatchTo({ file -> originalFile == file }, origLines)
            assertEquals(revLines.size, patchedLines.size)
            for (i in revLines.indices) {
                val l1 = revLines[i]
                val l2 = patchedLines[i]
                if (l1 != l2) {
                    fail<Unit>("Line ${i + 1} of the patched file did not match the revised original")
                }
            }
        } catch (e: PatchFailedException) {
            fail<Unit>(e.message ?: "Patching unified diff failed")
        }
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
