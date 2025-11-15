package com.bernaferrari.difflib.patch

/**
 * Describes the change-delta between original and revised texts.
 */
class ChangeDelta<T>(source: Chunk<T>, target: Chunk<T>) : AbstractDelta<T>(DeltaType.CHANGE, source, target) {

    @Throws(PatchFailedException::class)
    override fun applyTo(target: MutableList<T>) {
        val position = source.position
        repeat(source.size()) {
            target.removeAt(position)
        }
        var index = 0
        for (line in this.target.lines) {
            target.add(position + index, line)
            index++
        }
    }

    override fun restore(target: MutableList<T>) {
        val position = this.target.position
        repeat(this.target.size()) {
            target.removeAt(position)
        }
        var index = 0
        for (line in source.lines) {
            target.add(position + index, line)
            index++
        }
    }

    @Throws(PatchFailedException::class)
    override fun applyFuzzyToAt(target: MutableList<T>, fuzz: Int, position: Int) {
        val size = source.size()
        for (i in fuzz until size - fuzz) {
            target.removeAt(position + fuzz)
        }
        var index = fuzz
        for (line in this.target.lines.subList(fuzz, this.target.size() - fuzz)) {
            target.add(position + index, line)
            index++
        }
    }

    override fun toString(): String =
        "[ChangeDelta, position: ${source.position}, lines: ${source.lines} to ${target.lines}]"

    override fun withChunks(original: Chunk<T>, revised: Chunk<T>): AbstractDelta<T> = ChangeDelta(original, revised)
}
