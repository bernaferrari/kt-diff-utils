package com.bernaferrari.difflib.algorithm

import com.bernaferrari.difflib.patch.DeltaType

/**
 * Immutable value describing a change span inside the diff algorithm.
 */
data class Change(
    val deltaType: DeltaType,
    val startOriginal: Int,
    val endOriginal: Int,
    val startRevised: Int,
    val endRevised: Int
) {

    fun withEndOriginal(endOriginal: Int): Change = copy(endOriginal = endOriginal)

    fun withEndRevised(endRevised: Int): Change = copy(endRevised = endRevised)
}
