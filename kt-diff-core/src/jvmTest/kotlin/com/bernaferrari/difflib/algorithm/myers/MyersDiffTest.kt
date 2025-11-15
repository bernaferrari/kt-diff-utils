package com.bernaferrari.difflib.algorithm.myers

import com.bernaferrari.difflib.algorithm.DiffAlgorithmListener
import com.bernaferrari.difflib.patch.Patch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class MyersDiffTest {

    @Test
    fun `diff Myers example forward`() {
        val original = listOf("A", "B", "C", "A", "B", "B", "A")
        val revised = listOf("C", "B", "A", "B", "A", "C")
        val patch = Patch.generate(original, revised, MyersDiff<String>().computeDiff(original, revised, null))
        assertNotNull(patch)
        assertEquals(4, patch.deltas.size)
        assertEquals(
            "Patch{deltas=[[DeleteDelta, position: 0, lines: [A, B]], [InsertDelta, position: 3, lines: [B]], [DeleteDelta, position: 5, lines: [B]], [InsertDelta, position: 7, lines: [C]]]}",
            patch.toString()
        )
    }

    @Test
    fun `diff Myers example forward with listener`() {
        val original = listOf("A", "B", "C", "A", "B", "B", "A")
        val revised = listOf("C", "B", "A", "B", "A", "C")

        val logData = mutableListOf<String>()
        val patch = Patch.generate(
            original,
            revised,
            MyersDiff<String>().computeDiff(
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
        assertEquals(4, patch.deltas.size)
        assertEquals(
            "Patch{deltas=[[DeleteDelta, position: 0, lines: [A, B]], [InsertDelta, position: 3, lines: [B]], [DeleteDelta, position: 5, lines: [B]], [InsertDelta, position: 7, lines: [C]]]}",
            patch.toString()
        )
        assertEquals(8, logData.size)
    }
}
