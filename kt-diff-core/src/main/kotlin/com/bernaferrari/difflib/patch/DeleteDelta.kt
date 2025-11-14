package com.bernaferrari.difflib.patch

/**
 * Describes the delete-delta between original and revised texts.
 */
class DeleteDelta<T>(original: Chunk<T>, revised: Chunk<T>) :
    AbstractDelta<T>(DeltaType.DELETE, original, revised) {

    @Throws(PatchFailedException::class)
    override fun applyTo(target: MutableList<T>) {
        val position = source.position
        repeat(source.size()) {
            target.removeAt(position)
        }
    }

    override fun restore(target: MutableList<T>) {
        val position = this.target.position
        val lines = source.lines
        for (i in lines.indices) {
            target.add(position + i, lines[i])
        }
    }

    override fun toString(): String =
        "[DeleteDelta, position: ${source.position}, lines: ${source.lines}]"

    override fun withChunks(original: Chunk<T>, revised: Chunk<T>): AbstractDelta<T> = DeleteDelta(original, revised)
}
