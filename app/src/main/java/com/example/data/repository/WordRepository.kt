package com.example.data.repository

import com.example.data.local.WordDao
import com.example.data.model.WordEntry
import com.example.domain.SrsGrade
import com.example.domain.SrsScheduler
import kotlinx.coroutines.flow.Flow

class WordRepository(private val wordDao: WordDao) {

    val allWords: Flow<List<WordEntry>> = wordDao.getAllWords()

    fun observeWordById(id: Long): Flow<WordEntry?> = wordDao.getWordById(id)

    suspend fun getWordOnce(id: Long): WordEntry? = wordDao.getWordByIdOnce(id)

    suspend fun saveWord(entry: WordEntry): Long {
        val now = System.currentTimeMillis()
        return if (entry.id == 0L) {
            wordDao.insertWord(entry.copy(createdAt = now, updatedAt = now))
        } else {
            wordDao.updateWord(entry.copy(updatedAt = now))
            entry.id
        }
    }

    suspend fun deleteWord(id: Long) {
        wordDao.deleteWordById(id)
    }

    suspend fun toggleFavorite(id: Long, currentFavorite: Boolean) {
        wordDao.setFavorite(
            id = id,
            isFavorite = !currentFavorite,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun recordSrsReview(entry: WordEntry, grade: SrsGrade, nowMillis: Long = System.currentTimeMillis()) {
        val result = SrsScheduler.scheduleNext(entry, grade, nowMillis)
        val wasCorrect = grade != SrsGrade.AGAIN
        wordDao.updateSrsStats(
            id = entry.id,
            repetitions = result.repetitions,
            intervalDays = result.intervalDays,
            easeFactor = result.easeFactor,
            nextReviewAt = result.nextReviewAt,
            lastReviewedAt = nowMillis,
            totalReviews = entry.srsTotalReviews + 1,
            correctCount = entry.srsCorrectCount + if (wasCorrect) 1 else 0,
            updatedAt = nowMillis
        )
    }

    suspend fun updateManualSrsSchedule(
        entry: WordEntry,
        intervalDays: Int,
        easeFactor: Float = entry.srsEaseFactor,
        resetProgress: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val safeDays = intervalDays.coerceIn(0, 3650)
        val nextAt = if (safeDays == 0) 0L else nowMillis + safeDays * SrsScheduler.DAY_MILLIS
        val newReps = if (resetProgress) 0 else if (safeDays > 0 && entry.srsRepetitions == 0) 1 else entry.srsRepetitions
        wordDao.updateSrsStats(
            id = entry.id,
            repetitions = newReps,
            intervalDays = safeDays,
            easeFactor = easeFactor.coerceIn(1.3f, 3.2f),
            nextReviewAt = nextAt,
            lastReviewedAt = if (resetProgress) 0L else entry.srsLastReviewedAt,
            totalReviews = if (resetProgress) 0 else entry.srsTotalReviews,
            correctCount = if (resetProgress) 0 else entry.srsCorrectCount,
            updatedAt = nowMillis
        )
    }
}
