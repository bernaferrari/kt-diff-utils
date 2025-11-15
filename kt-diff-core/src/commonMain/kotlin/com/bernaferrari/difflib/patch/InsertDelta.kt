package com.bernaferrari.difflib.patch

/**
 * Describes the add-delta between original and revised texts.
 */
class InsertDelta<T>(original: Chunk<T>, revised: Chunk<T>) :
    AbstractDelta<T>(DeltaType.INSERT, original, revised) {

    @Throws(PatchFailedException::class)
    override fun applyTo(targetList: MutableList<T>) {
        val position = source.position
        val lines = target.lines
        for (i in lines.indices) {
            targetList.add(position + i, lines[i])
        }
    }

    override fun restore(targetList: MutableList<T>) {
        val position = this.target.position
        repeat(this.target.size()) {
            targetList.removeAt(position)
        }
    }

    override fun toString(): String =
        "[InsertDelta, position: ${source.position}, lines: ${target.lines}]"

    override fun withChunks(original: Chunk<T>, revised: Chunk<T>): AbstractDelta<T> = InsertDelta(original, revised)
}
