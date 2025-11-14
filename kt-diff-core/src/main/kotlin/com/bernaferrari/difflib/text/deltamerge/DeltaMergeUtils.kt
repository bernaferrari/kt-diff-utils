package com.bernaferrari.difflib.text.deltamerge

import com.bernaferrari.difflib.patch.AbstractDelta
import com.bernaferrari.difflib.patch.ChangeDelta
import com.bernaferrari.difflib.patch.Chunk
import java.util.ArrayList
import java.util.function.Predicate

object DeltaMergeUtils {
    fun mergeInlineDeltas(
        deltaMergeInfo: InlineDeltaMergeInfo,
        replaceEquality: Predicate<List<String>>
    ): List<AbstractDelta<String>> {
        val originalDeltas = deltaMergeInfo.deltas
        if (originalDeltas.size < 2) {
            return originalDeltas
        }
        val newDeltas = ArrayList<AbstractDelta<String>>()
        newDeltas.add(originalDeltas[0])
        for (i in 1 until originalDeltas.size) {
            val previousDelta = newDeltas[newDeltas.size - 1]
            val currentDelta = originalDeltas[i]
            val equalities = deltaMergeInfo.origList.subList(
                previousDelta.source.position + previousDelta.source.size(),
                currentDelta.source.position
            )
            if (replaceEquality.test(equalities)) {
                val allSourceLines = ArrayList<String>().apply {
                    addAll(previousDelta.source.lines)
                    addAll(equalities)
                    addAll(currentDelta.source.lines)
                }
                val allTargetLines = ArrayList<String>().apply {
                    addAll(previousDelta.target.lines)
                    addAll(equalities)
                    addAll(currentDelta.target.lines)
                }
                val replacement = ChangeDelta(
                    Chunk(previousDelta.source.position, allSourceLines),
                    Chunk(previousDelta.target.position, allTargetLines)
                )
                newDeltas.removeAt(newDeltas.size - 1)
                newDeltas.add(replacement)
            } else {
                newDeltas.add(currentDelta)
            }
        }
        return newDeltas
    }
}
