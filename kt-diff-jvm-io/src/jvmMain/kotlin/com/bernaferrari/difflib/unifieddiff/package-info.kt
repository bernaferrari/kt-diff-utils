package com.bernaferrari.difflib.unifieddiff

/**
 * This is the new implementation of UnifiedDiff tools. This version is multi-file aware.
 *
 * Use [UnifiedDiffReader.parseUnifiedDiff] to read a unified diff file and obtain a [UnifiedDiff]
 * that holds all information about the diffs and the affected files. To process a unified diff,
 * call [UnifiedDiffWriter.write].
 */
