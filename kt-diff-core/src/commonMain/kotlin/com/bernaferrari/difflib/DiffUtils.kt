package com.bernaferrari.difflib

import com.bernaferrari.difflib.algorithm.DiffAlgorithmFactory
import com.bernaferrari.difflib.algorithm.DiffAlgorithmI
import com.bernaferrari.difflib.algorithm.DiffAlgorithmListener
import com.bernaferrari.difflib.algorithm.Equalizer
import com.bernaferrari.difflib.algorithm.myers.MyersDiff
import com.bernaferrari.difflib.patch.AbstractDelta
import com.bernaferrari.difflib.patch.Patch
import com.bernaferrari.difflib.patch.PatchFailedException

/**
 * Utility entry points for computing diffs and applying patches.
 */
object DiffUtils {
    @JvmStatic
    var DEFAULT_DIFF: DiffAlgorithmFactory = MyersDiff.factory()

    @JvmStatic
    fun withDefaultDiffAlgorithmFactory(factory: DiffAlgorithmFactory) {
        DEFAULT_DIFF = factory
    }

    @JvmStatic
    fun <T> diff(original: List<T>, revised: List<T>, progress: DiffAlgorithmListener?): Patch<T> =
        diff(original, revised, DEFAULT_DIFF.create(), progress)

    @JvmStatic
    fun <T> diff(original: List<T>, revised: List<T>): Patch<T> =
        diff(original, revised, DEFAULT_DIFF.create(), null)

    @JvmStatic
    fun <T> diff(original: List<T>, revised: List<T>, includeEqualParts: Boolean): Patch<T> =
        diff(original, revised, DEFAULT_DIFF.create(), null, includeEqualParts)

    @JvmStatic
    fun diff(sourceText: String, targetText: String, progress: DiffAlgorithmListener?): Patch<String> =
        diff(sourceText.split("\n"), targetText.split("\n"), progress)

    @JvmStatic
    fun <T> diff(
        source: List<T>,
        target: List<T>,
        equalizer: Equalizer<T>?
    ): Patch<T> =
        if (equalizer != null) {
            diff(source, target, DEFAULT_DIFF.create(equalizer))
        } else {
            diff(source, target, MyersDiff())
        }

    @JvmStatic
    fun <T> diff(
        original: List<T>,
        revised: List<T>,
        algorithm: DiffAlgorithmI<T>,
        progress: DiffAlgorithmListener?
    ): Patch<T> = diff(original, revised, algorithm, progress, includeEqualParts = false)

    @JvmStatic
    fun <T> diff(
        original: List<T>,
        revised: List<T>,
        algorithm: DiffAlgorithmI<T>,
        progress: DiffAlgorithmListener?,
        includeEqualParts: Boolean
    ): Patch<T> =
        Patch.generate(original, revised, algorithm.computeDiff(original, revised, progress), includeEqualParts)

    @JvmStatic
    fun <T> diff(
        original: List<T>,
        revised: List<T>,
        algorithm: DiffAlgorithmI<T>
    ): Patch<T> = diff(original, revised, algorithm, null)

    @JvmStatic
    fun diffInline(original: String, revised: String): Patch<String> {
        val origList = original.map { it.toString() }
        val revList = revised.map { it.toString() }
        val patch = diff(origList, revList)
        for (delta: AbstractDelta<String> in patch.deltas) {
            delta.source.lines = compressLines(delta.source.lines, "")
            delta.target.lines = compressLines(delta.target.lines, "")
        }
        return patch
    }

    @JvmStatic
    @Throws(PatchFailedException::class)
    fun <T> patch(original: List<T>, patch: Patch<T>): List<T> = patch.applyTo(original)

    @JvmStatic
    fun <T> unpatch(revised: List<T>, patch: Patch<T>): List<T> = patch.restore(revised)

    private fun compressLines(lines: List<String>, delimiter: String): List<String> =
        if (lines.isEmpty()) {
            emptyList()
        } else {
            listOf(lines.joinToString(delimiter))
        }
}
