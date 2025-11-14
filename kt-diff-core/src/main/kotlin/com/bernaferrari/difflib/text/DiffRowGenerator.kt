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
import java.util.ArrayList
import java.util.Collections
import java.util.Objects
import java.util.function.BiFunction
import java.util.function.Function
import java.util.regex.Pattern

/**
 * Generates [DiffRow]s for displaying side-by-side diffs.
 */
class DiffRowGenerator private constructor(builder: Builder) {

    private val columnWidth: Int = builder.columnWidth
    private val equalizer: Equalizer<String> =
        builder.equalizer ?: if (builder.ignoreWhiteSpaces) IGNORE_WHITESPACE_EQUALIZER else DEFAULT_EQUALIZER
    private val ignoreWhiteSpaces: Boolean = builder.ignoreWhiteSpaces
    private val inlineDiffSplitter: Function<String, List<String>> = builder.inlineDiffSplitter
    private val mergeOriginalRevised: Boolean = builder.mergeOriginalRevised
    private val newTag: BiFunction<Tag, Boolean, String> = builder.newTag
    private val oldTag: BiFunction<Tag, Boolean, String> = builder.oldTag
    private val reportLinesUnchanged: Boolean = builder.reportLinesUnchanged
    private val lineNormalizer: Function<String, String> = builder.lineNormalizer
    private val processDiffs: Function<String, String>? = builder.processDiffs
    private val inlineDeltaMerger: Function<InlineDeltaMergeInfo, List<AbstractDelta<String>>> =
        builder.inlineDeltaMerger
    private val showInlineDiffs: Boolean = builder.showInlineDiffs
    private val replaceOriginalLinefeedInChangesWithSpaces: Boolean =
        builder.replaceOriginalLinefeedInChangesWithSpaces
    private val decompressDeltas: Boolean = builder.decompressDeltas

    init {
        Objects.requireNonNull(inlineDiffSplitter, "inlineDiffSplitter")
        Objects.requireNonNull(lineNormalizer, "lineNormalizer")
        Objects.requireNonNull(inlineDeltaMerger, "inlineDeltaMerger")
        Objects.requireNonNull(newTag, "newTag")
        Objects.requireNonNull(oldTag, "oldTag")
    }

    fun generateDiffRows(original: List<String>, revised: List<String>): List<DiffRow> =
        generateDiffRows(original, DiffUtils.diff(original, revised, equalizer))

    fun generateDiffRows(original: List<String>, patch: Patch<String>): List<DiffRow> {
        val diffRows = ArrayList<DiffRow>()
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
            val deltas = ArrayList<AbstractDelta<String>>()
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
            wrapOrg = oldTag.apply(type, true) + wrapOrg + oldTag.apply(type, false)
        }
        if (type == Tag.INSERT) {
            if (mergeOriginalRevised) {
                wrapOrg = newTag.apply(type, true) + wrapNew + newTag.apply(type, false)
            } else if (showInlineDiffs) {
                wrapNew = newTag.apply(type, true) + wrapNew + newTag.apply(type, false)
            }
        }
        return DiffRow(type, wrapOrg, wrapNew)
    }

    private fun buildDiffRowWithoutNormalizing(type: Tag, orgLine: String, newLine: String): DiffRow =
        DiffRow(type, StringUtils.wrapText(orgLine, columnWidth), StringUtils.wrapText(newLine, columnWidth))

    internal fun normalizeLines(list: List<String>): List<String> =
        if (reportLinesUnchanged) list else list.map { lineNormalizer.apply(it) }

    private fun generateInlineDiffs(delta: AbstractDelta<String>): List<DiffRow> {
        val orig = normalizeLines(delta.source.lines)
        val rev = normalizeLines(delta.target.lines)
        val joinedOrig = orig.joinToString("\n")
        val joinedRev = rev.joinToString("\n")

        val origList = ArrayList(inlineDiffSplitter.apply(joinedOrig))
        val revList = ArrayList(inlineDiffSplitter.apply(joinedRev))

        val originalInlineDeltas = DiffUtils.diff(origList, revList, equalizer).deltas
        val inlineDeltas = inlineDeltaMerger.apply(InlineDeltaMergeInfo(originalInlineDeltas, origList, revList)).toMutableList()

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
        val diffRows = ArrayList<DiffRow>()

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
            lineNormalizer.apply(line)
        } else {
            StringUtils.wrapText(lineNormalizer.apply(line), columnWidth)
        }

    class Builder private constructor() {
        var showInlineDiffs: Boolean = false
            private set
        var ignoreWhiteSpaces: Boolean = false
            private set
        var decompressDeltas: Boolean = true
            private set
        var oldTag: BiFunction<Tag, Boolean, String> =
            BiFunction { _, flag -> if (flag) "<span class=\"editOldInline\">" else "</span>" }
            private set
        var newTag: BiFunction<Tag, Boolean, String> =
            BiFunction { _, flag -> if (flag) "<span class=\"editNewInline\">" else "</span>" }
            private set
        var columnWidth: Int = 80
            private set
        var mergeOriginalRevised: Boolean = false
            private set
        var inlineDiffSplitter: Function<String, List<String>> = SPLITTER_BY_CHARACTER
            private set
        var equalizer: Equalizer<String>? = null
            private set
        var processDiffs: Function<String, String>? = null
            private set
        var reportLinesUnchanged: Boolean = false
            private set
        var lineNormalizer: Function<String, String> = LINE_NORMALIZER_FOR_HTML
            private set
        var replaceOriginalLinefeedInChangesWithSpaces: Boolean = false
            private set
        var inlineDeltaMerger: Function<InlineDeltaMergeInfo, List<AbstractDelta<String>>> =
            DEFAULT_INLINE_DELTA_MERGER
            private set

        fun showInlineDiffs(value: Boolean) = apply { showInlineDiffs = value }

        fun ignoreWhiteSpaces(value: Boolean) = apply { ignoreWhiteSpaces = value }

        fun reportLinesUnchanged(value: Boolean) = apply { reportLinesUnchanged = value }

        fun oldTag(generator: BiFunction<Tag, Boolean, String>) = apply { oldTag = generator }

        fun oldTag(generator: Function<Boolean, String>) =
            apply { oldTag = BiFunction { _, flag -> generator.apply(flag) } }

        fun newTag(generator: BiFunction<Tag, Boolean, String>) = apply { newTag = generator }

        fun newTag(generator: Function<Boolean, String>) =
            apply { newTag = BiFunction { _, flag -> generator.apply(flag) } }

        fun processDiffs(processor: Function<String, String>) = apply { processDiffs = processor }

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

        fun inlineDiffBySplitter(splitter: Function<String, List<String>>) = apply { inlineDiffSplitter = splitter }

        fun lineNormalizer(normalizer: Function<String, String>) = apply { lineNormalizer = normalizer }

        fun equalizer(equalizer: Equalizer<String>) = apply { this.equalizer = equalizer }

        fun replaceOriginalLinefeedInChangesWithSpaces(replace: Boolean) =
            apply { replaceOriginalLinefeedInChangesWithSpaces = replace }

        fun inlineDeltaMerger(
            merger: Function<InlineDeltaMergeInfo, List<AbstractDelta<String>>>
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

        val LINE_NORMALIZER_FOR_HTML: Function<String, String> = Function { StringUtils.normalize(it) }

        val SPLITTER_BY_CHARACTER: Function<String, List<String>> = Function { line ->
            val list = ArrayList<String>(line.length)
            for (character in line.toCharArray()) {
                list.add(character.toString())
            }
            list
        }

        internal val SPLIT_BY_WORD_PATTERN: Pattern =
            Pattern.compile("\\s+|[,.\\[\\](){}/\\\\*+\\-#<>;:&\\']+")

        val SPLITTER_BY_WORD: Function<String, List<String>> =
            Function { line -> splitStringPreserveDelimiter(line, SPLIT_BY_WORD_PATTERN) }

        private val WHITESPACE_PATTERN: Pattern = Pattern.compile("\\s+")

        val DEFAULT_INLINE_DELTA_MERGER: Function<InlineDeltaMergeInfo, List<AbstractDelta<String>>> =
            Function { info -> info.deltas }

        val WHITESPACE_EQUALITIES_MERGER: Function<InlineDeltaMergeInfo, List<AbstractDelta<String>>> =
            Function { info ->
                DeltaMergeUtils.mergeInlineDeltas(info) { equalities ->
                    equalities.all { it == null || it.replace("\\s+".toRegex(), "").isEmpty() }
                }
            }

        private fun adjustWhitespace(raw: String): String =
            WHITESPACE_PATTERN.matcher(raw.trim()).replaceAll(" ")

        internal fun splitStringPreserveDelimiter(str: String?, splitPattern: Pattern): List<String> {
            val list = ArrayList<String>()
            if (str != null) {
                val matcher = splitPattern.matcher(str)
                var pos = 0
                while (matcher.find()) {
                    if (pos < matcher.start()) {
                        list.add(str.substring(pos, matcher.start()))
                    }
                    list.add(matcher.group())
                    pos = matcher.end()
                }
                if (pos < str.length) {
                    list.add(str.substring(pos))
                }
            }
            return list
        }

        internal fun wrapInTag(
            sequence: MutableList<String>,
            startPosition: Int,
            endPosition: Int,
            tag: Tag,
            tagGenerator: BiFunction<Tag, Boolean, String>,
            processDiffs: Function<String, String>?,
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
                sequence.add(endPos, tagGenerator.apply(tag, false))
                if (processDiffs != null) {
                    sequence[endPos - 1] = processDiffs.apply(sequence[endPos - 1])
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
                    if (processDiffs != null) {
                        sequence[endPos - 1] = processDiffs.apply(sequence[endPos - 1])
                    }
                    endPos--
                }

                sequence.add(endPos, tagGenerator.apply(tag, true))
                endPos--
            }
        }
    }
}
