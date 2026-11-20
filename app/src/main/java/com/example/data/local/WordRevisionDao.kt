package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.WordRevision
import kotlinx.coroutines.flow.Flow

@Dao
interface WordRevisionDao {
    @Query("SELECT * FROM word_revisions WHERE wordId = :wordId ORDER BY editedAt DESC")
    fun observeRevisionsForWord(wordId: Long): Flow<List<WordRevision>>

    @Query("SELECT * FROM word_revisions WHERE wordId = :wordId ORDER BY editedAt DESC")
    suspend fun getRevisionsForWordOnce(wordId: Long): List<WordRevision>

    @Query("SELECT * FROM word_revisions WHERE id = :revisionId LIMIT 1")
    suspend fun getRevisionById(revisionId: Long): WordRevision?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevision(revision: WordRevision): Long

    @Query("DELETE FROM word_revisions WHERE wordId = :wordId")
    suspend fun deleteRevisionsForWord(wordId: Long)

    @Query(
        """
        DELETE FROM word_revisions
        WHERE wordId = :wordId AND id NOT IN (
            SELECT id FROM word_revisions WHERE wordId = :wordId ORDER BY editedAt DESC LIMIT :keepCount
        )
        """
    )
    suspend fun pruneRevisionsForWord(wordId: Long, keepCount: Int = 10)
}
