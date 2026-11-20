package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(
    tableName = "word_revisions",
    indices = [Index(value = ["wordId", "editedAt"])]
)
data class WordRevision(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val wordId: Long,
    val snapshotJson: String,
    val changeSummaryAr: String,
    val editedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromWordEntry(entry: WordEntry, summaryAr: String, timestamp: Long = System.currentTimeMillis()): WordRevision {
            val obj = JSONObject().apply {
                put("word", entry.word)
                put("ipa", entry.ipa)
                put("arabicMeaning", entry.arabicMeaning)
                put("books", JSONArray(entry.books))
                put("locations", JSONArray().apply {
                    entry.locations.forEach { loc ->
                        put(
                            JSONObject().apply {
                                put("book", loc.book)
                                put("unit", loc.unit)
                                put("lesson", loc.lesson)
                                put("page", loc.page)
                            }
                        )
                    }
                })
                put("partsOfSpeech", JSONArray(entry.partsOfSpeech))
                put("synonyms", JSONArray(entry.synonyms))
                put("antonyms", JSONArray(entry.antonyms))
                put("prepositions", JSONArray(entry.prepositions))
                put("collocations", JSONArray(entry.collocations))
                put("definition", entry.definition)
                put("derivatives", entry.derivatives)
                put("expressions", entry.expressions)
                put("linguisticNotes", entry.linguisticNotes)
                put("examples", JSONArray(entry.examples))
                put("personalHint", entry.personalHint)
                put("notes", entry.notes)
                put("importantNotes", entry.importantNotes)
                put("difficulty", entry.difficulty)
                put("studyTags", JSONArray(entry.studyTags))
                put("isFavorite", entry.isFavorite)
            }
            return WordRevision(
                wordId = entry.id,
                snapshotJson = obj.toString(),
                changeSummaryAr = summaryAr,
                editedAt = timestamp
            )
        }

        fun applySnapshotTo(target: WordEntry, revision: WordRevision): WordEntry {
            val obj = JSONObject(revision.snapshotJson)
            fun strList(key: String): List<String> {
                val arr = obj.optJSONArray(key) ?: return emptyList()
                return buildList {
                    for (i in 0 until arr.length()) {
                        val s = arr.optString(i).trim()
                        if (s.isNotEmpty()) add(s)
                    }
                }
            }

            val locsArr = obj.optJSONArray("locations")
            val locs = buildList {
                if (locsArr != null) {
                    for (i in 0 until locsArr.length()) {
                        val item = locsArr.optJSONObject(i) ?: continue
                        val loc = WordLocation(
                            book = item.optString("book"),
                            unit = item.optString("unit"),
                            lesson = item.optString("lesson"),
                            page = item.optString("page")
                        )
                        if (loc.isNotBlank()) add(loc)
                    }
                }
            }

            return target.copy(
                word = obj.optString("word", target.word),
                ipa = obj.optString("ipa", ""),
                arabicMeaning = obj.optString("arabicMeaning", ""),
                books = strList("books"),
                locations = locs,
                partsOfSpeech = strList("partsOfSpeech"),
                synonyms = strList("synonyms"),
                antonyms = strList("antonyms"),
                prepositions = strList("prepositions"),
                collocations = strList("collocations"),
                definition = obj.optString("definition", ""),
                derivatives = obj.optString("derivatives", ""),
                expressions = obj.optString("expressions", ""),
                linguisticNotes = obj.optString("linguisticNotes", ""),
                examples = strList("examples"),
                personalHint = obj.optString("personalHint", ""),
                notes = obj.optString("notes", ""),
                importantNotes = obj.optString("importantNotes", ""),
                difficulty = obj.optInt("difficulty", target.difficulty),
                studyTags = strList("studyTags"),
                isFavorite = obj.optBoolean("isFavorite", target.isFavorite),
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
