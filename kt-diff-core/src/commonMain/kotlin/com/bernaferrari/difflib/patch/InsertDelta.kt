package com.bernaferrari.difflib.patch

/**
 * Describes the add-delta between original and revised texts.
 */
class InsertDelta<T>(sourceChunk: Chunk<T>, targetChunk: Chunk<T>) :
    AbstractDelta<T>(DeltaType.INSERT, sourceChunk, targetChunk) {

    @Throws(PatchFailedException::class)
    override fun applyTo(target: MutableList<T>) {
        val position = source.position
        val lines = this.target.lines
        for (i in lines.indices) {
            target.add(position + i, lines[i])
        }
    }

    override fun restore(target: MutableList<T>) {
        val position = this.target.position
        repeat(this.target.size()) {
            target.removeAt(position)
        }
    }

    override fun toString(): String =
        "[InsertDelta, position: ${source.position}, lines: ${target.lines}]"

    override fun withChunks(original: Chunk<T>, revised: Chunk<T>): AbstractDelta<T> = InsertDelta(original, revised)
}
