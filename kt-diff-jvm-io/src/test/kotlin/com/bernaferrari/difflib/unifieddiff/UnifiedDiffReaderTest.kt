package com.bernaferrari.difflib.unifieddiff

import com.bernaferrari.difflib.patch.AbstractDelta
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.regex.Pattern

class UnifiedDiffReaderTest {

    @Test
    @Throws(IOException::class)
    fun simpleParse() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("jsqlparser_patch_1.diff")
        )

        assertThat(diff.getFiles().size).isEqualTo(2)
        val file1 = diff.getFiles()[0]
        assertThat(file1.fromFile).isEqualTo("src/main/jjtree/net/sf/jsqlparser/parser/JSqlParserCC.jjt")
        assertThat(file1.patch.deltas.size).isEqualTo(3)
        assertThat(diff.tail).isEqualTo("2.17.1.windows.2\n")
    }

    @Test
    fun parseDiffBlock() {
        val files = UnifiedDiffReader.parseFileNames(
            "diff --git a/src/test/java/net/sf/jsqlparser/statement/select/SelectTest.java b/src/test/java/net/sf/jsqlparser/statement/select/SelectTest.java"
        )
        assertThat(files).containsExactly(
            "src/test/java/net/sf/jsqlparser/statement/select/SelectTest.java",
            "src/test/java/net/sf/jsqlparser/statement/select/SelectTest.java"
        )
    }

    @Test
    fun chunkHeaderParsing() {
        val matcher =
            UnifiedDiffReader.UNIFIED_DIFF_CHUNK_REGEXP.matcher(
                "@@ -189,6 +189,7 @@ TOKEN: /* SQL Keywords. prefixed with K_ to avoid name clashes */"
            )
        assertTrue(matcher.find())
        assertEquals("189", matcher.group(1))
        assertEquals("189", matcher.group(3))
    }

    @Test
    fun chunkHeaderParsing2() {
        val matcher = UnifiedDiffReader.UNIFIED_DIFF_CHUNK_REGEXP.matcher("@@ -189,6 +189,7 @@")
        assertTrue(matcher.find())
        assertEquals("189", matcher.group(1))
        assertEquals("189", matcher.group(3))
    }

    @Test
    fun chunkHeaderParsing3() {
        val matcher = UnifiedDiffReader.UNIFIED_DIFF_CHUNK_REGEXP.matcher("@@ -1,27 +1,27 @@")
        assertTrue(matcher.find())
        assertEquals("1", matcher.group(1))
        assertEquals("1", matcher.group(3))
    }

    @Test
    @Throws(IOException::class)
    fun simpleParse2() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("jsqlparser_patch_1.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(2)
        val file1 = diff.getFiles()[0]
        assertThat(file1.fromFile).isEqualTo("src/main/jjtree/net/sf/jsqlparser/parser/JSqlParserCC.jjt")
        assertThat(file1.patch.deltas.size).isEqualTo(3)
        val first: AbstractDelta<String> = file1.patch.deltas[0]
        assertThat(first.source.size()).isGreaterThan(0)
        assertThat(first.target.size()).isGreaterThan(0)
        assertThat(diff.tail).isEqualTo("2.17.1.windows.2\n")
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue201() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("jsqlparser_patch_1.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(2)
        val file1 = diff.getFiles()[0]
        assertThat(file1.fromFile).isEqualTo("src/main/jjtree/net/sf/jsqlparser/parser/JSqlParserCC.jjt")
        assertThat(file1.patch.deltas.size).isEqualTo(3)
        assertThat(file1.patch.deltas[0].source.size()).isGreaterThan(0)
        assertThat(file1.patch.deltas[0].target.size()).isGreaterThan(0)
        assertThat(diff.tail).isEqualTo("2.17.1.windows.2\n")
    }

    @Test
    fun simplePattern() {
        val pattern = Pattern.compile("^\\+\\+\\+\\s")
        val matcher = pattern.matcher("+++ revised.txt")
        assertTrue(matcher.find())
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue46() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue46.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(1)
        val file1 = diff.getFiles()[0]
        assertThat(file1.fromFile).isEqualTo("a.vhd")
        assertThat(file1.patch.deltas.size).isEqualTo(1)
        assertThat(diff.tail).isNull()
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue33() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue33.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(1)
        val file1 = diff.getFiles()[0]
        assertThat(file1.fromFile).isEqualTo("Main.java")
        assertThat(file1.patch.deltas.size).isEqualTo(1)
        assertThat(diff.tail).isNull()
        assertThat(diff.header).isNull()
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue51() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue51.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(2)
        val file1 = diff.getFiles()[0]
        assertThat(file1.fromFile).isEqualTo("f1")
        assertThat(file1.patch.deltas.size).isEqualTo(1)
        val file2 = diff.getFiles()[1]
        assertThat(file2.fromFile).isEqualTo("f2")
        assertThat(file2.patch.deltas.size).isEqualTo(1)
        assertThat(diff.tail).isNull()
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue79() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue79.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(1)
        val file1 = diff.getFiles()[0]
        assertThat(file1.fromFile).isEqualTo("test/Issue.java")
        assertThat(file1.patch.deltas.size).isEqualTo(0)
        assertThat(diff.tail).isNull()
        assertThat(diff.header).isNull()
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue84() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue84.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(2)
        val file1 = diff.getFiles()[0]
        assertThat(file1.fromFile).isEqualTo("config/ant-phase-verify.xml")
        assertThat(file1.patch.deltas.size).isEqualTo(1)
        val file2 = diff.getFiles()[1]
        assertThat(file2.fromFile).isEqualTo("/dev/null")
        assertThat(file2.patch.deltas.size).isEqualTo(1)
        assertThat(diff.tail).isEqualTo("2.7.4")
        assertThat(diff.header)
            .startsWith("From b53e612a2ab5ff15d14860e252f84c0f343fe93a Mon Sep 17 00:00:00 2001")
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue85() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue85.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(1)
        val file1 = diff.getFiles()[0]
        assertEquals("diff -r 83e41b73d115 -r a4438263b228 tests/test-check-pyflakes.t", file1.diffCommand)
        assertEquals("tests/test-check-pyflakes.t", file1.fromFile)
        assertEquals("tests/test-check-pyflakes.t", file1.toFile)
        assertEquals(1, file1.patch.deltas.size)
        assertNull(diff.tail)
    }

    @Test
    fun timeStampRegexp() {
        assertThat("2019-04-18 13:49:39.516149751 +0200").matches(UnifiedDiffReader.TIMESTAMP_REGEXP)
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue98() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue98.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(1)
        val file1 = diff.getFiles()[0]
        assertEquals("100644", file1.deletedFileMode)
        assertEquals("src/test/java/se/bjurr/violations/lib/model/ViolationTest.java", file1.fromFile)
        assertThat(diff.tail).isEqualTo("2.25.1")
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue104() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_parsing_issue104.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(6)
        val file = diff.getFiles()[2]
        assertThat(file.fromFile).isEqualTo("/dev/null")
        assertThat(file.toFile).isEqualTo("doc/samba_data_tool_path.xml.in")
        assertThat(file.patch.toString())
            .isEqualTo("Patch{deltas=[[ChangeDelta, position: 0, lines: [] to [@SAMBA_DATA_TOOL@]]]}") 
        assertThat(diff.tail).isEqualTo("2.14.4")
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue107BazelDiff() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("01-bazel-strip-unused.patch_issue107.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(450)
        val file = diff.getFiles()[0]
        assertThat(file.fromFile).isEqualTo("./src/main/java/com/amazonaws/AbortedException.java")
        assertThat(file.toFile)
            .isEqualTo("/home/greg/projects/bazel/third_party/aws-sdk-auth-lite/src/main/java/com/amazonaws/AbortedException.java")
        assertThat(diff.getFiles().count { file -> file.noNewLineAtTheEndOfTheFile }).isEqualTo(48)
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue107_2() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue107.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(2)
        val file1 = diff.getFiles()[0]
        assertThat(file1.fromFile).isEqualTo("Main.java")
        assertThat(file1.patch.deltas.size).isEqualTo(1)
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue107_3() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue107_3.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(1)
        val file1 = diff.getFiles()[0]
        assertThat(file1.fromFile).isEqualTo("Billion laughs attack.md")
        assertThat(file1.patch.deltas.size).isEqualTo(1)
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue107_4() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue107_4.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(27)
        assertThat(diff.getFiles().map { it.fromFile }).contains("README.md")
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue107_5() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue107_5.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(22)
        assertThat(diff.getFiles().map { it.fromFile })
            .contains("rt/management/src/test/java/org/apache/cxf/management/jmx/MBServerConnectorFactoryTest.java")
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue110() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("0001-avahi-python-Use-the-agnostic-DBM-interface.patch")
        )
        assertThat(diff.getFiles().size).isEqualTo(5)
        val file = diff.getFiles()[4]
        assertThat(file.similarityIndex).isEqualTo(87)
        assertThat(file.renameFrom).isEqualTo("service-type-database/build-db.in")
        assertThat(file.renameTo).isEqualTo("service-type-database/build-db")
        assertThat(file.fromFile).isEqualTo("service-type-database/build-db.in")
        assertThat(file.toFile).isEqualTo("service-type-database/build-db")
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue117() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue117.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(2)
        assertThat(diff.getFiles()[0].patch.deltas[0].source.changePosition).containsExactly(24, 27)
        assertThat(diff.getFiles()[0].patch.deltas[0].target.changePosition).containsExactly(24, 27)
        assertThat(diff.getFiles()[0].patch.deltas[1].source.changePosition).containsExactly(64)
        assertThat(diff.getFiles()[0].patch.deltas[1].target.changePosition)
            .containsExactly(64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74)
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue122() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue122.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(1)
        assertThat(diff.getFiles().map { it.fromFile }).contains("coders/wpg.c")
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue123() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue123.diff")
        )
        assertThat(diff.getFiles().size).isEqualTo(2)
        assertThat(diff.getFiles().map { it.fromFile })
            .contains("src/java/main/org/apache/zookeeper/server/FinalRequestProcessor.java")
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue141() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue141.diff")
        )
        val file1 = diff.getFiles()[0]
        assertThat(file1.fromFile).isEqualTo("a.txt")
        assertThat(file1.toFile).isEqualTo("a1.txt")
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue182Add() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue182_add.diff")
        )
        assertThat(diff.getFiles()[0].binaryAdded).isEqualTo("some-image.png")
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue182Delete() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue182_delete.diff")
        )
        assertThat(diff.getFiles()[0].binaryDeleted).isEqualTo("some-image.png")
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue182Edit() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue182_edit.diff")
        )
        assertThat(diff.getFiles()[0].binaryEdited).isEqualTo("some-image.png")
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue182Mode() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_issue182_mode.diff")
        )
        val file1 = diff.getFiles()[0]
        assertThat(file1.oldMode).isEqualTo("100644")
        assertThat(file1.newMode).isEqualTo("100755")
    }

    @Test
    @Throws(IOException::class)
    fun parseIssue193Copy() {
        val diff = UnifiedDiffReader.parseUnifiedDiff(
            javaClass.getResourceAsStream("problem_diff_parsing_issue193.diff")
        )
        val file1 = diff.getFiles()[0]
        assertThat(file1.copyFrom).isEqualTo("modules/configuration/config/web/pcf/account/AccountContactCV.pcf")
        assertThat(file1.copyTo)
            .isEqualTo("modules/configuration/config/web/pcf/account/AccountContactCV.default.pcf")
    }
}
