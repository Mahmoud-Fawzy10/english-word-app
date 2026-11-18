package com.example.data.repository

import com.example.data.local.WordDao
import com.example.data.model.DuplicatePolicy
import com.example.data.model.WordEntry
import com.example.domain.SrsGrade
import com.example.domain.SrsScheduler
import kotlinx.coroutines.flow.Flow

data class ImportSummary(
    val insertedCount: Int = 0,
    val mergedCount: Int = 0,
    val skippedCount: Int = 0
)

class WordRepository(private val wordDao: WordDao) {

    val allWords: Flow<List<WordEntry>> = wordDao.getAllWords()

    suspend fun getAllWordsOnce(): List<WordEntry> = wordDao.getAllWordsOnce()

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

    // -------------------------------------------------------------------------
    // Duplicate Detection, Merge & Offline Import/Restore
    // -------------------------------------------------------------------------
    suspend fun findDuplicatesFor(
        word: String,
        arabicMeaning: String,
        excludeId: Long = 0L
    ): List<WordEntry> {
        val normWord = normalizeKey(word)
        val normMeaning = normalizeKey(arabicMeaning)
        if (normWord.isEmpty() && normMeaning.isEmpty()) return emptyList()

        return wordDao.getAllWordsOnce().filter { candidate ->
            if (candidate.id == excludeId) return@filter false
            val candWord = normalizeKey(candidate.word)
            val candMeaning = normalizeKey(candidate.arabicMeaning)
            (normWord.isNotEmpty() && candWord == normWord) ||
                (normWord.isEmpty() && normMeaning.isNotEmpty() && candMeaning == normMeaning)
        }
    }

    fun findDuplicateGroups(words: List<WordEntry>): List<List<WordEntry>> {
        return words
            .groupBy { entry ->
                val w = normalizeKey(entry.word)
                if (w.isNotEmpty()) "w:$w" else "m:${normalizeKey(entry.arabicMeaning)}"
            }
            .filter { (key, group) -> key != "m:" && group.size > 1 }
            .values
            .toList()
    }

    suspend fun mergeEntries(existing: WordEntry, incoming: WordEntry): WordEntry {
        val now = System.currentTimeMillis()
        val merged = existing.copy(
            word = existing.word.ifBlank { incoming.word.trim() },
            ipa = existing.ipa.ifBlank { incoming.ipa.trim() },
            arabicMeaning = combineTextField(existing.arabicMeaning, incoming.arabicMeaning, "، "),
            books = (existing.books + incoming.books).map { it.trim() }.filter { it.isNotBlank() }.distinct(),
            locations = (existing.locations + incoming.locations).filter { it.isNotBlank() }.distinct(),
            partsOfSpeech = (existing.partsOfSpeech + incoming.partsOfSpeech).map { it.trim() }.filter { it.isNotBlank() }.distinct(),
            synonyms = (existing.synonyms + incoming.synonyms).map { it.trim() }.filter { it.isNotBlank() }.distinct(),
            antonyms = (existing.antonyms + incoming.antonyms).map { it.trim() }.filter { it.isNotBlank() }.distinct(),
            definition = combineTextField(existing.definition, incoming.definition, "\n"),
            derivatives = combineTextField(existing.derivatives, incoming.derivatives, "\n"),
            expressions = combineTextField(existing.expressions, incoming.expressions, "\n"),
            notes = combineTextField(existing.notes, incoming.notes, "\n"),
            importantNotes = combineTextField(existing.importantNotes, incoming.importantNotes, "\n"),
            isFavorite = existing.isFavorite || incoming.isFavorite,
            updatedAt = now
        )
        wordDao.updateWord(merged)
        return merged
    }

    suspend fun mergeDuplicateGroup(group: List<WordEntry>): WordEntry? {
        if (group.size < 2) return group.firstOrNull()
        val ordered = group.sortedWith(
            compareByDescending<WordEntry> { it.srsTotalReviews }
                .thenBy { it.createdAt }
                .thenBy { it.id }
        )
        val primary = ordered.first()
        var current = primary
        ordered.drop(1).forEach { extra ->
            current = mergeEntries(current, extra)
            wordDao.deleteWordById(extra.id)
        }
        return current
    }

    suspend fun importWords(
        incomingWords: List<WordEntry>,
        policy: DuplicatePolicy
    ): ImportSummary {
        val existingList = wordDao.getAllWordsOnce().toMutableList()
        val now = System.currentTimeMillis()
        var inserted = 0
        var merged = 0
        var skipped = 0

        val toInsert = mutableListOf<WordEntry>()
        for (raw in incomingWords) {
            if (!raw.hasAnyContent()) continue
            val clean = raw.copy(id = 0L, createdAt = if (raw.createdAt > 0L) raw.createdAt else now, updatedAt = now)
            val normWord = normalizeKey(clean.word)
            val normMeaning = normalizeKey(clean.arabicMeaning)

            val match = existingList.firstOrNull { ex ->
                val exW = normalizeKey(ex.word)
                val exM = normalizeKey(ex.arabicMeaning)
                (normWord.isNotEmpty() && exW == normWord) ||
                    (normWord.isEmpty() && normMeaning.isNotEmpty() && exM == normMeaning)
            }

            if (match == null || policy == DuplicatePolicy.KEEP_BOTH) {
                toInsert.add(clean)
                existingList.add(clean)
                inserted++
            } else {
                when (policy) {
                    DuplicatePolicy.SKIP -> skipped++
                    DuplicatePolicy.MERGE -> {
                        val updated = mergeEntries(match, clean)
                        val idx = existingList.indexOfFirst { it.id == match.id }
                        if (idx >= 0) existingList[idx] = updated
                        merged++
                    }
                    DuplicatePolicy.KEEP_BOTH -> Unit
                }
            }
        }

        if (toInsert.isNotEmpty()) {
            wordDao.insertWords(toInsert)
        }
        return ImportSummary(insertedCount = inserted, mergedCount = merged, skippedCount = skipped)
    }

    suspend fun restoreFullJsonBackup(
        backupWords: List<WordEntry>,
        replaceExisting: Boolean
    ): ImportSummary {
        if (replaceExisting) {
            val cleaned = backupWords.filter { it.hasAnyContent() }.map { it.copy(id = 0L) }
            wordDao.replaceAllWords(cleaned)
            return ImportSummary(insertedCount = cleaned.size)
        }
        return importWords(backupWords, DuplicatePolicy.MERGE)
    }

    private fun combineTextField(existing: String, incoming: String, separator: String): String {
        val a = existing.trim()
        val b = incoming.trim()
        return when {
            a.isEmpty() -> b
            b.isEmpty() -> a
            normalizeKey(a).contains(normalizeKey(b)) -> a
            else -> "$a$separator$b"
        }
    }

    companion object {
        fun normalizeKey(text: String): String {
            return text.trim()
                .lowercase()
                .replace(Regex("[\u064B-\u065F\u0670]"), "")
                .replace('أ', 'ا')
                .replace('إ', 'ا')
                .replace('آ', 'ا')
                .replace('ة', 'ه')
                .replace('ى', 'ي')
                .replace('ؤ', 'و')
                .replace('ئ', 'ي')
                .replace(Regex("\\s+"), " ")
        }
    }
}
