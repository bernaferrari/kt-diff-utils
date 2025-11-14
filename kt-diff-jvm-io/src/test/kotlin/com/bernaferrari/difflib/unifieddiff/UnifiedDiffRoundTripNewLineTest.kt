package com.bernaferrari.difflib.unifieddiff

import com.bernaferrari.difflib.patch.PatchFailedException
import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("for next release")
class UnifiedDiffRoundTripNewLineTest {

    @Test
    @Throws(IOException::class, PatchFailedException::class)
    fun issue135MissingNoNewLineInPatched() {
        val beforeContent = "rootProject.name = \"sample-repo\""
        val afterContent = "rootProject.name = \"sample-repo\"\n"
        val patch = """
            diff --git a/settings.gradle b/settings.gradle
            index ef3b8e2..ab30124 100644
            --- a/settings.gradle
            +++ b/settings.gradle
            @@ -1 +1 @@
            -rootProject.name = "sample-repo"
            \ No newline at end of file
            +rootProject.name = "sample-repo"
        """.trimIndent()

        val unifiedDiff = UnifiedDiffReader.parseUnifiedDiff(ByteArrayInputStream(patch.toByteArray()))
        val unifiedAfterContent = unifiedDiff.getFiles()[0].patch
            .applyTo(beforeContent.split("\n"))
            .joinToString("\n")

        assertEquals(afterContent, unifiedAfterContent)
    }
}
