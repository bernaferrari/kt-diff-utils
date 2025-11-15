package com.bernaferrari.difflib.patch

import com.bernaferrari.difflib.platform.PlatformSerializable

/**
 * Holds the information about the part of text involved in the diff process.
 */
class Chunk<T>(
    val position: Int,
    lines: List<T>,
    changePosition: List<Int>? = null
) : PlatformSerializable {

    var lines: List<T> = lines.toList()
        set(value) {
            field = value.toList()
        }

    val changePosition: List<Int>? = changePosition?.toList()

    constructor(position: Int, lines: Array<T>, changePosition: List<Int>? = null) :
        this(position, lines.toList(), changePosition)

    fun verifyChunk(target: List<T>): VerifyChunk = verifyChunk(target, 0, position)

    fun verifyChunk(target: List<T>, fuzz: Int, position: Int): VerifyChunk {
        val startIndex = fuzz
        val lastIndex = size() - fuzz
        val last = position + size() - 1

        if (position + fuzz > target.size || last - fuzz > target.size) {
            return VerifyChunk.POSITION_OUT_OF_TARGET
        }
        for (i in startIndex until lastIndex) {
            if (target[position + i] != lines[i]) {
                return VerifyChunk.CONTENT_DOES_NOT_MATCH_TARGET
            }
        }
        return VerifyChunk.OK
    }

    fun size(): Int = lines.size

    fun last(): Int = position + size() - 1

    override fun hashCode(): Int = 31 * lines.hashCode() + position

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Chunk<*>) {
            return false
        }
        if (position != other.position) {
            return false
        }
        return lines == other.lines
    }

    override fun toString(): String = "[position: $position, size: ${size()}, lines: $lines]"
}
