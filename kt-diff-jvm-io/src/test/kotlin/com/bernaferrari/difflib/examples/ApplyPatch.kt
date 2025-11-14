package com.bernaferrari.difflib.examples

import com.bernaferrari.difflib.DiffUtils
import com.bernaferrari.difflib.TestConstants
import com.bernaferrari.difflib.UnifiedDiffUtils
import com.bernaferrari.difflib.patch.PatchFailedException
import java.io.File
import java.io.IOException
import java.nio.file.Files

object ApplyPatch {
    private val ORIGINAL = TestConstants.MOCK_FOLDER + "issue10_base.txt"
    private val PATCH = TestConstants.MOCK_FOLDER + "issue10_patch.txt"

    @JvmStatic
    @Throws(PatchFailedException::class, IOException::class)
    fun main(args: Array<String>) {
        val original = Files.readAllLines(File(ORIGINAL).toPath())
        val patched = Files.readAllLines(File(PATCH).toPath())

        val patch = UnifiedDiffUtils.parseUnifiedDiff(patched)
        val result = DiffUtils.patch(original, patch)
        println(result)
    }
}
