package com.bernaferrari.difflib.patch

/**
 * Thrown whenever a delta cannot be applied as a patch to a given text.
 */
class PatchFailedException : DiffException {

    constructor() : super()

    constructor(message: String?) : super(message)

    companion object {
        private const val serialVersionUID = 1L
    }
}
