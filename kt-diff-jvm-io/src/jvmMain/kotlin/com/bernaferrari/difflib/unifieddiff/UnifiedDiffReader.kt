package com.bernaferrari.difflib.unifieddiff

import com.bernaferrari.difflib.patch.ChangeDelta
import com.bernaferrari.difflib.patch.Chunk
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.util.ArrayList
import java.util.Objects
import java.util.function.BiConsumer
import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.MatchResult
import java.util.regex.Matcher
import java.util.regex.Pattern

class UnifiedDiffReader private constructor(reader: Reader) {

    private val READABLE = InternalUnifiedDiffReader(reader)
    private val data = UnifiedDiff()

    private val diffCommand = UnifiedDiffLine(true, "^diff\\s", this::processDiff)
    private val similarityIndex = UnifiedDiffLine(true, "^similarity index (\\d+)%$", this::processSimilarityIndex)
    private val indexLine =
        UnifiedDiffLine(true, "^index\\s[\\da-zA-Z]+\\.\\.[\\da-zA-Z]+(\\s(\\d+))?$", this::processIndex)
    private val fromFile = UnifiedDiffLine(true, "^---\\s", this::processFromFile)
    private val toFile = UnifiedDiffLine(true, "^\\+\\+\\+\\s", this::processToFile)
    private val renameFrom = UnifiedDiffLine(true, "^rename\\sfrom\\s(.+)$", this::processRenameFrom)
    private val renameTo = UnifiedDiffLine(true, "^rename\\sto\\s(.+)$", this::processRenameTo)
    private val copyFrom = UnifiedDiffLine(true, "^copy\\sfrom\\s(.+)$", this::processCopyFrom)
    private val copyTo = UnifiedDiffLine(true, "^copy\\sto\\s(.+)$", this::processCopyTo)
    private val newFileMode = UnifiedDiffLine(true, "^new\\sfile\\smode\\s(\\d+)", this::processNewFileMode)
    private val deletedFileMode = UnifiedDiffLine(true, "^deleted\\sfile\\smode\\s(\\d+)", this::processDeletedFileMode)
    private val oldMode = UnifiedDiffLine(true, "^old\\smode\\s(\\d+)", this::processOldMode)
    private val newMode = UnifiedDiffLine(true, "^new\\smode\\s(\\d+)", this::processNewMode)
    private val binaryAdded = UnifiedDiffLine(
        true,
        "^Binary\\sfiles\\s/dev/null\\sand\\sb/(.+)\\sdiffer",
        this::processBinaryAdded
    )
    private val binaryDeleted = UnifiedDiffLine(
        true,
        "^Binary\\sfiles\\sa/(.+)\\sand\\s/dev/null\\sdiffer",
        this::processBinaryDeleted
    )
    private val binaryEdited =
        UnifiedDiffLine(true, "^Binary\\sfiles\\sa/(.+)\\sand\\sb/(.+)\\sdiffer", this::processBinaryEdited)
    private val chunk = UnifiedDiffLine(false, UNIFIED_DIFF_CHUNK_REGEXP, this::processChunk)
    private val lineNormal = UnifiedDiffLine("^\\s", this::processNormalLine)
    private val lineDel = UnifiedDiffLine("^-", this::processDelLine)
    private val lineAdd = UnifiedDiffLine("^\\+", this::processAddLine)

    private var actualFile: UnifiedDiffFile? = null
    private var actualFileCreatedFromDiff = false
    private val originalTxt = ArrayList<String>()
    private val revisedTxt = ArrayList<String>()
    private val addLineIdxList = ArrayList<Int>()
    private val delLineIdxList = ArrayList<Int>()
    private var oldLn = 0
    private var oldSize = 0
    private var newLn = 0
    private var newSize = 0
    private var delLineIdx = 0
    private var addLineIdx = 0

    @Throws(IOException::class, UnifiedDiffParserException::class)
    private fun parse(): UnifiedDiff {
        var line = READABLE.readLine()
        while (line != null) {
            var headerTxt = ""
            LOG.log(Level.FINE, "header parsing")
            while (line != null) {
                LOG.log(Level.FINE, "parsing line {0}", line)
                if (
                    validLine(
                        line,
                        diffCommand,
                        similarityIndex,
                        indexLine,
                        fromFile,
                        toFile,
                        renameFrom,
                        renameTo,
                        copyFrom,
                        copyTo,
                        newFileMode,
                        deletedFileMode,
                        oldMode,
                        newMode,
                        binaryAdded,
                        binaryDeleted,
                        binaryEdited,
                        chunk
                    )
                ) {
                    break
                } else {
                    headerTxt += line + "\n"
                }
                line = READABLE.readLine()
            }
            if (headerTxt.isNotEmpty()) {
                data.header = headerTxt
            }
            if (line != null && !chunk.validLine(line)) {
                while (line != null && !chunk.validLine(line)) {
                    if (
                        !processLine(
                            line,
                            diffCommand,
                            similarityIndex,
                            indexLine,
                            fromFile,
                            toFile,
                            renameFrom,
                            renameTo,
                            copyFrom,
                            copyTo,
                            newFileMode,
                            deletedFileMode,
                            oldMode,
                            newMode,
                            binaryAdded,
                            binaryDeleted,
                            binaryEdited
                        )
                    ) {
                        throw UnifiedDiffParserException("expected file start line not found")
                    }
                    line = READABLE.readLine()
                }
            }
            if (line != null) {
                processLine(line, chunk)
                while (true) {
                    line = READABLE.readLine() ?: break
                    line = checkForNoNewLineAtTheEndOfTheFile(line)
                    if (!processLine(line, lineNormal, lineAdd, lineDel)) {
                        throw UnifiedDiffParserException("expected data line not found")
                    }
                    if (
                        (originalTxt.size == oldSize && revisedTxt.size == newSize) ||
                        (oldSize == 0 && newSize == 0 && originalTxt.size == oldLn && revisedTxt.size == newLn)
                    ) {
                        finalizeChunk()
                        break
                    }
                }
                line = READABLE.readLine()
                line = checkForNoNewLineAtTheEndOfTheFile(line)
            }
            if (line == null || (line.startsWith("--") && !line.startsWith("---"))) {
                break
            }
        }

        if (READABLE.ready()) {
            var tailTxt = ""
            while (READABLE.ready()) {
                if (tailTxt.isNotEmpty()) {
                    tailTxt += "\n"
                }
                tailTxt += READABLE.readLine()
            }
            data.tail = tailTxt
        }

        return data
    }

    @Throws(IOException::class)
    private fun checkForNoNewLineAtTheEndOfTheFile(line: String?): String? {
        if ("\\ No newline at end of file" == line) {
            actualFile?.noNewLineAtTheEndOfTheFile = true
            return READABLE.readLine()
        }
        return line
    }

    private fun processLine(line: String?, vararg rules: UnifiedDiffLine): Boolean {
        if (line == null) {
            return false
        }
        for (rule in rules) {
            if (rule.processLine(line)) {
                LOG.fine("  >>> processed rule $rule")
                return true
            }
        }
        LOG.warning("  >>> no rule matched $line")
        return false
    }

    private fun validLine(line: String?, vararg rules: UnifiedDiffLine): Boolean {
        if (line == null) {
            return false
        }
        for (rule in rules) {
            if (rule.validLine(line)) {
                LOG.fine("  >>> accepted rule $rule")
                return true
            }
        }
        return false
    }

    private fun processDiff(@Suppress("UNUSED_PARAMETER") match: MatchResult, line: String) {
        LOG.log(Level.FINE, "start {0}", line)
        val fromTo = parseFileNames(READABLE.lastLine() ?: line)
        val file = createNewFile()
        file.fromFile = fromTo[0]
        file.toFile = fromTo[1]
        file.diffCommand = line
        actualFileCreatedFromDiff = true
    }

    private fun processSimilarityIndex(match: MatchResult, @Suppress("UNUSED_PARAMETER") line: String) {
        actualFile?.similarityIndex = match.group(1)?.toInt()
    }

    private fun finalizeChunk() {
        if (originalTxt.isNotEmpty() || revisedTxt.isNotEmpty()) {
            val file = actualFile ?: throw IllegalStateException("no active file")
            file.patch.addDelta(
                ChangeDelta(
                    Chunk(oldLn - 1, ArrayList(originalTxt), ArrayList(delLineIdxList)),
                    Chunk(newLn - 1, ArrayList(revisedTxt), ArrayList(addLineIdxList))
                )
            )
            oldLn = 0
            newLn = 0
            originalTxt.clear()
            revisedTxt.clear()
            addLineIdxList.clear()
            delLineIdxList.clear()
            delLineIdx = 0
            addLineIdx = 0
        }
    }

    private fun processNormalLine(@Suppress("UNUSED_PARAMETER") match: MatchResult, line: String) {
        val content = line.substring(1)
        originalTxt.add(content)
        revisedTxt.add(content)
        delLineIdx++
        addLineIdx++
    }

    private fun processAddLine(@Suppress("UNUSED_PARAMETER") match: MatchResult, line: String) {
        val content = line.substring(1)
        revisedTxt.add(content)
        addLineIdx++
        addLineIdxList.add(newLn - 1 + addLineIdx)
    }

    private fun processDelLine(@Suppress("UNUSED_PARAMETER") match: MatchResult, line: String) {
        val content = line.substring(1)
        originalTxt.add(content)
        delLineIdx++
        delLineIdxList.add(oldLn - 1 + delLineIdx)
    }

    private fun processChunk(match: MatchResult, @Suppress("UNUSED_PARAMETER") chunkStart: String) {
        if (actualFile == null) {
            createNewFile()
        }
        oldLn = toInteger(match, 1, 1)
        oldSize = toInteger(match, 2, 1)
        newLn = toInteger(match, 3, 1)
        newSize = toInteger(match, 4, 1)
        if (oldLn == 0) {
            oldLn = 1
        }
        if (newLn == 0) {
            newLn = 1
        }
    }

    private fun processIndex(@Suppress("UNUSED_PARAMETER") match: MatchResult, line: String) {
        actualFile?.index = line.substring(6)
    }

    private fun processFromFile(@Suppress("UNUSED_PARAMETER") match: MatchResult, line: String) {
        val file = if (actualFileCreatedFromDiff && actualFile != null) {
            actualFile!!
        } else {
            createNewFile()
        }
        actualFileCreatedFromDiff = false
        file.fromFile = extractFileName(line)
        file.fromTimestamp = extractTimestamp(line)
    }

    private fun processToFile(@Suppress("UNUSED_PARAMETER") match: MatchResult, line: String) {
        val file = actualFile ?: createNewFile()
        file.toFile = extractFileName(line)
        file.toTimestamp = extractTimestamp(line)
    }

    private fun processRenameFrom(match: MatchResult, @Suppress("UNUSED_PARAMETER") line: String) {
        actualFile?.renameFrom = match.group(1)
    }

    private fun processRenameTo(match: MatchResult, @Suppress("UNUSED_PARAMETER") line: String) {
        actualFile?.renameTo = match.group(1)
    }

    private fun processCopyFrom(match: MatchResult, @Suppress("UNUSED_PARAMETER") line: String) {
        actualFile?.copyFrom = match.group(1)
    }

    private fun processCopyTo(match: MatchResult, @Suppress("UNUSED_PARAMETER") line: String) {
        actualFile?.copyTo = match.group(1)
    }

    private fun processNewFileMode(match: MatchResult, @Suppress("UNUSED_PARAMETER") line: String) {
        actualFile?.newFileMode = match.group(1)
    }

    private fun processDeletedFileMode(match: MatchResult, @Suppress("UNUSED_PARAMETER") line: String) {
        actualFile?.deletedFileMode = match.group(1)
    }

    private fun processOldMode(match: MatchResult, @Suppress("UNUSED_PARAMETER") line: String) {
        actualFile?.oldMode = match.group(1)
    }

    private fun processNewMode(match: MatchResult, @Suppress("UNUSED_PARAMETER") line: String) {
        actualFile?.newMode = match.group(1)
    }

    private fun processBinaryAdded(match: MatchResult, @Suppress("UNUSED_PARAMETER") line: String) {
        actualFile?.binaryAdded = match.group(1)
    }

    private fun processBinaryDeleted(match: MatchResult, @Suppress("UNUSED_PARAMETER") line: String) {
        actualFile?.binaryDeleted = match.group(1)
    }

    private fun processBinaryEdited(match: MatchResult, @Suppress("UNUSED_PARAMETER") line: String) {
        actualFile?.binaryEdited = match.group(1)
    }

    private fun createNewFile(): UnifiedDiffFile =
        UnifiedDiffFile().also {
            actualFile = it
            actualFileCreatedFromDiff = false
            data.addFile(it)
        }

    private fun extractFileName(lineContent: String): String {
        var line = lineContent
        val matcher = TIMESTAMP_REGEXP.matcher(line)
        if (matcher.find()) {
            line = line.substring(0, matcher.start())
        }
        line = line.split("\t".toRegex())[0]
        return line.substring(4).replaceFirst("^(a|b|old|new)/".toRegex(), "").trim()
    }

    private fun extractTimestamp(line: String): String? {
        val matcher = TIMESTAMP_REGEXP.matcher(line)
        return if (matcher.find()) matcher.group() else null
    }

    private fun toInteger(match: MatchResult, group: Int, defValue: Int): Int =
        Objects.toString(match.group(group), "$defValue").toInt()

    private inner class UnifiedDiffLine(
        private val stopsHeaderParsing: Boolean,
        pattern: String,
        command: BiConsumer<MatchResult, String>
    ) {
        private val pattern: Pattern = Pattern.compile(pattern)
        private val command: BiConsumer<MatchResult, String> = command

        constructor(pattern: String, command: BiConsumer<MatchResult, String>) : this(false, pattern, command)

        constructor(stopsHeaderParsing: Boolean, pattern: Pattern, command: BiConsumer<MatchResult, String>) : this(
            stopsHeaderParsing,
            pattern.pattern(),
            command
        )

        fun validLine(line: String): Boolean = pattern.matcher(line).find()

        fun processLine(line: String): Boolean {
            val matcher = pattern.matcher(line)
            return if (matcher.find()) {
                command.accept(matcher.toMatchResult(), line)
                true
            } else {
                false
            }
        }

        override fun toString(): String =
            "UnifiedDiffLine{pattern=$pattern, stopsHeaderParsing=$stopsHeaderParsing}"
    }

    companion object {
        val UNIFIED_DIFF_CHUNK_REGEXP: Pattern =
            Pattern.compile("^@@\\s+-(?:(\\d+)(?:,(\\d+))?)\\s+\\+(?:(\\d+)(?:,(\\d+))?)\\s+@@")
        val TIMESTAMP_REGEXP: Pattern =
            Pattern.compile("(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}\\.\\d{3,})(?: [+-]\\d+)?")
        private val LOG = Logger.getLogger(UnifiedDiffReader::class.java.name)

        @JvmStatic
        @Throws(IOException::class, UnifiedDiffParserException::class)
        fun parseUnifiedDiff(stream: InputStream): UnifiedDiff {
            val parser = UnifiedDiffReader(BufferedReader(InputStreamReader(stream)))
            return parser.parse()
        }

        fun parseFileNames(line: String): Array<String> {
            val split = line.split(" ")
            return arrayOf(split[2].replace("^a/".toRegex(), ""), split[3].replace("^b/".toRegex(), ""))
        }
    }
}

private class InternalUnifiedDiffReader(reader: Reader) : BufferedReader(reader) {
    private var lastLine: String? = null

    @Throws(IOException::class)
    override fun readLine(): String? {
        lastLine = super.readLine()
        return lastLine()
    }

    fun lastLine(): String? = lastLine
}
