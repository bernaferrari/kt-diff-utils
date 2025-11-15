package com.bernaferrari.difflib.text

import com.bernaferrari.difflib.DiffUtils
import com.bernaferrari.difflib.TestConstants
import com.bernaferrari.difflib.algorithm.myers.MyersDiffWithLinearSpace
import com.bernaferrari.difflib.text.DiffRow.Tag
import com.bernaferrari.difflib.text.deltamerge.DeltaMergeUtils
import com.bernaferrari.difflib.text.deltamerge.InlineDeltaMergeInfo
import java.nio.file.Files
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DiffRowGeneratorTest {

    @Test
    fun generatorDefault() {
        val first = "anything \n \nother"
        val second = "anything\n\nother"
        val generator = DiffRowGenerator.create().columnWidth(Int.MAX_VALUE).build()
        val rows = generator.generateDiffRows(split(first), split(second))
        assertEquals(3, rows.size)
    }

    @Test
    fun normalizeList() {
        val generator = DiffRowGenerator.create().build()
        assertEquals(listOf("    test"), generator.normalizeLines(listOf("\ttest")))
    }

    @Test
    fun generatorInlineDiff() {
        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .columnWidth(Int.MAX_VALUE)
            .build()
        val rows = generator.generateDiffRows(split("anything \n \nother"), split("anything\n\nother"))
        assertEquals(3, rows.size)
        assertTrue(rows[0].oldLine.contains("<span"))
    }

    @Test
    fun generatorIgnoreWhitespaces() {
        val generator = DiffRowGenerator.create()
            .ignoreWhiteSpaces(true)
            .columnWidth(Int.MAX_VALUE)
            .build()
        val rows = generator.generateDiffRows(
            split("anything \n \nother\nmore lines"),
            split("anything\n\nother\nsome more lines")
        )
        assertEquals(listOf(Tag.EQUAL, Tag.EQUAL, Tag.EQUAL, Tag.CHANGE), rows.map { it.tag })
    }

    @Test
    fun generatorWordWrap() {
        val generator = DiffRowGenerator.create().columnWidth(5).build()
        val rows = generator.generateDiffRows(split("anything \n \nother"), split("anything\n\nother"))
        assertEquals("[CHANGE,anyth<br/>ing ,anyth<br/>ing]", rows[0].toString())
    }

    @Test
    fun generatorWithMerge() {
        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .mergeOriginalRevised(true)
            .build()
        val rows = generator.generateDiffRows(split("anything \n \nother"), split("anything\n\nother"))
        assertEquals(
            "[CHANGE,anything<span class=\"editOldInline\"> </span>,anything]",
            rows[0].toString()
        )
    }

    @Test
    fun generatorWithMergeByWord() {
        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .mergeOriginalRevised(true)
            .inlineDiffByWord(true)
            .build()
        val rows = generator.generateDiffRows(listOf("Test"), listOf("ester"))
        assertEquals(
            "[CHANGE,<span class=\"editOldInline\">Test</span><span class=\"editNewInline\">ester</span>,ester]",
            rows[0].toString()
        )
    }

    @Test
    fun splitStringPreserveDelimiter() {
        val list = DiffRowGenerator.splitStringPreserveDelimiter("test,test2", DiffRowGenerator.SPLIT_BY_WORD_REGEX)
        assertEquals(listOf("test", ",", "test2"), list)
    }

    @Test
    fun generatorExample() {
        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .inlineDiffByWord(true)
            .oldTag { _: Boolean -> "~" }
            .newTag { _: Boolean -> "**" }
            .build()
        val rows = generator.generateDiffRows(
            listOf("This is a test senctence."),
            listOf("This is a test for diffutils.")
        )
        assertEquals("This is a test ~senctence~.", rows[0].oldLine)
        assertEquals("This is a test **for diffutils**.", rows[0].newLine)
    }

    @Test
    fun generatorUnchanged() {
        val generator = DiffRowGenerator.create()
            .columnWidth(5)
            .reportLinesUnchanged(true)
            .build()
        val rows = generator.generateDiffRows(split("anything \n \nother"), split("anything\n\nother"))
        assertEquals(Tag.CHANGE, rows[0].tag)
        assertEquals(Tag.EQUAL, rows[2].tag)
    }

    @Test
    fun generatorIssue14() {
        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .mergeOriginalRevised(true)
            .inlineDiffBySplitter { line ->
                DiffRowGenerator.splitStringPreserveDelimiter(line, Regex(","))
            }
            .oldTag { _: Boolean -> "~" }
            .newTag { _: Boolean -> "**" }
            .build()
        val rows = generator.generateDiffRows(
            listOf("J. G. Feldstein, Chair"),
            listOf("T. P. Pastor, Chair")
        )
        assertEquals("~J. G. Feldstein~**T. P. Pastor**, Chair", rows[0].oldLine)
    }

    @Test
    fun generatorIssue15() {
        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .inlineDiffByWord(true)
            .oldTag { _: Boolean -> "~" }
            .newTag { _: Boolean -> "**" }
            .build()
        val listOne = Files.readAllLines(TestConstants.path("mocks/issue15_1.txt"))
        val listTwo = Files.readAllLines(TestConstants.path("mocks/issue15_2.txt"))
        val rows = generator.generateDiffRows(listOne, listTwo)
        assertEquals(9, rows.size)
    }

    @Test
    fun generatorIssue22() {
        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .inlineDiffByWord(true)
            .oldTag { _: Boolean -> "~" }
            .newTag { _: Boolean -> "**" }
            .build()
        val rows = generator.generateDiffRows(
            listOf("This is a test senctence."),
            listOf("This is a test for diffutils.", "This is the second line.")
        )
        assertEquals(
            "[[CHANGE,This is a test ~senctence~.,This is a test **for diffutils**.], [INSERT,,**This is the second line.**]]",
            rows.toString()
        )
    }

    @Test
    fun generatorIssue41Normalization() {
        val defaultGenerator = DiffRowGenerator.create().build()
        assertEquals("[[EQUAL,&lt;,&lt;]]", defaultGenerator.generateDiffRows(listOf("<"), listOf("<")).toString())

        val custom = DiffRowGenerator.create()
            .lineNormalizer { it.replace("\t", "    ") }
            .build()
        assertEquals("[[EQUAL,<,<]]", custom.generateDiffRows(listOf("<"), listOf("<")).toString())
        assertEquals("[[CHANGE,    <,<]]", custom.generateDiffRows(listOf("\t<"), listOf("<")).toString())
    }

    @Test
    fun generatorIssue44ReportUnchanged() {
        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .reportLinesUnchanged(true)
            .oldTag { _: Boolean -> "~~" }
            .newTag { _: Boolean -> "**" }
            .build()
        val rows = generator.generateDiffRows(listOf("<dt>To do</dt>"), listOf("<dt>Done</dt>"))
        assertEquals("[[CHANGE,<dt>~~T~~o~~ do~~</dt>,<dt>**D**o**ne**</dt>]]", rows.toString())
    }

    @Test
    fun ignoreWhitespaceIssue66() {
        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .inlineDiffByWord(true)
            .ignoreWhiteSpaces(true)
            .mergeOriginalRevised(true)
            .oldTag { _: Boolean -> "~" }
            .newTag { _: Boolean -> "**" }
            .build()
        val rows = generator.generateDiffRows(listOf("This\tis\ta\ttest."), listOf("This is a test"))
        assertEquals("This    is    a    test~.~", rows[0].oldLine)
    }

    @Test
    fun ignoreWhitespaceIssue64() {
        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .inlineDiffByWord(true)
            .ignoreWhiteSpaces(true)
            .mergeOriginalRevised(true)
            .oldTag { _: Boolean -> "~" }
            .newTag { _: Boolean -> "**" }
            .build()
        val rows = generator.generateDiffRows(
            listOf("test", "", "testline"),
            listOf("A new text line", "", "another one")
        )
        assertThat(rows.map { row -> row.oldLine })
            .containsExactly("~test~**A new text line**", "", "~testline~**another one**")
    }

    @Test
    fun replaceDiffsIssue63() {
        val patch = DiffUtils.diff(
            listOf("1", "2", "3", "4", "5"),
            listOf("1", "3", "4", "5")
        )
        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .inlineDiffByWord(true)
            .build()
        val rows = generator.generateDiffRows(listOf("1", "2", "3", "4", "5"), patch.restore(listOf("1", "3", "4", "5")))
        assertEquals(5, rows.size)
    }

    @Test
    fun mergeDeltaForWhitespaceEqualities() {
        val patch = DiffUtils.diff(
            listOf("a ", "b"),
            listOf("a", "b")
        )
        val merged = DiffRowGenerator.WHITESPACE_EQUALITIES_MERGER(
            InlineDeltaMergeInfo(patch.deltas, listOf("a "), listOf("a"))
        )
        assertEquals(1, merged.size)
    }

    @Test
    fun deltaMergeUtils() {
        val deltas = DiffUtils.diff(listOf("a", "b", "c"), listOf("a", "c")).deltas
        val merged = DeltaMergeUtils.mergeInlineDeltas(
            InlineDeltaMergeInfo(deltas, listOf("a", "b", "c"), listOf("a", "c"))
        ) { equalities -> equalities.all { it?.trim().isNullOrEmpty() } }
        assertEquals(1, merged.size)
    }

    private fun split(content: String): List<String> = content.split("\n")
}
