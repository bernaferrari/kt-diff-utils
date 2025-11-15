package com.bernaferrari.difflib.patch

import com.bernaferrari.difflib.DiffUtils
import com.bernaferrari.difflib.algorithm.myers.MyersDiff
import com.bernaferrari.difflib.algorithm.myers.MyersDiffWithLinearSpace
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class PatchWithMyerDiffWithLinearSpaceTest {

    @Test
    fun `patch change with linear space algorithm`() {
        val changeTestFrom = arrayListOf("aaa", "bbb", "ccc", "ddd")
        val changeTestTo = listOf("aaa", "bxb", "cxc", "ddd")

        val patch = DiffUtils.diff(changeTestFrom, changeTestTo)
        changeTestFrom[2] = "CDC"
        patch.withConflictOutput(Patch.CONFLICT_PRODUCES_MERGE_CONFLICT)

        try {
            val data = DiffUtils.patch(changeTestFrom, patch)
            assertEquals(11, data.size)
            assertEquals(
                listOf(
                    "aaa",
                    "bxb",
                    "cxc",
                    "<<<<<< HEAD",
                    "bbb",
                    "CDC",
                    "======",
                    "bbb",
                    "ccc",
                    ">>>>>>> PATCH",
                    "ddd"
                ),
                data
            )
        } catch (e: PatchFailedException) {
            fail<Unit>(e.message ?: "Patch application failed for linear space algorithm")
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun setupClass() {
            DiffUtils.withDefaultDiffAlgorithmFactory(MyersDiffWithLinearSpace.factory())
        }

        @JvmStatic
        @AfterAll
        fun resetClass() {
            DiffUtils.withDefaultDiffAlgorithmFactory(MyersDiff.factory())
        }
    }
}
