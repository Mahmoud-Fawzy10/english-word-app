package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word_entries")
data class WordEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val word: String = "",
    val ipa: String = "",
    val arabicMeaning: String = "",
    val books: List<String> = emptyList(),
    val locations: List<WordLocation> = emptyList(),
    val partsOfSpeech: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val definition: String = "",
    val derivatives: String = "",
    val expressions: String = "",
    val notes: String = "",
    val importantNotes: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Spaced Repetition System (SRS) fields
    val srsRepetitions: Int = 0,
    val srsIntervalDays: Int = 0,
    val srsEaseFactor: Float = 2.5f,
    val srsNextReviewAt: Long = 0L,
    val srsLastReviewedAt: Long = 0L,
    val srsTotalReviews: Int = 0,
    val srsCorrectCount: Int = 0,
    // v4 Extended Study & Linguistic Fields (all optional)
    val prepositions: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val linguisticNotes: String = "",
    val examples: List<String> = emptyList(),
    val personalHint: String = "",
    val difficulty: Int = 0, // 0 = عادي, 1 = سهل, 2 = متوسط, 3 = صعب
    val studyTags: List<String> = emptyList(),
    val lastWrongAt: Long = 0L
) {
    fun displayTitle(): String = when {
        word.isNotBlank() -> word
        arabicMeaning.isNotBlank() -> arabicMeaning
        definition.isNotBlank() -> definition.take(40)
        else -> "كلمة بدون عنوان"
    }

    fun hasAnyContent(): Boolean =
        word.isNotBlank() ||
            ipa.isNotBlank() ||
            arabicMeaning.isNotBlank() ||
            books.isNotEmpty() ||
            locations.any { it.isNotBlank() } ||
            partsOfSpeech.isNotEmpty() ||
            synonyms.isNotEmpty() ||
            antonyms.isNotEmpty() ||
            definition.isNotBlank() ||
            derivatives.isNotBlank() ||
            expressions.isNotBlank() ||
            notes.isNotBlank() ||
            importantNotes.isNotBlank() ||
            prepositions.isNotEmpty() ||
            collocations.isNotEmpty() ||
            linguisticNotes.isNotBlank() ||
            examples.isNotEmpty() ||
            personalHint.isNotBlank() ||
            studyTags.isNotEmpty()

    fun isDueForReview(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!hasAnyContent()) return false
        return srsNextReviewAt == 0L || srsNextReviewAt <= nowMillis
    }

    fun isNewForSrs(): Boolean = srsTotalReviews == 0 && srsNextReviewAt == 0L

    fun isHardOrMistake(): Boolean = difficulty >= 3 || lastWrongAt > 0L

    fun difficultyLabelAr(): String = when (difficulty) {
        1 -> "سهل"
        2 -> "متوسط"
        3 -> "صعب"
        else -> "عادي"
    }

    fun firstUnitNumberOrNull(): Int? =
        locations.firstNotNullOfOrNull { extractFirstInt(it.unit) }

    fun firstPageNumberOrNull(): Int? =
        locations.firstNotNullOfOrNull { extractFirstInt(it.page) }

    private fun extractFirstInt(text: String): Int? =
        Regex("\\d+").find(text)?.value?.toIntOrNull()
}
