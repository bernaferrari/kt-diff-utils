package com.bernaferrari.difflib.algorithm.jgit

import com.bernaferrari.difflib.algorithm.Change
import com.bernaferrari.difflib.algorithm.DiffAlgorithmI
import com.bernaferrari.difflib.algorithm.DiffAlgorithmListener
import com.bernaferrari.difflib.patch.DeltaType
import org.eclipse.jgit.diff.Edit
import org.eclipse.jgit.diff.EditList
import org.eclipse.jgit.diff.Sequence
import org.eclipse.jgit.diff.SequenceComparator

class HistogramDiff<T> : DiffAlgorithmI<T> {

    override fun computeDiff(
        source: List<T>,
        target: List<T>,
        progress: DiffAlgorithmListener?
    ): List<Change> {
        progress?.diffStart()
        val diffList = EditList()
        diffList.addAll(
            org.eclipse.jgit.diff.HistogramDiff().diff(
                DataListComparator(progress),
                DataList(source),
                DataList(target)
            )
        )
        val patch = ArrayList<Change>()
        for (edit in diffList) {
            val type = when (edit.type) {
                Edit.Type.DELETE -> DeltaType.DELETE
                Edit.Type.INSERT -> DeltaType.INSERT
                Edit.Type.REPLACE -> DeltaType.CHANGE
                else -> DeltaType.EQUAL
            }
            patch.add(Change(type, edit.beginA, edit.endA, edit.beginB, edit.endB))
        }
        progress?.diffEnd()
        return patch
    }
}

private class DataListComparator<T>(
    private val progress: DiffAlgorithmListener?
) : SequenceComparator<DataList<T>>() {

    override fun equals(original: DataList<T>, orgIdx: Int, revised: DataList<T>, revIdx: Int): Boolean {
        progress?.diffStep(orgIdx + revIdx, original.size() + revised.size())
        return original.data[orgIdx] == revised.data[revIdx]
    }

    override fun hash(s: DataList<T>, i: Int): Int = s.data[i].hashCode()
}

private class DataList<T>(val data: List<T>) : Sequence() {
    override fun size(): Int = data.size
}
