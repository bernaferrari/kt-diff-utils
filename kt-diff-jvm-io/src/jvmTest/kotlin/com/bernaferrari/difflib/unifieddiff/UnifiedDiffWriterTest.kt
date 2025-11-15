package com.bernaferrari.difflib.unifieddiff

import com.bernaferrari.difflib.DiffUtils
import com.bernaferrari.difflib.patch.Patch
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.StringWriter
import java.net.URI
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UnifiedDiffWriterTest {

    @Test
    @Throws(IOException::class)
    fun writeDiff() {
        val str = readFile(
            javaClass.getResource("jsqlparser_patch_1.diff")!!.toURI(),
            Charset.defaultCharset()
        )
        val diff = UnifiedDiffReader.parseUnifiedDiff(ByteArrayInputStream(str.toByteArray()))

        val writer = StringWriter()
        UnifiedDiffWriter.write(diff, { emptyList() }, writer, 5)
        println(writer.toString())
    }

    /**
     * Issue 47
     */
    @Test
    @Throws(IOException::class)
    fun writeWithNewFile() {
        val original = emptyList<String>()
        val revised = listOf("line1", "line2")

        val patch = DiffUtils.diff(original, revised)
        val diff = UnifiedDiff()
        diff.addFile(UnifiedDiffFile.from(null, "revised", patch))

        val writer = StringWriter()
        UnifiedDiffWriter.write(diff, { original }, writer, 5)
        val lines = writer.toString().split("\n")

        assertEquals("--- /dev/null", lines[0])
        assertEquals("+++ revised", lines[1])
        assertEquals("@@ -0,0 +1,2 @@", lines[2])
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class)
        fun readFile(path: URI, encoding: Charset): String {
            val encoded = Files.readAllBytes(Paths.get(path))
            return String(encoded, encoding)
        }
    }
}
