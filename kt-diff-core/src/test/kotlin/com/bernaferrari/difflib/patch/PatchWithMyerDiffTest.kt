package com.bernaferrari.difflib.patch

import com.bernaferrari.difflib.DiffUtils
import java.util.stream.Collectors.joining
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class PatchWithMyerDiffTest {

    @Test
    fun `patch change with merge conflict output`() {
        val changeTestFrom = arrayListOf("aaa", "bbb", "ccc", "ddd")
        val changeTestTo = listOf("aaa", "bxb", "cxc", "ddd")

        val patch = DiffUtils.diff(changeTestFrom, changeTestTo)
        changeTestFrom[2] = "CDC"
        patch.withConflictOutput(Patch.CONFLICT_PRODUCES_MERGE_CONFLICT)

        try {
            val data = DiffUtils.patch(changeTestFrom, patch)
            assertEquals(9, data.size)
            assertEquals(
                listOf("aaa", "<<<<<< HEAD", "bbb", "CDC", "======", "bbb", "ccc", ">>>>>>> PATCH", "ddd"),
                data
            )
        } catch (e: PatchFailedException) {
            fail<Unit>(e.message ?: "Patching diff failed")
        }
    }

    @Test
    @Throws(PatchFailedException::class)
    fun `patch three way issue 138`() {
        val base = "Imagine there's no heaven".split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
        val left = "Imagine there's no HEAVEN".split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
        val right = "IMAGINE there's no heaven".split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }

        val rightPatch = DiffUtils.diff(base, right).withConflictOutput(Patch.CONFLICT_PRODUCES_MERGE_CONFLICT)
        val applied = rightPatch.applyTo(left)

        assertEquals("IMAGINE there's no HEAVEN", applied.joining(" "))
    }

    private fun List<String>.joining(delimiter: String): String = this.joinToString(delimiter)
}
