package com.bernaferrari.difflib.patch

import com.bernaferrari.difflib.DiffUtils
import com.bernaferrari.difflib.algorithm.DiffAlgorithmFactory
import com.bernaferrari.difflib.algorithm.myers.MyersDiff
import com.bernaferrari.difflib.algorithm.myers.MyersDiffWithLinearSpace
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail

class PatchWithAllDiffAlgorithmsTest {

    companion object {
        @JvmStatic
        fun provideAlgorithms() = java.util.stream.Stream.of(
            Arguments.of(MyersDiff.factory()),
            Arguments.of(MyersDiffWithLinearSpace.factory())
        )

        @JvmStatic
        @AfterAll
        fun afterAll() {
            DiffUtils.withDefaultDiffAlgorithmFactory(MyersDiff.factory())
        }
    }

    @ParameterizedTest
    @MethodSource("provideAlgorithms")
    fun patchInsert(factory: DiffAlgorithmFactory) {
        DiffUtils.withDefaultDiffAlgorithmFactory(factory)
        val from = listOf("hhh")
        val to = listOf("hhh", "jjj", "kkk", "lll")
        val patch = DiffUtils.diff(from, to)
        try {
            assertEquals(to, DiffUtils.patch(from, patch))
        } catch (e: PatchFailedException) {
            fail<Unit>(e.message ?: "Patch insert failed for factory ${factory.javaClass.simpleName}")
        }
    }

    @ParameterizedTest
    @MethodSource("provideAlgorithms")
    fun patchDelete(factory: DiffAlgorithmFactory) {
        DiffUtils.withDefaultDiffAlgorithmFactory(factory)
        val from = listOf("ddd", "fff", "ggg", "hhh")
        val to = listOf("ggg")
        val patch = DiffUtils.diff(from, to)
        try {
            assertEquals(to, DiffUtils.patch(from, patch))
        } catch (e: PatchFailedException) {
            fail<Unit>(e.message ?: "Patch delete failed for factory ${factory.javaClass.simpleName}")
        }
    }

    @ParameterizedTest
    @MethodSource("provideAlgorithms")
    fun patchChange(factory: DiffAlgorithmFactory) {
        DiffUtils.withDefaultDiffAlgorithmFactory(factory)
        val from = listOf("aaa", "bbb", "ccc", "ddd")
        val to = listOf("aaa", "bxb", "cxc", "ddd")
        val patch = DiffUtils.diff(from, to)
        try {
            assertEquals(to, DiffUtils.patch(from, patch))
        } catch (e: PatchFailedException) {
            fail<Unit>(e.message ?: "Patch change failed for factory ${factory.javaClass.simpleName}")
        }
    }

}
