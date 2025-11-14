package com.bernaferrari.difflib.text

import java.lang.Character

internal object StringUtils {

    fun htmlEntites(str: String): String = str.replace("<", "&lt;").replace(">", "&gt;")

    fun normalize(str: String): String = htmlEntites(str).replace("\t", "    ")

    fun wrapText(list: List<String>, columnWidth: Int): List<String> = list.map { wrapText(it, columnWidth) }

    fun wrapText(line: String, columnWidth: Int): String {
        require(columnWidth >= 0) { "columnWidth may not be less 0" }
        if (columnWidth == 0) {
            return line
        }
        val length = line.length
        val delimiter = "<br/>".length
        var widthIndex = columnWidth
        val builder = StringBuilder(line)

        var count = 0
        while (length > widthIndex) {
            var breakPoint = widthIndex + delimiter * count
            if (Character.isHighSurrogate(builder[breakPoint - 1]) && Character.isLowSurrogate(builder[breakPoint])) {
                breakPoint += 1
                if (breakPoint == builder.length) {
                    breakPoint -= 2
                }
            }
            builder.insert(breakPoint, "<br/>")
            widthIndex += columnWidth
            count++
        }
        return builder.toString()
    }
}
