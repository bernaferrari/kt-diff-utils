package com.bernaferrari.difflib.algorithm

/**
 * Function type used to decide whether two values are considered equal when diffing.
 */
typealias Equalizer<T> = (T, T) -> Boolean
