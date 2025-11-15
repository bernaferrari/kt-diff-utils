package com.bernaferrari.difflib

import com.bernaferrari.difflib.patch.AbstractDelta
import com.bernaferrari.difflib.patch.ChangeDelta
import com.bernaferrari.difflib.patch.Chunk
import com.bernaferrari.difflib.patch.DeleteDelta
import com.bernaferrari.difflib.patch.EqualDelta
import com.bernaferrari.difflib.patch.InsertDelta
import com.bernaferrari.difflib.patch.Patch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.ZipFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class DiffUtilsTest {

    @Test
    fun `diff insert`() {
        val patch = DiffUtils.diff(listOf("hhh"), listOf("hhh", "jjj", "kkk"))
        assertNotNull(patch)
        assertEquals(1, patch.deltas.size)
        val delta = patch.deltas[0]
        assertTrue(delta is InsertDelta<*>)
        assertEquals(Chunk(1, emptyList<String>()), delta.source)
        assertEquals(Chunk(1, listOf("jjj", "kkk")), delta.target)
    }

    @Test
    fun `diff delete`() {
        val patch = DiffUtils.diff(listOf("ddd", "fff", "ggg"), listOf("ggg"))
        assertNotNull(patch)
        assertEquals(1, patch.deltas.size)
        val delta = patch.deltas[0]
        assertTrue(delta is DeleteDelta<*>)
        assertEquals(Chunk(0, listOf("ddd", "fff")), delta.source)
        assertEquals(Chunk(0, emptyList<String>()), delta.target)
    }

    @Test
    fun `diff change`() {
        val from = listOf("aaa", "bbb", "ccc")
        val to = listOf("aaa", "zzz", "ccc")

        val patch = DiffUtils.diff(from, to)
        assertNotNull(patch)
        assertEquals(1, patch.deltas.size)
        val delta = patch.deltas[0]
        assertTrue(delta is ChangeDelta<*>)
        assertEquals(Chunk(1, listOf("bbb")), delta.source)
        assertEquals(Chunk(1, listOf("zzz")), delta.target)
    }

    @Test
    fun `diff empty list`() {
        val patch = DiffUtils.diff(emptyList<String>(), emptyList())
        assertNotNull(patch)
        assertEquals(0, patch.deltas.size)
    }

    @Test
    fun `diff empty vs non-empty`() {
        val patch = DiffUtils.diff(emptyList<String>(), listOf("aaa"))
        assertNotNull(patch)
        assertEquals(1, patch.deltas.size)
        assertTrue(patch.deltas[0] is InsertDelta<*>)
    }

    @Test
    fun `diff inline simple`() {
        val patch = DiffUtils.diffInline("", "test")
        assertEquals(1, patch.deltas.size)
        val delta = patch.deltas[0]
        assertTrue(delta is InsertDelta<*>)
        assertEquals(0, delta.source.position)
        assertEquals(0, delta.source.lines.size)
        assertEquals("test", delta.target.lines[0])
    }

    @Test
    fun `diff inline insert middle`() {
        val patch = DiffUtils.diffInline("es", "fest")
        assertEquals(2, patch.deltas.size)
        assertTrue(patch.deltas[0] is InsertDelta<*>)
        assertEquals(0, patch.deltas[0].source.position)
        assertEquals(2, patch.deltas[1].source.position)
        assertEquals("f", patch.deltas[0].target.lines[0])
        assertEquals("t", patch.deltas[1].target.lines[0])
    }

    @Test
    fun `diff integer list`() {
        val original = listOf(1, 2, 3, 4, 5)
        val revised = listOf(2, 3, 4, 6)

        val patch = DiffUtils.diff(original, revised)

        assertEquals(2, patch.deltas.size)
        assertEquals("[DeleteDelta, position: 0, lines: [1]]", patch.deltas[0].toString())
        assertEquals("[ChangeDelta, position: 4, lines: [5] to [6]]", patch.deltas[1].toString())
    }

    @Test
    fun `diff detects change across multiple lines`() {
        val original = listOf("line1", "line2", "line3")
        val revised = listOf("line1", "line2-2", "line4")

        val patch = DiffUtils.diff(original, revised)
        assertEquals(1, patch.deltas.size)
        assertEquals(
            "[ChangeDelta, position: 1, lines: [line2, line3] to [line2-2, line4]]",
            patch.deltas[0].toString()
        )
    }

    /**
     * To test this, the greedy Myer algorithm is not suitable.
     */
    @Test
    @Disabled("Relies on large dataset resource")
    @Throws(IOException::class)
    fun `diff handles large dataset`() {
        ZipFile("${TestConstants.MOCK_FOLDER}large_dataset1.zip").use { zip ->
            val patch = DiffUtils.diff(
                readStringListFromInputStream(zip.getInputStream(zip.getEntry("ta"))),
                readStringListFromInputStream(zip.getInputStream(zip.getEntry("tb")))
            )
            assertEquals(1, patch.deltas.size)
        }
    }

    @Test
    fun `diff Myers example`() {
        val patch = DiffUtils.diff(
            listOf("A", "B", "C", "A", "B", "B", "A"),
            listOf("C", "B", "A", "B", "A", "C")
        )
        assertNotNull(patch)
        assertEquals(4, patch.deltas.size)
        assertEquals(
            "Patch{deltas=[[DeleteDelta, position: 0, lines: [A, B]], [InsertDelta, position: 3, lines: [B]], [DeleteDelta, position: 5, lines: [B]], [InsertDelta, position: 7, lines: [C]]]}",
            patch.toString()
        )
    }

    @Test
    fun `diff equal`() {
        val patch = DiffUtils.diff(listOf("hhh", "jjj", "kkk"), listOf("hhh", "jjj", "kkk"), true)
        assertNotNull(patch)
        assertEquals(1, patch.deltas.size)
        val delta = patch.deltas[0]
        assertTrue(delta is EqualDelta<*>)
        assertEquals(Chunk(0, listOf("hhh", "jjj", "kkk")), delta.source)
        assertEquals(Chunk(0, listOf("hhh", "jjj", "kkk")), delta.target)
    }

    @Test
    fun `diff equal with insert`() {
        val patch = DiffUtils.diff(listOf("hhh"), listOf("hhh", "jjj", "kkk"), true)
        assertNotNull(patch)
        assertEquals(2, patch.deltas.size)
        var delta: AbstractDelta<String> = patch.deltas[0]
        assertTrue(delta is EqualDelta<*>)
        assertEquals(Chunk(0, listOf("hhh")), delta.source)
        assertEquals(Chunk(0, listOf("hhh")), delta.target)

        delta = patch.deltas[1]
        assertTrue(delta is InsertDelta<*>)
        assertEquals(Chunk(1, emptyList<String>()), delta.source)
        assertEquals(Chunk(1, listOf("jjj", "kkk")), delta.target)
    }

    @Test
    fun `diff problem issue 42`() {
        val patch = DiffUtils.diff(
            listOf("The", "dog", "is", "brown"),
            listOf("The", "fox", "is", "down"),
            true
        )

        assertNotNull(patch)
        assertEquals(4, patch.deltas.size)

        assertThat(patch.deltas.map { delta -> delta.type.name })
            .containsExactly("EQUAL", "CHANGE", "EQUAL", "CHANGE")

        var delta: AbstractDelta<String> = patch.deltas[0]
        assertTrue(delta is EqualDelta<*>)
        assertEquals(Chunk(0, listOf("The")), delta.source)
        assertEquals(Chunk(0, listOf("The")), delta.target)

        delta = patch.deltas[1]
        assertTrue(delta is ChangeDelta<*>)
        assertEquals(Chunk(1, listOf("dog")), delta.source)
        assertEquals(Chunk(1, listOf("fox")), delta.target)

        delta = patch.deltas[2]
        assertTrue(delta is EqualDelta<*>)
        assertEquals(Chunk(2, listOf("is")), delta.source)
        assertEquals(Chunk(2, listOf("is")), delta.target)

        delta = patch.deltas[3]
        assertTrue(delta is ChangeDelta<*>)
        assertEquals(Chunk(3, listOf("brown")), delta.source)
        assertEquals(Chunk(3, listOf("down")), delta.target)
    }

    @Test
    @Throws(IOException::class)
    fun `diff patch issue 189`() {
        val originalBytes = Files.readAllBytes(
            TestConstants.path("com/bernaferrari/difflib/text/issue_189_insert_original.txt")
        )
        val revisedBytes = Files.readAllBytes(
            TestConstants.path("com/bernaferrari/difflib/text/issue_189_insert_revised.txt")
        )
        val original = String(originalBytes, StandardCharsets.UTF_8)
        val revised = String(revisedBytes, StandardCharsets.UTF_8)

        val patch = DiffUtils.diff(original.split("\n"), revised.split("\n"))
        assertEquals(1, patch.deltas.size)
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun readStringListFromInputStream(input: InputStream): List<String> {
            return BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
                reader.readLines()
            }
        }
    }
}
