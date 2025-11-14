package com.bernaferrari.difflib.algorithm.jgit

import com.bernaferrari.difflib.algorithm.DiffAlgorithmListener
import com.bernaferrari.difflib.patch.Patch
import com.bernaferrari.difflib.patch.PatchFailedException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LRHistogramDiffTest {

    @Test
    @Throws(IOException::class, PatchFailedException::class)
    fun possibleDiffHangOnLargeDataset() {
        val zip = ZipFile(resourcePath("mocks/large_dataset1.zip").toFile())
        val original = readStringListFromInputStream(zip.getInputStream(zip.getEntry("ta")))
        val revised = readStringListFromInputStream(zip.getInputStream(zip.getEntry("tb")))

        val logData = mutableListOf<String>()
        val patch = Patch.generate(
            original,
            revised,
            HistogramDiff<String>().computeDiff(original, revised, object : DiffAlgorithmListener {
                override fun diffStart() {
                    logData.add("start")
                }

                override fun diffStep(value: Int, max: Int) {
                    logData.add("$value - $max")
                }

                override fun diffEnd() {
                    logData.add("end")
                }
            })
        )

        assertEquals(34, patch.deltas.size)
        val created = patch.applyTo(original)
        assertArrayEquals(revised.toTypedArray(), created.toTypedArray())
        assertEquals(246579, logData.size)
    }

    companion object {
        private fun resourcePath(relative: String): Path {
            val baseDirs = listOf(
                Paths.get("kt-diff-jgit/build/resources/test"),
                Paths.get("build/resources/test"),
                Paths.get("target/test-classes"),
                Paths.get("kt-diff-jgit/src/test/resources")
            )
            return baseDirs
                .asSequence()
                .map { it.resolve(relative) }
                .firstOrNull { Files.exists(it) }
                ?: error("Unable to locate test resource: $relative")
        }

        @JvmStatic
        @Throws(IOException::class)
        fun readStringListFromInputStream(input: InputStream): List<String> {
            BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
                return reader.readLines()
            }
        }
    }
}
