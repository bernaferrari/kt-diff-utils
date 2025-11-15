package com.bernaferrari.difflib.unifieddiff

import com.bernaferrari.difflib.patch.PatchFailedException
import java.util.ArrayList
import java.util.Collections
import java.util.function.Predicate

class UnifiedDiff {
    var header: String? = null
    var tail: String? = null
    private val files: MutableList<UnifiedDiffFile> = ArrayList()

    fun addFile(file: UnifiedDiffFile) {
        files.add(file)
    }

    fun getFiles(): List<UnifiedDiffFile> = Collections.unmodifiableList(files)

    @Throws(PatchFailedException::class)
    fun applyPatchTo(findFile: Predicate<String>, originalLines: List<String>): List<String> {
        val file = files.firstOrNull { diffFile ->
            diffFile.fromFile?.let { findFile.test(it) } == true
        }
        return file?.patch?.applyTo(originalLines) ?: originalLines
    }

    companion object {
        @JvmStatic
        fun from(header: String?, tail: String?, vararg files: UnifiedDiffFile): UnifiedDiff {
            val diff = UnifiedDiff()
            diff.header = header
            diff.tail = tail
            files.forEach { diff.addFile(it) }
            return diff
        }
    }
}
