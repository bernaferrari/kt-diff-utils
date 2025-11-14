package com.bernaferrari.difflib.patch

import java.io.Serializable
import java.util.ArrayList
import java.util.Arrays
import java.util.Objects

/**
 * Holds the information about the part of text involved in the diff process.
 */
class Chunk<T>(
    val position: Int,
    lines: List<T>,
    changePosition: List<Int>? = null
) : Serializable {

    var lines: List<T> = ArrayList(lines)
        set(value) {
            field = ArrayList(value)
        }

    val changePosition: List<Int>? = changePosition?.let { ArrayList(it) }

    constructor(position: Int, lines: Array<T>, changePosition: List<Int>? = null) :
        this(position, Arrays.asList(*lines), changePosition)

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

    override fun hashCode(): Int = Objects.hash(lines, position, size())

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
