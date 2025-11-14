package com.bernaferrari.difflib

import com.bernaferrari.difflib.patch.Chunk
import com.bernaferrari.difflib.patch.Patch
import com.bernaferrari.difflib.patch.PatchFailedException
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class GenerateUnifiedDiffTest {

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

    @Test
    fun `generate unified`() {
        val origLines = fileToLines("${TestConstants.MOCK_FOLDER}original.txt")
        val revLines = fileToLines("${TestConstants.MOCK_FOLDER}revised.txt")

        verify(origLines, revLines, "original.txt", "revised.txt")
    }

    @Test
    fun `generate unified with one delta`() {
        val origLines = fileToLines("${TestConstants.MOCK_FOLDER}one_delta_test_original.txt")
        val revLines = fileToLines("${TestConstants.MOCK_FOLDER}one_delta_test_revised.txt")

        verify(origLines, revLines, "one_delta_test_original.txt", "one_delta_test_revised.txt")
    }

    @Test
    fun `generate unified diff without any deltas`() {
        val test = listOf("abc")
        val testRevised = listOf("abc2")
        val patch = DiffUtils.diff(test, testRevised)
        val unifiedDiffTxt = UnifiedDiffUtils.generateUnifiedDiff("abc1", "abc2", test, patch, 0).joinToString("\n")

        assertThat(unifiedDiffTxt)
            .describedAs("original filename should be abc1")
            .contains("--- abc1")
            .describedAs("revised filename should be abc2")
            .contains("+++ abc2")
    }

    @Test
    fun `diff issue10`() {
        val baseLines = fileToLines("${TestConstants.MOCK_FOLDER}issue10_base.txt")
        val patchLines = fileToLines("${TestConstants.MOCK_FOLDER}issue10_patch.txt")
        val patch = UnifiedDiffUtils.parseUnifiedDiff(patchLines)
        try {
            DiffUtils.patch(baseLines, patch)
        } catch (e: PatchFailedException) {
            fail<Unit>(e.message ?: "Patching baseLines failed")
        }
    }

    @Test
    fun `patch with no deltas`() {
        val lines1 = fileToLines("${TestConstants.MOCK_FOLDER}issue11_1.txt")
        val lines2 = fileToLines("${TestConstants.MOCK_FOLDER}issue11_2.txt")
        verify(lines1, lines2, "issue11_1.txt", "issue11_2.txt")
    }

    @Test
    fun `diff sample 5`() {
        val lines1 = fileToLines("${TestConstants.MOCK_FOLDER}5A.txt")
        val lines2 = fileToLines("${TestConstants.MOCK_FOLDER}5B.txt")
        verify(lines1, lines2, "5A.txt", "5B.txt")
    }

    @Test
    fun `diff with header line in text`() {
        val original = mutableListOf("test line1", "test line2", "test line 4", "test line 5")
        val revised = mutableListOf("test line1", "test line2", "@@ -2,6 +2,7 @@", "test line 4", "test line 5")

        val patch = DiffUtils.diff(original, revised)
        val udiff = UnifiedDiffUtils.generateUnifiedDiff("original", "revised", original, patch, 10)
        UnifiedDiffUtils.parseUnifiedDiff(udiff)
    }

    @Test
    fun `new file creation`() {
        val original = emptyList<String>()
        val revised = listOf("line1", "line2")

        val patch = DiffUtils.diff(original, revised)
        val udiff = UnifiedDiffUtils.generateUnifiedDiff(null, "revised", original, patch, 10)

        assertEquals("--- /dev/null", udiff[0])
        assertEquals("+++ revised", udiff[1])
        assertEquals("@@ -0,0 +1,2 @@", udiff[2])

        UnifiedDiffUtils.parseUnifiedDiff(udiff)
    }

    @Test
    fun `change position issue 89`() {
        val patchLines = fileToLines("${TestConstants.MOCK_FOLDER}issue89_patch.txt")
        val patch = UnifiedDiffUtils.parseUnifiedDiff(patchLines)
        val realRemoveListOne = listOf(3)
        val realAddListOne = listOf(3, 7, 8, 9, 10, 11, 12, 13, 14)
        validateChangePosition(patch, 0, realRemoveListOne, realAddListOne)
        val realRemoveListTwo: List<Int> = emptyList()
        val realAddListTwo = listOf(27, 28)
        validateChangePosition(patch, 1, realRemoveListTwo, realAddListTwo)
    }

    private fun validateChangePosition(
        patch: Patch<String>,
        index: Int,
        realRemoveList: List<Int>,
        realAddList: List<Int>
    ) {
        val originChunk: Chunk<String> = patch.deltas[index].source
        val removeList = originChunk.changePosition
        assertEquals(realRemoveList.size, removeList?.size ?: 0)
        removeList?.forEach { assertTrue(realRemoveList.contains(it)) }

        val targetChunk: Chunk<String> = patch.deltas[index].target
        val addList = targetChunk.changePosition
        assertEquals(realAddList.size, addList?.size ?: 0)
        addList?.forEach { assertTrue(realAddList.contains(it)) }
    }

    private fun verify(origLines: List<String>, revLines: List<String>, originalFile: String, revisedFile: String) {
        val patch = DiffUtils.diff(origLines, revLines)
        val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(originalFile, revisedFile, origLines, patch, 10)

        val fromUnifiedPatch = UnifiedDiffUtils.parseUnifiedDiff(unifiedDiff)
        try {
            val patchedLines = fromUnifiedPatch.applyTo(origLines)
            assertEquals(revLines.size, patchedLines.size)
            for (i in revLines.indices) {
                val originalLine = revLines[i]
                val patchedLine = patchedLines[i]
                if (originalLine != patchedLine) {
                    fail<Unit>("Line ${i + 1} of the patched file did not match the revised original")
                }
            }
        } catch (e: PatchFailedException) {
            fail<Unit>(e.message ?: "Patching unified diff failed")
        }
    }
}
