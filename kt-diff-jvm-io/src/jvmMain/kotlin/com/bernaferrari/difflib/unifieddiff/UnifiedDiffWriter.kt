package com.bernaferrari.difflib.unifieddiff

import com.bernaferrari.difflib.patch.AbstractDelta
import java.io.IOException
import java.io.Writer
import java.util.ArrayList
import java.util.Objects
import java.util.function.Consumer
import java.util.function.Function
import java.util.logging.Level
import java.util.logging.Logger

object UnifiedDiffWriter {
    private val LOG = Logger.getLogger(UnifiedDiffWriter::class.java.name)

    @JvmStatic
    @Throws(IOException::class)
    fun write(
        diff: UnifiedDiff,
        originalLinesProvider: Function<String?, List<String>>,
        writer: Writer,
        contextSize: Int
    ) {
        Objects.requireNonNull(originalLinesProvider, "original lines provider needs to be specified")
        write(
            diff,
            originalLinesProvider,
            Consumer { line ->
                try {
                    writer.append(line).append('\n')
                } catch (ex: IOException) {
                    LOG.log(Level.SEVERE, null, ex)
                }
            },
            contextSize
        )
    }

    @JvmStatic
    @Throws(IOException::class)
    fun write(
        diff: UnifiedDiff,
        originalLinesProvider: Function<String?, List<String>>,
        writer: Consumer<String>,
        contextSize: Int
    ) {
        diff.header?.let { writer.accept(it) }

        for (file in diff.getFiles()) {
            val patchDeltas = ArrayList(file.patch.deltas)
            if (patchDeltas.isEmpty()) {
                continue
            }
            writeOrNothing(writer, file.diffCommand)
            file.index?.let { writer.accept("index $it") }
            writer.accept("--- " + (file.fromFile ?: "/dev/null"))
            file.toFile?.let { writer.accept("+++ $it") }

            val originalLines = originalLinesProvider.apply(file.fromFile)
            val deltas = ArrayList<AbstractDelta<String>>()
            var delta = patchDeltas[0]
            deltas.add(delta)

            if (patchDeltas.size > 1) {
                for (i in 1 until patchDeltas.size) {
                    val position = delta.source.position
                    val nextDelta = patchDeltas[i]
                    if (position + delta.source.size() + contextSize >= nextDelta.source.position - contextSize) {
                        deltas.add(nextDelta)
                    } else {
                        processDeltas(writer, originalLines, deltas, contextSize, false)
                        deltas.clear()
                        deltas.add(nextDelta)
                    }
                    delta = nextDelta
                }
            }

            processDeltas(
                writer,
                originalLines,
                deltas,
                contextSize,
                patchDeltas.size == 1 && file.fromFile == null
            )
        }

        diff.tail?.let {
            writer.accept("--")
            writer.accept(it)
        }
    }

    private fun processDeltas(
        writer: Consumer<String>,
        origLines: List<String>,
        deltas: List<AbstractDelta<String>>,
        contextSize: Int,
        newFile: Boolean
    ) {
        val buffer = ArrayList<String>()
        var origTotal = 0
        var revTotal = 0
        var line: Int

        var curDelta = deltas[0]
        val origStart = if (newFile) {
            0
        } else {
            var start = curDelta.source.position + 1 - contextSize
            if (start < 1) start = 1
            start
        }

        var revStart = curDelta.target.position + 1 - contextSize
        if (revStart < 1) {
            revStart = 1
        }

        var contextStart = curDelta.source.position - contextSize
        if (contextStart < 0) {
            contextStart = 0
        }

        line = contextStart
        while (line < curDelta.source.position && line < origLines.size) {
            buffer.add(" " + origLines[line])
            origTotal++
            revTotal++
            line++
        }

        getDeltaText({ buffer.add(it) }, curDelta)
        origTotal += curDelta.source.lines.size
        revTotal += curDelta.target.lines.size

        var deltaIndex = 1
        while (deltaIndex < deltas.size) {
            val nextDelta = deltas[deltaIndex]
            val intermediateStart = curDelta.source.position + curDelta.source.lines.size
            line = intermediateStart
            while (line < nextDelta.source.position && line < origLines.size) {
                buffer.add(" " + origLines[line])
                origTotal++
                revTotal++
                line++
            }
            getDeltaText({ buffer.add(it) }, nextDelta)
            origTotal += nextDelta.source.lines.size
            revTotal += nextDelta.target.lines.size
            curDelta = nextDelta
            deltaIndex++
        }

        contextStart = curDelta.source.position + curDelta.source.lines.size
        line = contextStart
        while (line < contextStart + contextSize && line < origLines.size) {
            buffer.add(" " + origLines[line])
            origTotal++
            revTotal++
            line++
        }

        writer.accept("@@ -$origStart,$origTotal +$revStart,$revTotal @@")
        buffer.forEach { writer.accept(it) }
    }

    private fun getDeltaText(writer: Consumer<String>, delta: AbstractDelta<String>) {
        for (line in delta.source.lines) {
            writer.accept("-$line")
        }
        for (line in delta.target.lines) {
            writer.accept("+$line")
        }
    }

    private fun writeOrNothing(writer: Consumer<String>, value: String?) {
        if (value != null) {
            writer.accept(value)
        }
    }
}
