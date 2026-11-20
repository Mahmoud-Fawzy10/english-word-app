package com.example.export

import com.example.data.model.WordEntry
import com.example.data.model.WordLocation
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object JsonBackupManager {

    private const val BACKUP_FORMAT_VERSION = 2

    fun suggestedBackupFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return "Qamusi_Backup_$stamp.json"
    }

    fun exportBackup(words: List<WordEntry>, outputStream: OutputStream) {
        val root = JSONObject().apply {
            put("app", "Qamusi")
            put("backupVersion", BACKUP_FORMAT_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("wordCount", words.size)
            val arr = JSONArray()
            words.forEach { w -> arr.put(wordToJson(w)) }
            put("words", arr)
        }
        outputStream.write(root.toString(2).toByteArray(StandardCharsets.UTF_8))
        outputStream.flush()
    }

    fun parseBackup(inputStream: InputStream): List<WordEntry> {
        val raw = inputStream.readBytes().toString(StandardCharsets.UTF_8).trim()
        if (raw.isEmpty()) return emptyList()

        val wordsArray = if (raw.startsWith("[")) {
            JSONArray(raw)
        } else {
            val root = JSONObject(raw)
            root.optJSONArray("words")
                ?: error("ملف النسخة الاحتياطية لا يحتوي على قائمة كلمات صالحة")
        }

        return buildList {
            for (i in 0 until wordsArray.length()) {
                val obj = wordsArray.optJSONObject(i) ?: continue
                val entry = jsonToWord(obj)
                if (entry.hasAnyContent()) add(entry)
            }
        }
    }

    private fun wordToJson(entry: WordEntry): JSONObject = JSONObject().apply {
        put("word", entry.word)
        put("ipa", entry.ipa)
        put("arabicMeaning", entry.arabicMeaning)
        put("books", JSONArray(entry.books))
        put("locations", JSONArray().apply {
            entry.locations.forEach { loc ->
                put(JSONObject().apply {
                    put("book", loc.book)
                    put("unit", loc.unit)
                    put("lesson", loc.lesson)
                    put("page", loc.page)
                })
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
        put("lastWrongAt", entry.lastWrongAt)
        put("isFavorite", entry.isFavorite)
        put("createdAt", entry.createdAt)
        put("updatedAt", entry.updatedAt)
        put("srsRepetitions", entry.srsRepetitions)
        put("srsIntervalDays", entry.srsIntervalDays)
        put("srsEaseFactor", entry.srsEaseFactor.toDouble())
        put("srsNextReviewAt", entry.srsNextReviewAt)
        put("srsLastReviewedAt", entry.srsLastReviewedAt)
        put("srsTotalReviews", entry.srsTotalReviews)
        put("srsCorrectCount", entry.srsCorrectCount)
    }

    private fun jsonToWord(obj: JSONObject): WordEntry {
        val locsArr = obj.optJSONArray("locations")
        val locs = buildList {
            if (locsArr != null) {
                for (i in 0 until locsArr.length()) {
                    val lObj = locsArr.optJSONObject(i) ?: continue
                    val loc = WordLocation(
                        book = lObj.optString("book", ""),
                        unit = lObj.optString("unit", ""),
                        lesson = lObj.optString("lesson", ""),
                        page = lObj.optString("page", "")
                    )
                    if (loc.isNotBlank()) add(loc)
                }
            }
        }

        return WordEntry(
            word = obj.optString("word", ""),
            ipa = obj.optString("ipa", ""),
            arabicMeaning = obj.optString("arabicMeaning", ""),
            books = jsonArrayToStrings(obj.optJSONArray("books")),
            locations = locs,
            partsOfSpeech = jsonArrayToStrings(obj.optJSONArray("partsOfSpeech")),
            synonyms = jsonArrayToStrings(obj.optJSONArray("synonyms")),
            antonyms = jsonArrayToStrings(obj.optJSONArray("antonyms")),
            prepositions = jsonArrayToStrings(obj.optJSONArray("prepositions")),
            collocations = jsonArrayToStrings(obj.optJSONArray("collocations")),
            definition = obj.optString("definition", ""),
            derivatives = obj.optString("derivatives", ""),
            expressions = obj.optString("expressions", ""),
            linguisticNotes = obj.optString("linguisticNotes", ""),
            examples = jsonArrayToStrings(obj.optJSONArray("examples")),
            personalHint = obj.optString("personalHint", ""),
            notes = obj.optString("notes", ""),
            importantNotes = obj.optString("importantNotes", ""),
            difficulty = obj.optInt("difficulty", 0).coerceIn(0, 3),
            studyTags = jsonArrayToStrings(obj.optJSONArray("studyTags")),
            lastWrongAt = obj.optLong("lastWrongAt", 0L),
            isFavorite = obj.optBoolean("isFavorite", false),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
            srsRepetitions = obj.optInt("srsRepetitions", 0),
            srsIntervalDays = obj.optInt("srsIntervalDays", 0),
            srsEaseFactor = obj.optDouble("srsEaseFactor", 2.5).toFloat().coerceIn(1.3f, 3.2f),
            srsNextReviewAt = obj.optLong("srsNextReviewAt", 0L),
            srsLastReviewedAt = obj.optLong("srsLastReviewedAt", 0L),
            srsTotalReviews = obj.optInt("srsTotalReviews", 0),
            srsCorrectCount = obj.optInt("srsCorrectCount", 0)
        )
    }

    private fun jsonArrayToStrings(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val v = array.optString(i).trim()
                if (v.isNotBlank()) add(v)
            }
        }
    }
}
