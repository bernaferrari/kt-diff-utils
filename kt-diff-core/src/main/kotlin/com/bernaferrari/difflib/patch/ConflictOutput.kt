package com.bernaferrari.difflib.patch

import java.io.Serializable

fun interface ConflictOutput<T> : Serializable {
    @Throws(PatchFailedException::class)
    fun processConflict(verifyChunk: VerifyChunk, delta: AbstractDelta<T>, result: MutableList<T>)
}
