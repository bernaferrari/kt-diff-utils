package com.bernaferrari.difflib.text.deltamerge

import com.bernaferrari.difflib.patch.AbstractDelta

data class InlineDeltaMergeInfo(
    val deltas: List<AbstractDelta<String>>,
    val origList: List<String>,
    val revList: List<String>
)
