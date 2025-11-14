package com.bernaferrari.difflib.algorithm.myers

import com.bernaferrari.difflib.algorithm.Change
import com.bernaferrari.difflib.algorithm.DiffAlgorithmFactory
import com.bernaferrari.difflib.algorithm.DiffAlgorithmI
import com.bernaferrari.difflib.algorithm.DiffAlgorithmListener
import com.bernaferrari.difflib.algorithm.Equalizer
import com.bernaferrari.difflib.patch.DeltaType
import java.util.ArrayList
import java.util.HashMap
import java.util.Objects
import java.util.RandomAccess

/**
 * A clean-room implementation of Eugene Myers greedy differencing algorithm.
 */
class MyersDiff<T> @JvmOverloads constructor(
    private val equalizer: Equalizer<T> = defaultEqualizer()
) : DiffAlgorithmI<T> {

    private val usesDefaultEqualizer: Boolean = equalizer === DEFAULT_EQUALIZER

    override fun computeDiff(
        source: List<T>,
        target: List<T>,
        progress: DiffAlgorithmListener?
    ): List<Change> {
        val normalizedSource = ensureRandomAccess(source)
        val normalizedTarget = ensureRandomAccess(target)
        progress?.diffStart()
        val trimmed = trimEdges(normalizedSource, normalizedTarget)
        if (trimmed.source.isEmpty() || trimmed.target.isEmpty()) {
            val trivial = buildBasicDiff(trimmed.source, trimmed.target, progress)
            val rel = applyOffset(trivial, trimmed.offset, trimmed.offset)
            progress?.diffEnd()
            return rel
        }
        val anchors = if (usesDefaultEqualizer) {
            findAnchors(trimmed.source, trimmed.target)
        } else {
            emptyList()
        }
        val result =
            if (anchors.isEmpty()) {
                buildBasicDiff(trimmed.source, trimmed.target, progress)
            } else {
                buildDiffWithAnchors(trimmed.source, trimmed.target, anchors)
            }
        val finalResult = applyOffset(result, trimmed.offset, trimmed.offset)
        progress?.diffEnd()
        return finalResult
    }

    private fun buildPath(
        orig: List<T>,
        rev: List<T>,
        progress: DiffAlgorithmListener?
    ): PathNode {
        val n = orig.size
        val m = rev.size
        val max = n + m + 1
        val size = 1 + 2 * max
        val middle = size / 2
        val diagonal = arrayOfNulls<PathNode>(size)
        diagonal[middle + 1] = PathNode(0, -1, true, true, null)

        for (d in 0 until max) {
            progress?.diffStep(d, max)
            var k = -d
            while (k <= d) {
                val kmiddle = middle + k
                val kplus = kmiddle + 1
                val kminus = kmiddle - 1

                val usePlus = k == -d || (k != d && (diagonal[kminus]?.i ?: Int.MIN_VALUE) < (diagonal[kplus]?.i
                    ?: Int.MIN_VALUE))

                val prevNode: PathNode
                var i: Int
                if (usePlus) {
                    val node = diagonal[kplus] ?: throw NullPointerException("diagonal[$kplus] is null")
                    i = node.i
                    prevNode = node
                } else {
                    val node = diagonal[kminus] ?: throw NullPointerException("diagonal[$kminus] is null")
                    i = node.i + 1
                    prevNode = node
                }

                diagonal[kminus] = null
                var j = i - k
                var node = PathNode(i, j, false, false, prevNode)

                while (i < n && j < m && equalizer(orig[i], rev[j])) {
                    i++
                    j++
                }

                if (i != node.i) {
                    node = PathNode(i, j, true, false, node)
                }

                diagonal[kmiddle] = node

                if (i >= n && j >= m) {
                    return node
                }

                k += 2
            }
            diagonal[middle + d - 1] = null
        }
        throw IllegalStateException("could not find a diff path")
    }

    private fun buildRevision(actualPath: PathNode, orig: List<T>, rev: List<T>): List<Change> {
        var path: PathNode? = actualPath
        val changes = ArrayList<Change>()
        if (path?.isSnake() == true) {
            path = path.prev
        }
        while (path != null && path.prev != null && path.prev!!.j >= 0) {
            if (path.isSnake()) {
                throw IllegalStateException("bad diffpath: found snake when looking for diff")
            }
            val i = path.i
            val j = path.j

            path = path.prev
            val iAnchor = path!!.i
            val jAnchor = path.j

            val deltaType = when {
                iAnchor == i && jAnchor != j -> DeltaType.INSERT
                iAnchor != i && jAnchor == j -> DeltaType.DELETE
                else -> DeltaType.CHANGE
            }
            changes.add(Change(deltaType, iAnchor, i, jAnchor, j))

            if (path.isSnake()) {
                path = path.prev
            }
        }
        return changes
    }

    private fun buildBasicDiff(
        source: List<T>,
        target: List<T>,
        progress: DiffAlgorithmListener?
    ): List<Change> {
        if (source.isEmpty() && target.isEmpty()) {
            return emptyList()
        }
        if (source.isEmpty()) {
            return listOf(Change(DeltaType.INSERT, 0, 0, 0, target.size))
        }
        if (target.isEmpty()) {
            return listOf(Change(DeltaType.DELETE, 0, source.size, 0, 0))
        }
        val path = buildPath(source, target, progress)
        return buildRevision(path, source, target)
    }

    private data class TrimmedLists<T>(
        val source: List<T>,
        val target: List<T>,
        val offset: Int
    )

    private fun <T> ensureRandomAccess(list: List<T>): List<T> =
        if (list is RandomAccess) list else ArrayList(list)

    private fun applyOffset(
        changes: List<Change>,
        originalOffset: Int,
        revisedOffset: Int
    ): List<Change> {
        if ((originalOffset == 0 && revisedOffset == 0) || changes.isEmpty()) {
            return changes
        }
        return changes.map { change ->
            Change(
                change.deltaType,
                change.startOriginal + originalOffset,
                change.endOriginal + originalOffset,
                change.startRevised + revisedOffset,
                change.endRevised + revisedOffset
            )
        }
    }

    private fun buildDiffWithAnchors(
        source: List<T>,
        target: List<T>,
        anchors: List<Anchor>
    ): List<Change> {
        val result = ArrayList<Change>()
        var sourceStart = 0
        var targetStart = 0
        for (anchor in anchors) {
            if (anchor.sourceIndex > sourceStart || anchor.targetIndex > targetStart) {
                val segment = buildBasicDiff(
                    source.subList(sourceStart, anchor.sourceIndex),
                    target.subList(targetStart, anchor.targetIndex),
                    null
                )
                if (segment.isNotEmpty()) {
                    result.addAll(applyOffset(segment, sourceStart, targetStart))
                }
            }
            sourceStart = anchor.sourceIndex + 1
            targetStart = anchor.targetIndex + 1
        }
        if (sourceStart < source.size || targetStart < target.size) {
            val tail = buildBasicDiff(
                source.subList(sourceStart, source.size),
                target.subList(targetStart, target.size),
                null
            )
            if (tail.isNotEmpty()) {
                result.addAll(applyOffset(tail, sourceStart, targetStart))
            }
        }
        return result
    }

    private fun findAnchors(source: List<T>, target: List<T>): List<Anchor> {
        val minLength = minOf(source.size, target.size)
        if (minLength < 256) {
            return emptyList()
        }
        val maxUniqueWindow = 4096
        val sourceCounts = HashMap<T, Int>(minOf(source.size, maxUniqueWindow))
        val targetCounts = HashMap<T, Int>(minOf(target.size, maxUniqueWindow))
        for (i in 0 until minOf(source.size, maxUniqueWindow)) {
            val value = source[i]
            sourceCounts[value] = (sourceCounts[value] ?: 0) + 1
        }
        for (i in 0 until minOf(target.size, maxUniqueWindow)) {
            val value = target[i]
            targetCounts[value] = (targetCounts[value] ?: 0) + 1
        }
        val uniqueSourcePositions = HashMap<T, Int>()
        source.forEachIndexed { index, value ->
            if (sourceCounts[value] == 1) {
                uniqueSourcePositions[value] = index
            }
        }
        val candidates = ArrayList<Anchor>()
        target.forEachIndexed { index, value ->
            if (sourceCounts[value] == 1 && targetCounts[value] == 1) {
                val sourceIndex = uniqueSourcePositions[value]
                if (sourceIndex != null) {
                    candidates.add(Anchor(sourceIndex, index))
                }
            }
        }
        if (candidates.size < 32) {
            return emptyList()
        }
        val anchors = longestIncreasingSubsequence(candidates)
        return if (anchors.size < 4) emptyList() else anchors
    }

    private fun longestIncreasingSubsequence(candidates: List<Anchor>): List<Anchor> {
        val size = candidates.size
        val predecessors = IntArray(size) { -1 }
        val tails = IntArray(size)
        var length = 0
        for (i in 0 until size) {
            val value = candidates[i].sourceIndex
            var low = 0
            var high = length
            while (low < high) {
                val mid = (low + high) ushr 1
                val midValue = candidates[tails[mid]].sourceIndex
                if (midValue < value) {
                    low = mid + 1
                } else {
                    high = mid
                }
            }
            if (low > 0) {
                predecessors[i] = tails[low - 1]
            }
            tails[low] = i
            if (low == length) {
                length++
            }
        }
        if (length == 0) {
            return emptyList()
        }
        var idx = tails[length - 1]
        val result = ArrayList<Anchor>(length)
        while (idx >= 0) {
            result.add(candidates[idx])
            idx = predecessors[idx]
        }
        result.reverse()
        return result
    }


    private fun trimEdges(source: List<T>, target: List<T>): TrimmedLists<T> {
        var start = 0
        val maxPrefix = minOf(source.size, target.size)
        while (start < maxPrefix && equalizer(source[start], target[start])) {
            start++
        }
        var endSource = source.size
        var endTarget = target.size
        while (
            endSource > start &&
            endTarget > start &&
            equalizer(source[endSource - 1], target[endTarget - 1])
        ) {
            endSource--
            endTarget--
        }
        val trimmedSource = source.subList(start, endSource)
        val trimmedTarget = target.subList(start, endTarget)
        return TrimmedLists(trimmedSource, trimmedTarget, start)
    }

    private data class Anchor(val sourceIndex: Int, val targetIndex: Int)

    companion object {
        private val DEFAULT_EQUALIZER: Equalizer<Any?> = { a, b -> Objects.equals(a, b) }

        fun factory(): DiffAlgorithmFactory = object : DiffAlgorithmFactory {
            override fun <T> create(): DiffAlgorithmI<T> = MyersDiff()

            override fun <T> create(equalizer: Equalizer<T>): DiffAlgorithmI<T> = MyersDiff(equalizer)
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> defaultEqualizer(): Equalizer<T> =
            DEFAULT_EQUALIZER as Equalizer<T>
    }
}
