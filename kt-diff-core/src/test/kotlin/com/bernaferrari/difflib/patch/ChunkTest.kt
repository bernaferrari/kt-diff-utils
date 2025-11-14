package com.bernaferrari.difflib.patch

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ChunkTest {

    @Test
    @Throws(PatchFailedException::class)
    fun verifyChunk() {
        val chunk = Chunk(7, "test".toList())

        // normal check
        assertEquals(VerifyChunk.OK, chunk.verifyChunk("prefix test suffix".toList()))
        assertEquals(
            VerifyChunk.CONTENT_DOES_NOT_MATCH_TARGET,
            chunk.verifyChunk("prefix  es  suffix".toList(), 0, 7)
        )

        // position
        assertEquals(VerifyChunk.OK, chunk.verifyChunk("short test suffix".toList(), 0, 6))
        assertEquals(VerifyChunk.OK, chunk.verifyChunk("loonger test suffix".toList(), 0, 8))
        assertEquals(
            VerifyChunk.CONTENT_DOES_NOT_MATCH_TARGET,
            chunk.verifyChunk("prefix test suffix".toList(), 0, 6)
        )
        assertEquals(
            VerifyChunk.CONTENT_DOES_NOT_MATCH_TARGET,
            chunk.verifyChunk("prefix test suffix".toList(), 0, 8)
        )

        // fuzz
        assertEquals(VerifyChunk.OK, chunk.verifyChunk("prefix test suffix".toList(), 1, 7))
        assertEquals(VerifyChunk.OK, chunk.verifyChunk("prefix  es  suffix".toList(), 1, 7))
        assertEquals(
            VerifyChunk.CONTENT_DOES_NOT_MATCH_TARGET,
            chunk.verifyChunk("prefix      suffix".toList(), 1, 7)
        )
    }
}
