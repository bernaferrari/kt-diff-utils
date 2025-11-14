package com.bernaferrari.difflib.algorithm.jgit

import com.bernaferrari.difflib.algorithm.DiffAlgorithmListener
import com.bernaferrari.difflib.patch.Patch
import com.bernaferrari.difflib.patch.PatchFailedException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class HistogramDiffTest {

    @Test
    @Throws(PatchFailedException::class)
    fun diff() {
        val orgList = listOf("A", "B", "C", "A", "B", "B", "A")
        val revList = listOf("C", "B", "A", "B", "A", "C")
        val patch = Patch.generate(orgList, revList, HistogramDiff<String>().computeDiff(orgList, revList, null))
        println(patch)
        assertNotNull(patch)
        assertEquals(3, patch.deltas.size)
        assertEquals(
            "Patch{deltas=[[DeleteDelta, position: 0, lines: [A, B]], [DeleteDelta, position: 3, lines: [A, B]], [InsertDelta, position: 7, lines: [B, A, C]]]}",
            patch.toString()
        )
        val patched = patch.applyTo(orgList)
        assertEquals(revList, patched)
    }

    @Test
    @Throws(PatchFailedException::class)
    fun diffWithListener() {
        val orgList = listOf("A", "B", "C", "A", "B", "B", "A")
        val revList = listOf("C", "B", "A", "B", "A", "C")

        val logData = mutableListOf<String>()
        val patch = Patch.generate(
            orgList,
            revList,
            HistogramDiff<String>().computeDiff(orgList, revList, object : DiffAlgorithmListener {
                override fun diffStart() {
                    logData.add("start")
                }

                override fun diffStep(value: Int, max: Int) {
                    logData.add("$value - $max")
                }

                override fun diffEnd() {
                    logData.add("end")
                }
            })
        )
        println(patch)
        assertNotNull(patch)
        assertEquals(3, patch.deltas.size)
        assertEquals(
            "Patch{deltas=[[DeleteDelta, position: 0, lines: [A, B]], [DeleteDelta, position: 3, lines: [A, B]], [InsertDelta, position: 7, lines: [B, A, C]]]}",
            patch.toString()
        )
        val patched = patch.applyTo(orgList)
        assertEquals(revList, patched)
        println(logData)
        assertEquals(19, logData.size)
    }
}
