package com.bernaferrari.difflib.algorithm.myers

import com.bernaferrari.difflib.DiffUtils
import com.bernaferrari.difflib.algorithm.DiffAlgorithmListener
import com.bernaferrari.difflib.patch.Patch
import java.util.stream.IntStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class MyersDiffWithLinearSpaceTest {

    @Test
    fun `diff Myers example forward`() {
        val original = listOf("A", "B", "C", "A", "B", "B", "A")
        val revised = listOf("C", "B", "A", "B", "A", "C")
        val patch = Patch.generate(
            original,
            revised,
            MyersDiffWithLinearSpace<String>().computeDiff(original, revised, null)
        )
        assertNotNull(patch)
        assertEquals(5, patch.deltas.size)
        assertEquals(
            "Patch{deltas=[[InsertDelta, position: 0, lines: [C]], [DeleteDelta, position: 0, lines: [A]], [DeleteDelta, position: 2, lines: [C]], [DeleteDelta, position: 5, lines: [B]], [InsertDelta, position: 7, lines: [C]]]}",
            patch.toString()
        )
    }

    @Test
    fun `diff Myers example with listener`() {
        val original = listOf("A", "B", "C", "A", "B", "B", "A")
        val revised = listOf("C", "B", "A", "B", "A", "C")

        val logData = mutableListOf<String>()
        val patch = Patch.generate(
            original,
            revised,
            MyersDiffWithLinearSpace<String>().computeDiff(
                original,
                revised,
                object : DiffAlgorithmListener {
                    override fun diffStart() {
                        logData.add("start")
                    }

                    override fun diffStep(value: Int, max: Int) {
                        logData.add("$value - $max")
                    }

                    override fun diffEnd() {
                        logData.add("end")
                    }
                }
            )
        )
        assertNotNull(patch)
        assertEquals(5, patch.deltas.size)
        assertEquals(
            "Patch{deltas=[[InsertDelta, position: 0, lines: [C]], [DeleteDelta, position: 0, lines: [A]], [DeleteDelta, position: 2, lines: [C]], [DeleteDelta, position: 5, lines: [B]], [InsertDelta, position: 7, lines: [C]]]}",
            patch.toString()
        )
        assertEquals(11, logData.size)
    }

    @Test
    fun `performance problem reproduction issue 124`() {
        val old = listOf("abcd")
        val newList = (0 until 90_000).map { it.toString() }
        val diff = DiffUtils.diff(old, newList, MyersDiffWithLinearSpace())
        println("Generated ${diff.deltas.size} deltas for ${newList.size} entries")
    }
}
