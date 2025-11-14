package com.bernaferrari.difflib.text

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class StringUtilsTest {

    @Test
    fun htmlEntities() {
        assertEquals("&lt;test&gt;", StringUtils.htmlEntites("<test>"))
    }

    @Test
    fun normalizeString() {
        assertEquals("    test", StringUtils.normalize("\ttest"))
    }

    @Test
    fun wrapText() {
        assertEquals("te<br/>st", StringUtils.wrapText("test", 2))
        assertEquals("tes<br/>t", StringUtils.wrapText("test", 3))
        assertEquals("test", StringUtils.wrapText("test", 10))
        assertEquals(".\uD800\uDC01<br/>.", StringUtils.wrapText(".\uD800\uDC01.", 2))
        assertEquals("..<br/>\uD800\uDC01", StringUtils.wrapText("..\uD800\uDC01", 3))
    }

    @Test
    fun wrapTextZero() {
        assertThrows(IllegalArgumentException::class.java) { StringUtils.wrapText("test", -1) }
    }
}
