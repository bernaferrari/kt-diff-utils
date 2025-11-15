package com.bernaferrari.difflib.text

import com.bernaferrari.difflib.platform.PlatformSerializable

/**
 * Describes a diff row in form [tag, oldLine, newLine).
 */
data class DiffRow(
    var tag: Tag,
    val oldLine: String,
    val newLine: String
) : PlatformSerializable {

    enum class Tag {
        INSERT,
        DELETE,
        CHANGE,
        EQUAL
    }

    override fun toString(): String = "[$tag,$oldLine,$newLine]"
}
