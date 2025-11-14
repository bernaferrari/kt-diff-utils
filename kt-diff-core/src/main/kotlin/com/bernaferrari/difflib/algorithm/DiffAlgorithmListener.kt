package com.bernaferrari.difflib.algorithm

/**
 * Listener for reporting diff algorithm progress.
 */
interface DiffAlgorithmListener {

    fun diffStart() {}

    /**
     * Reports a step inside the diff algorithm.
     */
    fun diffStep(value: Int, max: Int) {}

    fun diffEnd() {}
}
