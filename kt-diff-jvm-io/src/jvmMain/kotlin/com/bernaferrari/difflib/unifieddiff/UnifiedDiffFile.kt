package com.bernaferrari.difflib.unifieddiff

import com.bernaferrari.difflib.patch.Patch

/**
 * Data structure for one patched file from a unified diff file.
 */
class UnifiedDiffFile {
    var diffCommand: String? = null
    var fromFile: String? = null
    var fromTimestamp: String? = null
    var toFile: String? = null
    var renameFrom: String? = null
    var renameTo: String? = null
    var copyFrom: String? = null
    var copyTo: String? = null
    var toTimestamp: String? = null
    var index: String? = null
    var newFileMode: String? = null
    var oldMode: String? = null
    var newMode: String? = null
    var deletedFileMode: String? = null
    var binaryAdded: String? = null
    var binaryDeleted: String? = null
    var binaryEdited: String? = null
    var patch: Patch<String> = Patch()
    var noNewLineAtTheEndOfTheFile: Boolean = false
    var similarityIndex: Int? = null

    companion object {
        fun from(fromFile: String?, toFile: String?, patch: Patch<String>): UnifiedDiffFile {
            val file = UnifiedDiffFile()
            file.fromFile = fromFile
            file.toFile = toFile
            file.patch = patch
            return file
        }
    }
}
