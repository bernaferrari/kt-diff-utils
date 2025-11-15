package com.bernaferrari.difflib.text

import com.bernaferrari.difflib.DiffUtils
import com.bernaferrari.difflib.algorithm.Equalizer
import com.bernaferrari.difflib.patch.AbstractDelta
import com.bernaferrari.difflib.patch.ChangeDelta
import com.bernaferrari.difflib.patch.Chunk
import com.bernaferrari.difflib.patch.DeleteDelta
import com.bernaferrari.difflib.patch.DeltaType
import com.bernaferrari.difflib.patch.InsertDelta
import com.bernaferrari.difflib.patch.Patch
import com.bernaferrari.difflib.text.DiffRow.Tag
import com.bernaferrari.difflib.text.deltamerge.DeltaMergeUtils
import com.bernaferrari.difflib.text.deltamerge.InlineDeltaMergeInfo

private typealias TagRenderer = (Tag, Boolean) -> String
private typealias LineProcessor = (String) -> String
private typealias InlineSplitter = (String) -> List<String>
private typealias InlineDeltaMergerFn = (InlineDeltaMergeInfo) -> List<AbstractDelta<String>>

/**
 * Generates [DiffRow]s for displaying side-by-side diffs.
 */
class DiffRowGenerator private constructor(builder: Builder) {

    private val columnWidth: Int = builder.columnWidth
    private val equalizer: Equalizer<String> =
        builder.equalizer ?: if (builder.ignoreWhiteSpaces) IGNORE_WHITESPACE_EQUALIZER else DEFAULT_EQUALIZER
    private val ignoreWhiteSpaces: Boolean = builder.ignoreWhiteSpaces
    private val inlineDiffSplitter: InlineSplitter = builder.inlineDiffSplitter
    private val mergeOriginalRevised: Boolean = builder.mergeOriginalRevised
    private val newTag: TagRenderer = builder.newTag
    private val oldTag: TagRenderer = builder.oldTag
    private val reportLinesUnchanged: Boolean = builder.reportLinesUnchanged
    private val lineNormalizer: LineProcessor = builder.lineNormalizer
    private val processDiffs: LineProcessor? = builder.processDiffs
    private val inlineDeltaMerger: InlineDeltaMergerFn = builder.inlineDeltaMerger
    private val showInlineDiffs: Boolean = builder.showInlineDiffs
    private val replaceOriginalLinefeedInChangesWithSpaces: Boolean =
        builder.replaceOriginalLinefeedInChangesWithSpaces
    private val decompressDeltas: Boolean = builder.decompressDeltas

    fun generateDiffRows(original: List<String>, revised: List<String>): List<DiffRow> =
        generateDiffRows(original, DiffUtils.diff(original, revised, equalizer))

    fun generateDiffRows(original: List<String>, patch: Patch<String>): List<DiffRow> {
        val diffRows = mutableListOf<DiffRow>()
        var endPos = 0
        val deltaList = patch.deltas

        if (decompressDeltas) {
            for (originalDelta in deltaList) {
                for (delta in decompressDeltas(originalDelta)) {
                    endPos = transformDeltaIntoDiffRow(original, endPos, diffRows, delta)
                }
            }
        } else {
            for (delta in deltaList) {
                endPos = transformDeltaIntoDiffRow(original, endPos, diffRows, delta)
            }
        }

        for (line in original.subList(endPos, original.size)) {
            diffRows.add(buildDiffRow(Tag.EQUAL, line, line))
        }
        return diffRows
    }

    private fun transformDeltaIntoDiffRow(
        original: List<String>,
        endPos: Int,
        diffRows: MutableList<DiffRow>,
        delta: AbstractDelta<String>
    ): Int {
        val orig = delta.source
        val rev = delta.target

        for (line in original.subList(endPos, orig.position)) {
            diffRows.add(buildDiffRow(Tag.EQUAL, line, line))
        }

        when (delta.type) {
            DeltaType.INSERT -> rev.lines.forEach { diffRows.add(buildDiffRow(Tag.INSERT, "", it)) }
            DeltaType.DELETE -> orig.lines.forEach { diffRows.add(buildDiffRow(Tag.DELETE, it, "")) }
            else -> if (showInlineDiffs) {
                diffRows.addAll(generateInlineDiffs(delta))
            } else {
                val max = maxOf(orig.size(), rev.size())
                for (j in 0 until max) {
                    diffRows.add(
                        buildDiffRow(
                            Tag.CHANGE,
                            if (orig.lines.size > j) orig.lines[j] else "",
                            if (rev.lines.size > j) rev.lines[j] else ""
                        )
                    )
                }
            }
        }

        return orig.last() + 1
    }

    private fun decompressDeltas(delta: AbstractDelta<String>): List<AbstractDelta<String>> {
        if (delta.type == DeltaType.CHANGE && delta.source.size() != delta.target.size()) {
            val deltas = mutableListOf<AbstractDelta<String>>()
            val minSize = minOf(delta.source.size(), delta.target.size())
            val orig = delta.source
            val rev = delta.target

            deltas.add(
                ChangeDelta(
                    Chunk(orig.position, orig.lines.subList(0, minSize)),
                    Chunk(rev.position, rev.lines.subList(0, minSize))
                )
            )

            if (orig.lines.size < rev.lines.size) {
                deltas.add(
                    InsertDelta(
                        Chunk(orig.position + minSize, emptyList()),
                        Chunk(rev.position + minSize, rev.lines.subList(minSize, rev.lines.size))
                    )
                )
            } else {
                deltas.add(
                    DeleteDelta(
                        Chunk(orig.position + minSize, orig.lines.subList(minSize, orig.lines.size)),
                        Chunk(rev.position + minSize, emptyList())
                    )
                )
            }
            return deltas
        }
        return listOf(delta)
    }

    private fun buildDiffRow(type: Tag, orgLine: String, newLine: String): DiffRow {
        if (reportLinesUnchanged) {
            return DiffRow(type, orgLine, newLine)
        }
        var wrapOrg = preprocessLine(orgLine)
        var wrapNew = preprocessLine(newLine)

        if (type == Tag.DELETE && (mergeOriginalRevised || showInlineDiffs)) {
            wrapOrg = oldTag(type, true) + wrapOrg + oldTag(type, false)
        }
        if (type == Tag.INSERT) {
            if (mergeOriginalRevised) {
                wrapOrg = newTag(type, true) + wrapNew + newTag(type, false)
            } else if (showInlineDiffs) {
                wrapNew = newTag(type, true) + wrapNew + newTag(type, false)
            }
        }
        return DiffRow(type, wrapOrg, wrapNew)
    }

    private fun buildDiffRowWithoutNormalizing(type: Tag, orgLine: String, newLine: String): DiffRow =
        DiffRow(type, StringUtils.wrapText(orgLine, columnWidth), StringUtils.wrapText(newLine, columnWidth))

    internal fun normalizeLines(list: List<String>): List<String> =
        if (reportLinesUnchanged) list else list.map { lineNormalizer(it) }

    private fun generateInlineDiffs(delta: AbstractDelta<String>): List<DiffRow> {
        val orig = normalizeLines(delta.source.lines)
        val rev = normalizeLines(delta.target.lines)
        val joinedOrig = orig.joinToString("\n")
        val joinedRev = rev.joinToString("\n")

        val origList = inlineDiffSplitter(joinedOrig).toMutableList()
        val revList = inlineDiffSplitter(joinedRev).toMutableList()

        val originalInlineDeltas = DiffUtils.diff(origList, revList, equalizer).deltas
        val inlineDeltas = inlineDeltaMerger(InlineDeltaMergeInfo(originalInlineDeltas, origList, revList)).toMutableList()

        for (inlineDelta in inlineDeltas.asReversed()) {
            val inlineOrig = inlineDelta.source
            val inlineRev = inlineDelta.target
            when (inlineDelta.type) {
                DeltaType.DELETE -> wrapInTag(
                    origList,
                    inlineOrig.position,
                    inlineOrig.position + inlineOrig.size(),
                    Tag.DELETE,
                    oldTag,
                    processDiffs,
                    replaceOriginalLinefeedInChangesWithSpaces && mergeOriginalRevised
                )

                DeltaType.INSERT -> if (mergeOriginalRevised) {
                    origList.addAll(
                        inlineOrig.position,
                        revList.subList(inlineRev.position, inlineRev.position + inlineRev.size())
                    )
                    wrapInTag(
                        origList,
                        inlineOrig.position,
                        inlineOrig.position + inlineRev.size(),
                        Tag.INSERT,
                        newTag,
                        processDiffs,
                        false
                    )
                } else {
                    wrapInTag(
                        revList,
                        inlineRev.position,
                        inlineRev.position + inlineRev.size(),
                        Tag.INSERT,
                        newTag,
                        processDiffs,
                        false
                    )
                }

                DeltaType.CHANGE -> {
                    if (mergeOriginalRevised) {
                        origList.addAll(
                            inlineOrig.position + inlineOrig.size(),
                            revList.subList(inlineRev.position, inlineRev.position + inlineRev.size())
                        )
                        wrapInTag(
                            origList,
                            inlineOrig.position + inlineOrig.size(),
                            inlineOrig.position + inlineOrig.size() + inlineRev.size(),
                            Tag.CHANGE,
                            newTag,
                            processDiffs,
                            false
                        )
                    } else {
                        wrapInTag(
                            revList,
                            inlineRev.position,
                            inlineRev.position + inlineRev.size(),
                            Tag.CHANGE,
                            newTag,
                            processDiffs,
                            false
                        )
                    }
                    wrapInTag(
                        origList,
                        inlineOrig.position,
                        inlineOrig.position + inlineOrig.size(),
                        Tag.CHANGE,
                        oldTag,
                        processDiffs,
                        replaceOriginalLinefeedInChangesWithSpaces && mergeOriginalRevised
                    )
                }

                else -> {}
            }
        }

        val origResult = StringBuilder()
        for (character in origList) {
            origResult.append(character)
        }
        val revResult = StringBuilder()
        for (character in revList) {
            revResult.append(character)
        }

        val originalLines = origResult.toString().split("\n")
        val revisedLines = revResult.toString().split("\n")
        val diffRows = mutableListOf<DiffRow>()

        val max = maxOf(originalLines.size, revisedLines.size)
        for (j in 0 until max) {
            diffRows.add(
                buildDiffRowWithoutNormalizing(
                    Tag.CHANGE,
                    if (originalLines.size > j) originalLines[j] else "",
                    if (revisedLines.size > j) revisedLines[j] else ""
                )
            )
        }
        return diffRows
    }

    private fun preprocessLine(line: String): String =
        if (columnWidth == 0) {
            lineNormalizer(line)
        } else {
            StringUtils.wrapText(lineNormalizer(line), columnWidth)
        }

    class Builder private constructor() {
        var showInlineDiffs: Boolean = false
            private set
        var ignoreWhiteSpaces: Boolean = false
            private set
        var decompressDeltas: Boolean = true
            private set
        var oldTag: TagRenderer = { _, flag -> if (flag) "<span class=\"editOldInline\">" else "</span>" }
            private set
        var newTag: TagRenderer = { _, flag -> if (flag) "<span class=\"editNewInline\">" else "</span>" }
            private set
        var columnWidth: Int = 80
            private set
        var mergeOriginalRevised: Boolean = false
            private set
        var inlineDiffSplitter: InlineSplitter = SPLITTER_BY_CHARACTER
            private set
        var equalizer: Equalizer<String>? = null
            private set
        var processDiffs: LineProcessor? = null
            private set
        var reportLinesUnchanged: Boolean = false
            private set
        var lineNormalizer: LineProcessor = LINE_NORMALIZER_FOR_HTML
            private set
        var replaceOriginalLinefeedInChangesWithSpaces: Boolean = false
            private set
        var inlineDeltaMerger: InlineDeltaMergerFn =
            DEFAULT_INLINE_DELTA_MERGER
            private set

        fun showInlineDiffs(value: Boolean) = apply { showInlineDiffs = value }

        fun ignoreWhiteSpaces(value: Boolean) = apply { ignoreWhiteSpaces = value }

        fun reportLinesUnchanged(value: Boolean) = apply { reportLinesUnchanged = value }

        fun oldTag(generator: TagRenderer) = apply { oldTag = generator }

        fun oldTag(generator: (Boolean) -> String) =
            apply { oldTag = { _, flag -> generator(flag) } }

        fun newTag(generator: TagRenderer) = apply { newTag = generator }

        fun newTag(generator: (Boolean) -> String) =
            apply { newTag = { _, flag -> generator(flag) } }

        fun processDiffs(processor: LineProcessor) = apply { processDiffs = processor }

        fun columnWidth(width: Int) = apply {
            if (width >= 0) {
                columnWidth = width
            }
        }

        fun build(): DiffRowGenerator = DiffRowGenerator(this)

        fun mergeOriginalRevised(value: Boolean) = apply { mergeOriginalRevised = value }

        fun decompressDeltas(value: Boolean) = apply { decompressDeltas = value }

        fun inlineDiffByWord(inlineDiffByWord: Boolean) = apply {
            inlineDiffSplitter = if (inlineDiffByWord) SPLITTER_BY_WORD else SPLITTER_BY_CHARACTER
        }

        fun inlineDiffBySplitter(splitter: InlineSplitter) = apply { inlineDiffSplitter = splitter }

        fun lineNormalizer(normalizer: LineProcessor) = apply { lineNormalizer = normalizer }

        fun equalizer(equalizer: Equalizer<String>) = apply { this.equalizer = equalizer }

        fun replaceOriginalLinefeedInChangesWithSpaces(replace: Boolean) =
            apply { replaceOriginalLinefeedInChangesWithSpaces = replace }

        fun inlineDeltaMerger(
            merger: InlineDeltaMergerFn
        ) = apply { inlineDeltaMerger = merger }

        companion object {
            @JvmStatic
            fun create(): Builder = Builder()
        }
    }

    companion object {
        @JvmStatic
        fun create(): Builder = Builder.create()
        val DEFAULT_EQUALIZER: Equalizer<String> = { original, revised -> original == revised }

        val IGNORE_WHITESPACE_EQUALIZER: Equalizer<String> =
            { original, revised -> adjustWhitespace(original) == adjustWhitespace(revised) }

        val LINE_NORMALIZER_FOR_HTML: LineProcessor = { StringUtils.normalize(it) }

        val SPLITTER_BY_CHARACTER: InlineSplitter = { line ->
            line.map { it.toString() }
        }

        internal val SPLIT_BY_WORD_REGEX: Regex =
            Regex("\\s+|[,.\\[\\](){}/\\\\*+\\-#<>;:&\\']+")

        val SPLITTER_BY_WORD: InlineSplitter =
            { line -> splitStringPreserveDelimiter(line, SPLIT_BY_WORD_REGEX) }

        private val WHITESPACE_REGEX: Regex = Regex("\\s+")

        val DEFAULT_INLINE_DELTA_MERGER: InlineDeltaMergerFn = { info -> info.deltas }

        val WHITESPACE_EQUALITIES_MERGER: InlineDeltaMergerFn =
            { info ->
                DeltaMergeUtils.mergeInlineDeltas(info) { equalities ->
                    equalities.all { it.isBlank() }
                }
            }

        private fun adjustWhitespace(raw: String): String =
            WHITESPACE_REGEX.replace(raw.trim(), " ")

        internal fun splitStringPreserveDelimiter(str: String?, splitPattern: Regex): List<String> {
            if (str == null) return emptyList()
            val list = mutableListOf<String>()
            var pos = 0
            for (match in splitPattern.findAll(str)) {
                val start = match.range.first
                if (pos < start) {
                    list.add(str.substring(pos, start))
                }
                list.add(match.value)
                pos = match.range.last + 1
            }
            if (pos < str.length) {
                list.add(str.substring(pos))
            }
            return list
        }

        internal fun wrapInTag(
            sequence: MutableList<String>,
            startPosition: Int,
            endPosition: Int,
            tag: Tag,
            tagGenerator: TagRenderer,
            processDiffs: LineProcessor?,
            replaceLinefeedWithSpace: Boolean
        ) {
            var endPos = endPosition
            while (endPos >= startPosition) {
                while (endPos > startPosition) {
                    val value = sequence[endPos - 1]
                    if (value != "\n") {
                        break
                    } else if (replaceLinefeedWithSpace) {
                        sequence[endPos - 1] = " "
                        break
                    }
                    endPos--
                }
                if (endPos == startPosition) {
                    break
                }
                sequence.add(endPos, tagGenerator(tag, false))
                processDiffs?.let { processor ->
                    sequence[endPos - 1] = processor(sequence[endPos - 1])
                }
                endPos--

                while (endPos > startPosition) {
                    if (sequence[endPos - 1] == "\n") {
                        if (replaceLinefeedWithSpace) {
                            sequence[endPos - 1] = " "
                        } else {
                            break
                        }
                    }
                    processDiffs?.let { processor ->
                        sequence[endPos - 1] = processor(sequence[endPos - 1])
                    }
                    endPos--
                }

                sequence.add(endPos, tagGenerator(tag, true))
                endPos--
            }
        }
    }
}
