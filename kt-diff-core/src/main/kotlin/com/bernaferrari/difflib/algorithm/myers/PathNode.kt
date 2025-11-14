package com.bernaferrari.difflib.algorithm.myers

/**
 * A node inside the diff path graph.
 */
class PathNode(
    val i: Int,
    val j: Int,
    private val snake: Boolean,
    private val bootstrap: Boolean,
    prev: PathNode?
) {

    val prev: PathNode? = if (snake) prev else prev?.previousSnake()

    fun isSnake(): Boolean = snake

    fun isBootstrap(): Boolean = bootstrap

    fun previousSnake(): PathNode? {
        if (isBootstrap()) {
            return null
        }
        if (!isSnake() && prev != null) {
            return prev.previousSnake()
        }
        return this
    }

    override fun toString(): String {
        val builder = StringBuilder("[")
        var node: PathNode? = this
        while (node != null) {
            builder.append("(")
                .append(node.i)
                .append(",")
                .append(node.j)
                .append(")")
            node = node.prev
        }
        builder.append("]")
        return builder.toString()
    }
}
