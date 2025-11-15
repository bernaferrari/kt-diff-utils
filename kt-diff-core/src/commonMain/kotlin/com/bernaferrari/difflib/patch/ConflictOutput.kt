package com.bernaferrari.difflib.patch

import com.bernaferrari.difflib.platform.PlatformSerializable

fun interface ConflictOutput<T> : PlatformSerializable {
    @Throws(PatchFailedException::class)
    fun processConflict(verifyChunk: VerifyChunk, delta: AbstractDelta<T>, result: MutableList<T>)
}
