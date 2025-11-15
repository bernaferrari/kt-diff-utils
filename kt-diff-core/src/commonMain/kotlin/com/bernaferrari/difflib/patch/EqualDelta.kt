package com.bernaferrari.difflib.patch

/**
 * Delta describing equal lines, i.e. no-op.
 */
class EqualDelta<T>(source: Chunk<T>, target: Chunk<T>) :
    AbstractDelta<T>(DeltaType.EQUAL, source, target) {

    @Throws(PatchFailedException::class)
    override fun applyTo(target: MutableList<T>) {
        // nothing to do
    }

    override fun restore(target: MutableList<T>) {
        // nothing to do
    }

    override fun applyFuzzyToAt(target: MutableList<T>, fuzz: Int, position: Int) {
        // nothing to do
    }

    override fun toString(): String =
        "[EqualDelta, position: ${source.position}, lines: ${source.lines}]"

    override fun withChunks(original: Chunk<T>, revised: Chunk<T>): AbstractDelta<T> = EqualDelta(original, revised)
}
