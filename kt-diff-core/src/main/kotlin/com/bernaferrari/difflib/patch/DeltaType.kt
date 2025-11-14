package com.bernaferrari.difflib.patch

/**
 * Specifies the type of the delta.
 */
enum class DeltaType {
    CHANGE,
    DELETE,
    INSERT,
    EQUAL
}
