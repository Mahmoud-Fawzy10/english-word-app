package com.example.data.repository

import com.example.data.local.WordDao
import com.example.data.local.WordRevisionDao
import com.example.data.model.DuplicatePolicy
import com.example.data.model.WordEntry
import com.example.data.model.WordRevision
import com.example.domain.SrsGrade
import com.example.domain.SrsScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

data class ImportSummary(
    val insertedCount: Int = 0,
    val mergedCount: Int = 0,
    val skippedCount: Int = 0
)

sealed interface SaveWordResult {
    data class Saved(val id: Long) : SaveWordResult
    data class Merged(val id: Long) : SaveWordResult
    data class DuplicateRejected(val existing: WordEntry) : SaveWordResult
    data object EmptyRejected : SaveWordResult
}

class WordRepository(
    private val wordDao: WordDao,
    private val revisionDao: WordRevisionDao? = null
) {

    val allWords: Flow<List<WordEntry>> = wordDao.getAllWords()

    suspend fun getAllWordsOnce(): List<WordEntry> = wordDao.getAllWordsOnce()

    fun observeWordById(id: Long): Flow<WordEntry?> = wordDao.getWordById(id)

    suspend fun getWordOnce(id: Long): WordEntry? = wordDao.getWordByIdOnce(id)

    /**
     * Strict anti-duplication save:
     * Refuses to insert or update an entry if another word with the same normalized key exists,
     * unless [mergeTarget] is explicitly provided.
     */
    suspend fun saveWordChecked(
        entry: WordEntry,
        mergeTarget: WordEntry? = null
    ): SaveWordResult {
        if (!entry.hasAnyContent()) return SaveWordResult.EmptyRejected

        if (mergeTarget != null) {
            val merged = mergeEntries(mergeTarget, entry)
            if (entry.id > 0L && entry.id != mergeTarget.id) {
                wordDao.deleteWordById(entry.id)
            }
            return SaveWordResult.Merged(merged.id)
        }

        val duplicates = findDuplicatesFor(
            word = entry.word,
            arabicMeaning = entry.arabicMeaning,
            excludeId = entry.id
        )
        if (duplicates.isNotEmpty()) {
            return SaveWordResult.DuplicateRejected(duplicates.first())
        }

        val now = System.currentTimeMillis()
        val id = if (entry.id == 0L) {
            wordDao.insertWord(entry.copy(createdAt = now, updatedAt = now))
        } else {
            saveRevisionSnapshotIfChanged(entry.id, summaryAr = "تعديل الكلمة")
            wordDao.updateWord(entry.copy(updatedAt = now))
            entry.id
        }
        return SaveWordResult.Saved(id)
    }

    /**
     * Legacy helper delegating to strict saveWordChecked; throws or returns -1L if rejected as duplicate.
     */
    suspend fun saveWord(entry: WordEntry): Long {
        return when (val res = saveWordChecked(entry)) {
            is SaveWordResult.Saved -> res.id
            is SaveWordResult.Merged -> res.id
            is SaveWordResult.DuplicateRejected -> -1L
            SaveWordResult.EmptyRejected -> -1L
        }
    }

    fun observeRevisions(wordId: Long): Flow<List<WordRevision>> {
        return revisionDao?.observeRevisionsForWord(wordId) ?: flowOf(emptyList())
    }

    suspend fun restoreRevision(revisionId: Long): WordEntry? {
        val dao = revisionDao ?: return null
        val rev = dao.getRevisionById(revisionId) ?: return null
        val current = wordDao.getWordByIdOnce(rev.wordId) ?: return null
        saveRevisionSnapshotIfChanged(current.id, summaryAr = "قبل استعادة نسخة سابقة")
        val restored = WordRevision.applySnapshotTo(current, rev)
        wordDao.updateWord(restored)
        return restored
    }

    private suspend fun saveRevisionSnapshotIfChanged(wordId: Long, summaryAr: String) {
        val dao = revisionDao ?: return
        val before = wordDao.getWordByIdOnce(wordId) ?: return
        dao.insertRevision(WordRevision.fromWordEntry(before, summaryAr))
        dao.pruneRevisionsForWord(wordId, keepCount = 10)
    }

    suspend fun deleteWord(id: Long) {
        wordDao.deleteWordById(id)
        revisionDao?.deleteRevisionsForWord(id)
    }

    suspend fun deleteWordsBatch(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            wordDao.deleteWordsByIds(ids)
            ids.forEach { revisionDao?.deleteRevisionsForWord(it) }
        }
    }

    suspend fun toggleFavorite(id: Long, currentFavorite: Boolean) {
        wordDao.setFavorite(
            id = id,
            isFavorite = !currentFavorite,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun setFavoriteBatch(ids: List<Long>, isFavorite: Boolean) {
        if (ids.isNotEmpty()) {
            wordDao.setFavoriteBatch(ids, isFavorite, System.currentTimeMillis())
        }
    }

    suspend fun setDifficultyBatch(ids: List<Long>, difficulty: Int) {
        if (ids.isNotEmpty()) {
            wordDao.setDifficultyBatch(ids, difficulty.coerceIn(0, 3), System.currentTimeMillis())
        }
    }

    suspend fun addStudyTagBatch(ids: List<Long>, tag: String) {
        val cleanTag = tag.trim()
        if (ids.isEmpty() || cleanTag.isEmpty()) return
        val now = System.currentTimeMillis()
        val all = wordDao.getAllWordsOnce()
        all.filter { ids.contains(it.id) }.forEach { entry ->
            if (entry.studyTags.none { it.equals(cleanTag, ignoreCase = true) }) {
                wordDao.updateWord(
                    entry.copy(
                        studyTags = (entry.studyTags + cleanTag).distinct(),
                        updatedAt = now
                    )
                )
            }
        }
    }

    suspend fun recordSrsReview(entry: WordEntry, grade: SrsGrade, nowMillis: Long = System.currentTimeMillis()) {
        val result = SrsScheduler.scheduleNext(entry, grade, nowMillis)
        val wasCorrect = grade != SrsGrade.AGAIN
        val newWrongAt = if (!wasCorrect) nowMillis else 0L
        wordDao.updateSrsStats(
            id = entry.id,
            repetitions = result.repetitions,
            intervalDays = result.intervalDays,
            easeFactor = result.easeFactor,
            nextReviewAt = result.nextReviewAt,
            lastReviewedAt = nowMillis,
            totalReviews = entry.srsTotalReviews + 1,
            correctCount = entry.srsCorrectCount + if (wasCorrect) 1 else 0,
            lastWrongAt = newWrongAt,
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
            lastWrongAt = if (resetProgress) 0L else entry.lastWrongAt,
            updatedAt = nowMillis
        )
    }

    // -------------------------------------------------------------------------
    // Duplicate Detection, Merge & Offline Import/Restore (Zero Duplicates)
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
        saveRevisionSnapshotIfChanged(existing.id, summaryAr = "دمج مع إدخال مطابق")
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
            prepositions = (existing.prepositions + incoming.prepositions).map { it.trim() }.filter { it.isNotBlank() }.distinct(),
            collocations = (existing.collocations + incoming.collocations).map { it.trim() }.filter { it.isNotBlank() }.distinct(),
            definition = combineTextField(existing.definition, incoming.definition, "\n"),
            derivatives = combineTextField(existing.derivatives, incoming.derivatives, "\n"),
            expressions = combineTextField(existing.expressions, incoming.expressions, "\n"),
            linguisticNotes = combineTextField(existing.linguisticNotes, incoming.linguisticNotes, "\n"),
            examples = (existing.examples + incoming.examples).map { it.trim() }.filter { it.isNotBlank() }.distinct(),
            personalHint = combineTextField(existing.personalHint, incoming.personalHint, "\n"),
            notes = combineTextField(existing.notes, incoming.notes, "\n"),
            importantNotes = combineTextField(existing.importantNotes, incoming.importantNotes, "\n"),
            difficulty = maxOf(existing.difficulty, incoming.difficulty),
            studyTags = (existing.studyTags + incoming.studyTags).map { it.trim() }.filter { it.isNotBlank() }.distinct(),
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

            if (match == null) {
                toInsert.add(clean)
                existingList.add(clean)
                inserted++
            } else {
                when (policy) {
                    DuplicatePolicy.SKIP -> skipped++
                    DuplicatePolicy.MERGE -> {
                        if (match.id > 0L) {
                            val updated = mergeEntries(match, clean)
                            val idx = existingList.indexOfFirst { it.id == match.id }
                            if (idx >= 0) existingList[idx] = updated
                        } else {
                            // Matched a newly queued word in the same batch before DB insert
                            val idx = toInsert.indexOfFirst {
                                normalizeKey(it.word) == normWord && normWord.isNotEmpty() ||
                                    (normWord.isEmpty() && normalizeKey(it.arabicMeaning) == normMeaning)
                            }
                            if (idx >= 0) {
                                val prev = toInsert[idx]
                                toInsert[idx] = prev.copy(
                                    arabicMeaning = combineTextField(prev.arabicMeaning, clean.arabicMeaning, "، "),
                                    synonyms = (prev.synonyms + clean.synonyms).distinct(),
                                    antonyms = (prev.antonyms + clean.antonyms).distinct(),
                                    prepositions = (prev.prepositions + clean.prepositions).distinct(),
                                    collocations = (prev.collocations + clean.collocations).distinct(),
                                    locations = (prev.locations + clean.locations).distinct()
                                )
                            }
                        }
                        merged++
                    }
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
            val deduplicated = mutableListOf<WordEntry>()
            val seen = mutableSetOf<String>()
            backupWords.filter { it.hasAnyContent() }.forEach { w ->
                val key = normalizeKey(w.word).ifEmpty { "m:${normalizeKey(w.arabicMeaning)}" }
                if (seen.add(key)) {
                    deduplicated.add(w.copy(id = 0L))
                }
            }
            wordDao.replaceAllWords(deduplicated)
            return ImportSummary(insertedCount = deduplicated.size)
        }
        return importWords(backupWords, DuplicatePolicy.MERGE)
    }

    fun findRelatedWords(target: WordEntry, allWords: List<WordEntry>, limit: Int = 8): List<WordEntry> {
        val targetWordNorm = normalizeKey(target.word)
        val targetSyns = target.synonyms.map { normalizeKey(it) }.filter { it.isNotEmpty() }.toSet()
        val targetAnts = target.antonyms.map { normalizeKey(it) }.filter { it.isNotEmpty() }.toSet()
        val targetUnits = target.locations.map { normalizeKey(it.unit) }.filter { it.isNotEmpty() }.toSet()

        return allWords
            .asSequence()
            .filter { it.id != target.id }
            .mapNotNull { candidate ->
                val candNorm = normalizeKey(candidate.word)
                val candSyns = candidate.synonyms.map { normalizeKey(it) }.toSet()
                val candAnts = candidate.antonyms.map { normalizeKey(it) }.toSet()
                val candUnits = candidate.locations.map { normalizeKey(it.unit) }.toSet()

                var score = 0
                if (targetSyns.contains(candNorm) || candSyns.contains(targetWordNorm)) score += 5
                if (targetAnts.contains(candNorm) || candAnts.contains(targetWordNorm)) score += 5
                if (targetSyns.intersect(candSyns).isNotEmpty()) score += 3
                if (targetAnts.intersect(candAnts).isNotEmpty()) score += 3
                if (targetUnits.intersect(candUnits).isNotEmpty()) score += 1
                if (score > 0) candidate to score else null
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .toList()
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
