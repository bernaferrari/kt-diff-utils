package com.bernaferrari.difflib.serialization

import com.bernaferrari.difflib.DiffUtils
import com.bernaferrari.difflib.algorithm.DiffAlgorithmFactory
import com.bernaferrari.difflib.algorithm.myers.MyersDiff
import com.bernaferrari.difflib.algorithm.myers.MyersDiffWithLinearSpace
import com.bernaferrari.difflib.patch.Patch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class PatchSerializationTest {

    companion object {
        @JvmStatic
        fun algorithms(): Stream<Arguments> = Stream.of(
            Arguments.of(MyersDiff.factory()),
            Arguments.of(MyersDiffWithLinearSpace.factory())
        )
    }

    @ParameterizedTest
    @MethodSource("algorithms")
    fun patchSurvivesJavaSerialization(factory: DiffAlgorithmFactory) {
        val original = listOf("aaa", "bbb", "ccc", "ddd")
        val revised = listOf("aaa", "bxb", "cxc", "ddd")
        val patch = DiffUtils.diff(original, revised, factory.create())

        val restoredPatch = serializeAndRestore(patch)

        assertEquals(revised, DiffUtils.patch(original, restoredPatch))
    }

    private fun serializeAndRestore(patch: Patch<String>): Patch<String> {
        val bytes = ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use { it.writeObject(patch) }
            baos.toByteArray()
        }
        return ObjectInputStream(ByteArrayInputStream(bytes)).use {
            @Suppress("UNCHECKED_CAST")
            it.readObject() as Patch<String>
        }
    }
}
