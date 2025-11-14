package com.bernaferrari.difflib.algorithm

/**
 * Factory capable of creating new instances of a diff algorithm implementation.
 */
interface DiffAlgorithmFactory {
    fun <T> create(): DiffAlgorithmI<T>

    fun <T> create(equalizer: Equalizer<T>): DiffAlgorithmI<T>
}
