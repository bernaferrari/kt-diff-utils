package com.bernaferrari.difflib.algorithm

/**
 * Diff algorithm contract.
 *
 * @param T type that is diffed.
 */
interface DiffAlgorithmI<T> {

    /**
     * Computes the change set required to transform the source list into the target list.
     */
    fun computeDiff(source: List<T>, target: List<T>, progress: DiffAlgorithmListener?): List<Change>

    /**
     * Convenience overload working on arrays.
     */
    fun computeDiff(source: Array<T>, target: Array<T>, progress: DiffAlgorithmListener?): List<Change> =
        computeDiff(source.asList(), target.asList(), progress)
}
