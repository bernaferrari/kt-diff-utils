package com.bernaferrari.difflib.patch

/**
 * Base class for all diff related exceptions.
 */
open class DiffException : Exception {

    constructor() : super()

    constructor(message: String?) : super(message)

    companion object {
        private const val serialVersionUID = 1L
    }
}
