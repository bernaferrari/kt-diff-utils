package com.bernaferrari.difflib.algorithm.myers

import com.bernaferrari.difflib.DiffUtils
import com.bernaferrari.difflib.patch.ChangeDelta
import com.bernaferrari.difflib.patch.Chunk
import com.bernaferrari.difflib.patch.Patch
import com.bernaferrari.difflib.patch.PatchFailedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class WithMyersDiffWithLinearSpacePatchTest {

    @Test
    fun `patch insert`() {
        val insertFrom = listOf("hhh")
        val insertTo = listOf("hhh", "jjj", "kkk", "lll")

        val patch = DiffUtils.diff(insertFrom, insertTo, MyersDiffWithLinearSpace())
        try {
            assertEquals(insertTo, DiffUtils.patch(insertFrom, patch))
        } catch (e: PatchFailedException) {
            fail<Unit>(e.message ?: "Patch application failed for insert case")
        }
    }

    @Test
    fun `patch delete`() {
        val deleteFrom = listOf("ddd", "fff", "ggg", "hhh")
        val deleteTo = listOf("ggg")

        val patch = DiffUtils.diff(deleteFrom, deleteTo, MyersDiffWithLinearSpace())
        try {
            assertEquals(deleteTo, DiffUtils.patch(deleteFrom, patch))
        } catch (e: PatchFailedException) {
            fail<Unit>(e.message ?: "Patch application failed for delete case")
        }
    }

    @Test
    fun `patch change`() {
        val changeFrom = listOf("aaa", "bbb", "ccc", "ddd")
        val changeTo = listOf("aaa", "bxb", "cxc", "ddd")

        val patch = DiffUtils.diff(changeFrom, changeTo, MyersDiffWithLinearSpace())
        try {
            assertEquals(changeTo, DiffUtils.patch(changeFrom, patch))
        } catch (e: PatchFailedException) {
            fail<Unit>(e.message ?: "Patch application failed for change case")
        }
    }

    @Test
    @Throws(PatchFailedException::class)
    fun fuzzyApply() {
        val patch = Patch<String>()
        val deltaFrom = listOf("aaa", "bbb", "ccc", "ddd", "eee", "fff")
        val deltaTo = listOf("aaa", "bbb", "cxc", "dxd", "eee", "fff")
        patch.addDelta(ChangeDelta(Chunk(6, deltaFrom), Chunk(6, deltaTo)))

        val moves = arrayOf(
            intRange(6),
            intRange(3),
            intRange(9),
            intRange(0)
        )

        for (pair in FUZZY_APPLY_TEST_PAIRS) {
            for (move in moves) {
                val from = join(move, pair.from)
                val to = join(move, pair.to)

                for (maxFuzz in 0 until pair.requiredFuzz) {
                    assertThrows(
                        PatchFailedException::class.java,
                        { patch.applyFuzzy(from, maxFuzz) },
                        "fail for $from -> $to for fuzz $maxFuzz required ${pair.requiredFuzz}"
                    )
                }
                for (maxFuzz in pair.requiredFuzz until 4) {
                    assertEquals(to, patch.applyFuzzy(from, maxFuzz), "with $maxFuzz")
                }
            }
        }
    }

    @Test
    @Throws(PatchFailedException::class)
    fun fuzzyApplyTwoSideBySidePatches() {
        val patch = Patch<String>()
        val deltaFrom = listOf("aaa", "bbb", "ccc", "ddd", "eee", "fff")
        val deltaTo = listOf("aaa", "bbb", "cxc", "dxd", "eee", "fff")
        patch.addDelta(ChangeDelta(Chunk(0, deltaFrom), Chunk(0, deltaTo)))
        patch.addDelta(ChangeDelta(Chunk(6, deltaFrom), Chunk(6, deltaTo)))

        assertEquals(join(deltaTo, deltaTo), patch.applyFuzzy(join(deltaFrom, deltaFrom), 0))
    }

    @Test
    @Throws(PatchFailedException::class)
    fun fuzzyApplyToNearest() {
        val patch = Patch<String>()
        val deltaFrom = listOf("aaa", "bbb", "ccc", "ddd", "eee", "fff")
        val deltaTo = listOf("aaa", "bbb", "cxc", "dxd", "eee", "fff")
        patch.addDelta(ChangeDelta(Chunk(0, deltaFrom), Chunk(0, deltaTo)))
        patch.addDelta(ChangeDelta(Chunk(10, deltaFrom), Chunk(10, deltaTo)))

        assertEquals(join(deltaTo, deltaFrom, deltaTo), patch.applyFuzzy(join(deltaFrom, deltaFrom, deltaFrom), 0))
        assertEquals(
            join(intRange(1), deltaTo, deltaFrom, deltaTo),
            patch.applyFuzzy(join(intRange(1), deltaFrom, deltaFrom, deltaFrom), 0)
        )
    }

    @Test
    fun `patch change with exception processor`() {
        val changeFrom = arrayListOf("aaa", "bbb", "ccc", "ddd")
        val changeTo = listOf("aaa", "bxb", "cxc", "ddd")

        val patch = DiffUtils.diff(changeFrom, changeTo, MyersDiffWithLinearSpace())
        changeFrom[2] = "CDC"

        patch.withConflictOutput(Patch.CONFLICT_PRODUCES_MERGE_CONFLICT)

        try {
            val data = DiffUtils.patch(changeFrom, patch)
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
            fail<Unit>(e.message ?: "Patch application failed for conflict output case")
        }
    }

    private data class FuzzyApplyTestPair(val from: List<String>, val to: List<String>, val requiredFuzz: Int)

    private fun intRange(count: Int): List<String> = (0 until count).map { it.toString() }

    private fun join(vararg lists: List<String>): List<String> = lists.flatMap { it }

    companion object {
        private val FUZZY_APPLY_TEST_PAIRS = arrayOf(
            FuzzyApplyTestPair(
                listOf("aaa", "bbb", "ccc", "ddd", "eee", "fff"),
                listOf("aaa", "bbb", "cxc", "dxd", "eee", "fff"),
                0
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bbb", "ccc", "ddd", "eee", "fff"),
                listOf("axa", "bbb", "cxc", "dxd", "eee", "fff"),
                1
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bbb", "ccc", "ddd", "eee", "fxf"),
                listOf("aaa", "bbb", "cxc", "dxd", "eee", "fxf"),
                1
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bbb", "ccc", "ddd", "eee", "fxf"),
                listOf("axa", "bbb", "cxc", "dxd", "eee", "fxf"),
                1
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bxb", "ccc", "ddd", "eee", "fff"),
                listOf("aaa", "bxb", "cxc", "dxd", "eee", "fff"),
                2
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bxb", "ccc", "ddd", "eee", "fff"),
                listOf("axa", "bxb", "cxc", "dxd", "eee", "fff"),
                2
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bbb", "ccc", "ddd", "exe", "fff"),
                listOf("aaa", "bbb", "cxc", "dxd", "exe", "fff"),
                2
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bbb", "ccc", "ddd", "exe", "fff"),
                listOf("axa", "bbb", "cxc", "dxd", "exe", "fff"),
                2
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bxb", "ccc", "ddd", "exe", "fff"),
                listOf("aaa", "bxb", "cxc", "dxd", "exe", "fff"),
                2
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bxb", "ccc", "ddd", "exe", "fff"),
                listOf("axa", "bxb", "cxc", "dxd", "exe", "fff"),
                2
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bxb", "ccc", "ddd", "eee", "fxf"),
                listOf("aaa", "bxb", "cxc", "dxd", "eee", "fxf"),
                2
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bxb", "ccc", "ddd", "eee", "fxf"),
                listOf("axa", "bxb", "cxc", "dxd", "eee", "fxf"),
                2
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bbb", "ccc", "ddd", "exe", "fxf"),
                listOf("aaa", "bbb", "cxc", "dxd", "exe", "fxf"),
                2
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bbb", "ccc", "ddd", "exe", "fxf"),
                listOf("axa", "bbb", "cxc", "dxd", "exe", "fxf"),
                2
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bxb", "ccc", "ddd", "exe", "fxf"),
                listOf("aaa", "bxb", "cxc", "dxd", "exe", "fxf"),
                2
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bxb", "ccc", "ddd", "exe", "fxf"),
                listOf("axa", "bxb", "cxc", "dxd", "exe", "fxf"),
                2
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bbb", "czc", "dzd", "eee", "fff"),
                listOf("aaa", "bbb", "czc", "dzd", "eee", "fff"),
                3
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bbb", "czc", "dzd", "eee", "fff"),
                listOf("axa", "bbb", "czc", "dzd", "eee", "fff"),
                3
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bxb", "czc", "dzd", "eee", "fff"),
                listOf("aaa", "bxb", "czc", "dzd", "eee", "fff"),
                3
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bxb", "czc", "dzd", "eee", "fff"),
                listOf("axa", "bxb", "czc", "dzd", "eee", "fff"),
                3
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bbb", "czc", "dzd", "exe", "fff"),
                listOf("aaa", "bbb", "czc", "dzd", "exe", "fff"),
                3
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bbb", "czc", "dzd", "exe", "fff"),
                listOf("axa", "bbb", "czc", "dzd", "exe", "fff"),
                3
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bxb", "czc", "dzd", "exe", "fff"),
                listOf("aaa", "bxb", "czc", "dzd", "exe", "fff"),
                3
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bxb", "czc", "dzd", "exe", "fff"),
                listOf("axa", "bxb", "czc", "dzd", "exe", "fff"),
                3
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bbb", "czc", "dzd", "eee", "fxf"),
                listOf("aaa", "bbb", "czc", "dzd", "eee", "fxf"),
                3
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bbb", "czc", "dzd", "eee", "fxf"),
                listOf("axa", "bbb", "czc", "dzd", "eee", "fxf"),
                3
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bxb", "czc", "dzd", "eee", "fxf"),
                listOf("aaa", "bxb", "czc", "dzd", "eee", "fxf"),
                3
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bxb", "czc", "dzd", "eee", "fxf"),
                listOf("axa", "bxb", "czc", "dzd", "eee", "fxf"),
                3
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bbb", "czc", "dzd", "exe", "fxf"),
                listOf("aaa", "bbb", "czc", "dzd", "exe", "fxf"),
                3
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bbb", "czc", "dzd", "exe", "fxf"),
                listOf("axa", "bbb", "czc", "dzd", "exe", "fxf"),
                3
            ),
            FuzzyApplyTestPair(
                listOf("aaa", "bxb", "czc", "dzd", "exe", "fxf"),
                listOf("aaa", "bxb", "czc", "dzd", "exe", "fxf"),
                3
            ),
            FuzzyApplyTestPair(
                listOf("axa", "bxb", "czc", "dzd", "exe", "fxf"),
                listOf("axa", "bxb", "czc", "dzd", "exe", "fxf"),
                3
            )
        )
    }
}
