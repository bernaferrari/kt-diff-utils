package com.bernaferrari.difflib.patch

import com.bernaferrari.difflib.platform.PlatformSerializable

/**
 * Abstract delta between a source and a target.
 */
abstract class AbstractDelta<T>(
    val type: DeltaType,
    val source: Chunk<T>,
    val target: Chunk<T>
) : PlatformSerializable {

    protected open fun verifyChunkToFitTarget(target: List<T>): VerifyChunk = source.verifyChunk(target)

    internal fun verifyAndApplyTo(target: MutableList<T>): VerifyChunk {
        val verify = verifyChunkToFitTarget(target)
        if (verify == VerifyChunk.OK) {
            applyTo(target)
        }
        return verify
    }

    @Throws(PatchFailedException::class)
    internal abstract fun applyTo(target: MutableList<T>)

    internal abstract fun restore(target: MutableList<T>)

    @Throws(PatchFailedException::class)
    internal open fun applyFuzzyToAt(target: MutableList<T>, fuzz: Int, position: Int) {
        throw UnsupportedOperationException("${javaClass.simpleName} does not supports applying patch fuzzy")
    }

    abstract fun withChunks(original: Chunk<T>, revised: Chunk<T>): AbstractDelta<T>

    override fun hashCode(): Int = 31 * (31 * source.hashCode() + target.hashCode()) + type.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val delta = other as AbstractDelta<*>
        return source == delta.source && target == delta.target && type == delta.type
    }
}
