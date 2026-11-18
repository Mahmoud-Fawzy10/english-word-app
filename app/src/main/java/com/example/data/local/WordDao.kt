package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.WordEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM word_entries ORDER BY createdAt DESC")
    fun getAllWords(): Flow<List<WordEntry>>

    @Query("SELECT * FROM word_entries ORDER BY createdAt DESC")
    suspend fun getAllWordsOnce(): List<WordEntry>

    @Query("SELECT * FROM word_entries WHERE id = :id LIMIT 1")
    fun getWordById(id: Long): Flow<WordEntry?>

    @Query("SELECT * FROM word_entries WHERE id = :id LIMIT 1")
    suspend fun getWordByIdOnce(id: Long): WordEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: WordEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntry>): List<Long>

    @Update
    suspend fun updateWord(word: WordEntry)

    @Query("DELETE FROM word_entries WHERE id = :id")
    suspend fun deleteWordById(id: Long)

    @Query("DELETE FROM word_entries")
    suspend fun deleteAllWords()

    @Transaction
    suspend fun replaceAllWords(words: List<WordEntry>) {
        deleteAllWords()
        if (words.isNotEmpty()) {
            insertWords(words)
        }
    }

    @Query("UPDATE word_entries SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE word_entries
        SET srsRepetitions = :repetitions,
            srsIntervalDays = :intervalDays,
            srsEaseFactor = :easeFactor,
            srsNextReviewAt = :nextReviewAt,
            srsLastReviewedAt = :lastReviewedAt,
            srsTotalReviews = :totalReviews,
            srsCorrectCount = :correctCount,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateSrsStats(
        id: Long,
        repetitions: Int,
        intervalDays: Int,
        easeFactor: Float,
        nextReviewAt: Long,
        lastReviewedAt: Long,
        totalReviews: Int,
        correctCount: Int,
        updatedAt: Long
    )
}
