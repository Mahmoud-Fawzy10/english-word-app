package com.example.data.model

/**
 * Represents a curriculum location for a word entry.
 * Supports multiple locations, multiple lessons (e.g. "1, 2"), and multiple pages (e.g. "45, 48").
 */
data class WordLocation(
    val book: String = "",
    val unit: String = "",
    val lesson: String = "",
    val page: String = ""
) {
    fun isNotBlank(): Boolean =
        book.isNotBlank() || unit.isNotBlank() || lesson.isNotBlank() || page.isNotBlank()

    fun formattedLabel(): String {
        val parts = mutableListOf<String>()
        if (book.isNotBlank()) parts.add(book.trim())
        if (unit.isNotBlank()) parts.add("وحدة ${unit.trim()}")
        if (lesson.isNotBlank()) parts.add("درس ${lesson.trim()}")
        if (page.isNotBlank()) parts.add("ص ${page.trim()}")
        return parts.joinToString(" • ")
    }
}
