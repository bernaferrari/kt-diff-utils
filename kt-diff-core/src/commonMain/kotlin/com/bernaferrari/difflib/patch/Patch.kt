package com.bernaferrari.difflib.patch

import com.bernaferrari.difflib.algorithm.Change
import com.bernaferrari.difflib.platform.PlatformSerializable

/**
 * Describes the patch holding all deltas between the original and revised texts.
 */
class Patch<T> @JvmOverloads constructor(@Suppress("UNUSED_PARAMETER") estimatedPatchSize: Int = 10) :
    PlatformSerializable {

    private val deltaList: MutableList<AbstractDelta<T>> = mutableListOf()
    private var deltasSorted: Boolean = true
    private val conflictProducesException: ConflictOutput<T> =
        ConflictOutput { verifyChunk, _, _ ->
            throw PatchFailedException("could not apply patch due to $verifyChunk")
        }
    private var conflictOutput: ConflictOutput<T> = conflictProducesException

    @Throws(PatchFailedException::class)
    fun applyTo(target: List<T>): List<T> {
        val result = target.toMutableList()
        applyToExisting(result)
        return result
    }

    @Throws(PatchFailedException::class)
    fun applyToExisting(target: MutableList<T>) {
        val ordered = deltas
        for (i in ordered.size - 1 downTo 0) {
            val delta = ordered[i]
            val verify = delta.verifyAndApplyTo(target)
            if (verify != VerifyChunk.OK) {
                conflictOutput.processConflict(verify, delta, target)
            }
        }
    }

    @Throws(PatchFailedException::class)
    fun applyFuzzy(target: List<T>, maxFuzz: Int): List<T> {
        val ctx = PatchApplyingContext(target.toMutableList(), maxFuzz)
        var lastPatchDelta = 0

        for (delta in deltas) {
            ctx.defaultPosition = delta.source.position + lastPatchDelta
            val patchPosition = findPositionFuzzy(ctx, delta)
            if (patchPosition >= 0) {
                delta.applyFuzzyToAt(ctx.result, ctx.currentFuzz, patchPosition)
                lastPatchDelta = patchPosition - delta.source.position
                ctx.lastPatchEnd = delta.source.last() + lastPatchDelta
            } else {
                conflictOutput.processConflict(VerifyChunk.CONTENT_DOES_NOT_MATCH_TARGET, delta, ctx.result)
            }
        }
        return ctx.result
    }

    private fun findPositionFuzzy(ctx: PatchApplyingContext<T>, delta: AbstractDelta<T>): Int {
        for (fuzz in 0..ctx.maxFuzz) {
            ctx.currentFuzz = fuzz
            val foundPosition = findPositionWithFuzz(ctx, delta, fuzz)
            if (foundPosition >= 0) {
                return foundPosition
            }
        }
        return -1
    }

    private fun findPositionWithFuzz(
        ctx: PatchApplyingContext<T>,
        delta: AbstractDelta<T>,
        fuzz: Int
    ): Int {
        if (delta.source.verifyChunk(ctx.result, fuzz, ctx.defaultPosition) == VerifyChunk.OK) {
            return ctx.defaultPosition
        }

        ctx.beforeOutRange = false
        ctx.afterOutRange = false

        var moreDelta = 0
        while (moreDelta >= 0) {
            val pos = findPositionWithFuzzAndMoreDelta(ctx, delta, fuzz, moreDelta)
            if (pos >= 0) {
                return pos
            }
            if (ctx.beforeOutRange && ctx.afterOutRange) {
                break
            }
            moreDelta++
        }
        return -1
    }

    private fun findPositionWithFuzzAndMoreDelta(
        ctx: PatchApplyingContext<T>,
        delta: AbstractDelta<T>,
        fuzz: Int,
        moreDelta: Int
    ): Int {
        if (!ctx.beforeOutRange) {
            val beginAt = ctx.defaultPosition - moreDelta + fuzz
            if (beginAt <= ctx.lastPatchEnd) {
                ctx.beforeOutRange = true
            }
        }

        if (!ctx.afterOutRange) {
            val beginAt = ctx.defaultPosition + moreDelta + delta.source.size() - fuzz
            if (ctx.result.size < beginAt) {
                ctx.afterOutRange = true
            }
        }

        if (!ctx.beforeOutRange) {
            val before = delta.source.verifyChunk(ctx.result, fuzz, ctx.defaultPosition - moreDelta)
            if (before == VerifyChunk.OK) {
                return ctx.defaultPosition - moreDelta
            }
        }

        if (!ctx.afterOutRange) {
            val after = delta.source.verifyChunk(ctx.result, fuzz, ctx.defaultPosition + moreDelta)
            if (after == VerifyChunk.OK) {
                return ctx.defaultPosition + moreDelta
            }
        }
        return -1
    }

    fun withConflictOutput(conflictOutput: ConflictOutput<T>): Patch<T> {
        this.conflictOutput = conflictOutput
        return this
    }

    fun restore(target: List<T>): List<T> {
        val result = target.toMutableList()
        restoreToExisting(result)
        return result
    }

    fun restoreToExisting(target: MutableList<T>) {
        val ordered = deltas
        for (i in ordered.size - 1 downTo 0) {
            ordered[i].restore(target)
        }
    }

    fun addDelta(delta: AbstractDelta<T>) {
        deltaList.add(delta)
        deltasSorted = false
    }

    val deltas: List<AbstractDelta<T>>
        get() {
            if (!deltasSorted && deltaList.size > 1) {
                deltaList.sortBy { it.source.position }
                deltasSorted = true
            } else if (!deltasSorted) {
                deltasSorted = true
            }
            return deltaList
        }

    override fun toString(): String = "Patch{deltas=$deltaList}"

    private class PatchApplyingContext<T>(
        val result: MutableList<T>,
        val maxFuzz: Int
    ) {
        var lastPatchEnd: Int = -1
        var currentFuzz: Int = 0
        var defaultPosition: Int = 0
        var beforeOutRange: Boolean = false
        var afterOutRange: Boolean = false
    }

    companion object {
        @JvmStatic
        val CONFLICT_PRODUCES_MERGE_CONFLICT: ConflictOutput<String> = ConflictOutput { _, delta, result ->
            if (result.size > delta.source.position) {
                val orgData = mutableListOf<String>()
                repeat(delta.source.size()) {
                    orgData.add(result[delta.source.position])
                    result.removeAt(delta.source.position)
                }
                orgData.add(0, "<<<<<< HEAD")
                orgData.add("======")
                orgData.addAll(delta.source.lines)
                orgData.add(">>>>>>> PATCH")
                result.addAll(delta.source.position, orgData)
            } else {
                throw UnsupportedOperationException("Not supported yet.")
            }
        }

        @JvmStatic
        fun <T> generate(original: List<T>, revised: List<T>, changes: List<Change>): Patch<T> =
            generate(original, revised, changes, false)

        private fun <T> buildChunk(start: Int, end: Int, data: List<T>): Chunk<T> =
            Chunk(start, data.subList(start, end).toList())

        @JvmStatic
        fun <T> generate(
            original: List<T>,
            revised: List<T>,
            changesInput: List<Change>,
            includeEquals: Boolean
        ): Patch<T> {
            val patch = Patch<T>(changesInput.size)
            var startOriginal = 0
            var startRevised = 0
            val changes = if (includeEquals) {
                changesInput.toMutableList().also { list ->
                    list.sortBy { it.startOriginal }
                }
            } else {
                changesInput
            }

            for (change in changes) {
                if (includeEquals && startOriginal < change.startOriginal) {
                    patch.addDelta(
                        EqualDelta(
                            buildChunk(startOriginal, change.startOriginal, original),
                            buildChunk(startRevised, change.startRevised, revised)
                        )
                    )
                }

                val orgChunk = buildChunk(change.startOriginal, change.endOriginal, original)
                val revChunk = buildChunk(change.startRevised, change.endRevised, revised)
                when (change.deltaType) {
                    DeltaType.DELETE -> patch.addDelta(DeleteDelta(orgChunk, revChunk))
                    DeltaType.INSERT -> patch.addDelta(InsertDelta(orgChunk, revChunk))
                    DeltaType.CHANGE -> patch.addDelta(ChangeDelta(orgChunk, revChunk))
                    DeltaType.EQUAL -> {
                        if (includeEquals) {
                            patch.addDelta(EqualDelta(orgChunk, revChunk))
                        }
                    }
                }

                startOriginal = change.endOriginal
                startRevised = change.endRevised
            }

            if (includeEquals && startOriginal < original.size) {
                patch.addDelta(
                    EqualDelta(
                        buildChunk(startOriginal, original.size, original),
                        buildChunk(startRevised, revised.size, revised)
                    )
                )
            }
            return patch
        }
    }
}
