package com.bernaferrari.difflib.patch

enum class VerifyChunk {
    OK,
    POSITION_OUT_OF_TARGET,
    CONTENT_DOES_NOT_MATCH_TARGET
}
