package com.example.export

import com.example.data.model.WordEntry
import com.example.data.model.WordLocation
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

object DictionaryImporter {

    fun parseCsv(inputStream: InputStream): List<WordEntry> {
        val rawBytes = inputStream.readBytes()
        val text = if (rawBytes.size >= 3 &&
            rawBytes[0] == 0xEF.toByte() &&
            rawBytes[1] == 0xBB.toByte() &&
            rawBytes[2] == 0xBF.toByte()
        ) {
            String(rawBytes, 3, rawBytes.size - 3, StandardCharsets.UTF_8)
        } else {
            String(rawBytes, StandardCharsets.UTF_8)
        }
        val rows = parseCsvRows(text)
        return mapRowsToWordEntries(rows)
    }

    fun parseXlsx(inputStream: InputStream): List<WordEntry> {
        val zipEntries = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    zipEntries[entry.name] = zis.readBytes()
                }
                entry = zis.nextEntry
            }
        }

        val sharedStrings = zipEntries["xl/sharedStrings.xml"]
            ?.let { parseSharedStrings(it) }
            ?: emptyList()

        val sheetBytes = zipEntries["xl/worksheets/sheet1.xml"]
            ?: zipEntries.entries.firstOrNull {
                it.key.startsWith("xl/worksheets/sheet") && it.key.endsWith(".xml")
            }?.value
            ?: error("لم يتم العثور على ورقة بيانات صالحة داخل ملف Excel")

        val rows = parseWorksheetXml(sheetBytes, sharedStrings)
        return mapRowsToWordEntries(rows)
    }

    private fun parseCsvRows(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < text.length) {
            val ch = text[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        cell.append('"')
                        i += 2
                        continue
                    } else {
                        inQuotes = false
                    }
                } else {
                    cell.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow.add(cell.toString())
                        cell.setLength(0)
                    }
                    '\r' -> {
                        if (i + 1 < text.length && text[i + 1] == '\n') i++
                        currentRow.add(cell.toString())
                        cell.setLength(0)
                        if (currentRow.any { it.isNotBlank() }) rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(cell.toString())
                        cell.setLength(0)
                        if (currentRow.any { it.isNotBlank() }) rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    else -> cell.append(ch)
                }
            }
            i++
        }

        if (cell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(cell.toString())
            if (currentRow.any { it.isNotBlank() }) rows.add(currentRow.toList())
        }
        return rows
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val list = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(InputStreamReader(ByteArrayInputStream(bytes), StandardCharsets.UTF_8))

        var inSi = false
        val sb = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "si") {
                        inSi = true
                        sb.setLength(0)
                    } else if (inSi && parser.name == "t") {
                        sb.append(parser.nextText())
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "si") {
                        list.add(sb.toString())
                        inSi = false
                    }
                }
            }
            event = parser.next()
        }
        return list
    }

    private fun parseWorksheetXml(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(InputStreamReader(ByteArrayInputStream(bytes), StandardCharsets.UTF_8))

        var currentRowCells = mutableMapOf<Int, String>()
        var currentColIdx = 0
        var currentCellType = ""
        var inCell = false
        val cellText = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRowCells = mutableMapOf()
                            currentColIdx = 0
                        }
                        "c" -> {
                            inCell = true
                            cellText.setLength(0)
                            val ref = parser.getAttributeValue(null, "r")
                            currentColIdx = colIndexFromRef(ref) ?: currentColIdx
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                        }
                        "t", "v" -> {
                            if (inCell) {
                                cellText.append(parser.nextText())
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "c" -> {
                            if (inCell) {
                                val resolved = when (currentCellType) {
                                    "s" -> {
                                        val idx = cellText.toString().trim().toIntOrNull()
                                        if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else ""
                                    }
                                    else -> cellText.toString()
                                }
                                currentRowCells[currentColIdx] = resolved
                                currentColIdx++
                                inCell = false
                            }
                        }
                        "row" -> {
                            if (currentRowCells.isNotEmpty()) {
                                val maxCol = currentRowCells.keys.maxOrNull() ?: 0
                                val rowList = (0..maxCol).map { idx -> currentRowCells[idx].orEmpty() }
                                if (rowList.any { it.isNotBlank() }) rows.add(rowList)
                            }
                        }
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }

    private fun colIndexFromRef(ref: String?): Int? {
        if (ref.isNullOrBlank()) return null
        val letters = ref.takeWhile { it.isLetter() }.uppercase()
        if (letters.isEmpty()) return null
        var index = 0
        for (ch in letters) {
            index = index * 26 + (ch - 'A' + 1)
        }
        return index - 1
    }

    private fun mapRowsToWordEntries(rows: List<List<String>>): List<WordEntry> {
        if (rows.isEmpty()) return emptyList()

        val firstRow = rows.first().map { it.trim() }
        val headerMap = detectColumns(firstRow)
        val hasHeader = headerMap.isNotEmpty()
        val dataRows = if (hasHeader) rows.drop(1) else rows

        return dataRows.mapNotNull { row ->
            fun col(key: String, fallbackIdx: Int? = null): String {
                val idx = headerMap[key] ?: fallbackIdx
                return if (idx != null && idx in row.indices) row[idx].trim() else ""
            }

            val word = col("word", if (!hasHeader) 0 else null)
            val ipa = col("ipa")
            val meaning = col("meaning", if (!hasHeader) 1 else null)
            val books = splitTags(col("books"))
            val locationsRaw = col("locations")
            val unitCol = col("unit")
            val lessonCol = col("lesson")
            val pageCol = col("page")
            val pos = splitTags(col("pos"))
            val synonyms = splitTags(col("synonyms"))
            val antonyms = splitTags(col("antonyms"))
            val definition = col("definition")
            val derivatives = col("derivatives")
            val expressions = col("expressions")
            val notes = col("notes")
            val importantNotes = col("importantNotes")
            val favRaw = col("favorite").lowercase()
            val isFavorite = favRaw == "نعم" || favRaw == "yes" || favRaw == "true" || favRaw == "1"

            val parsedLocations = buildList {
                if (locationsRaw.isNotBlank()) {
                    addAll(parseLocationsString(locationsRaw))
                }
                if (unitCol.isNotBlank() || lessonCol.isNotBlank() || pageCol.isNotBlank()) {
                    add(
                        WordLocation(
                            book = books.firstOrNull().orEmpty(),
                            unit = unitCol,
                            lesson = lessonCol,
                            page = pageCol
                        )
                    )
                }
            }.distinct()

            val entry = WordEntry(
                word = word,
                ipa = ipa,
                arabicMeaning = meaning,
                books = books,
                locations = parsedLocations,
                partsOfSpeech = pos,
                synonyms = synonyms,
                antonyms = antonyms,
                definition = definition,
                derivatives = derivatives,
                expressions = expressions,
                notes = notes,
                importantNotes = importantNotes,
                isFavorite = isFavorite
            )
            if (entry.hasAnyContent()) entry else null
        }
    }

    private fun detectColumns(headers: List<String>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        headers.forEachIndexed { idx, raw ->
            val h = raw.trim().lowercase()
            when {
                h == "word" || h == "الكلمة" || h.contains("english") -> map.putIfAbsent("word", idx)
                h.contains("ipa") || h.contains("النطق") -> map.putIfAbsent("ipa", idx)
                h.contains("معنى") || h.contains("المعنى") || h == "meaning" || h == "arabic" -> map.putIfAbsent("meaning", idx)
                h == "الكتاب" || h == "book" || h == "books" -> map.putIfAbsent("books", idx)
                h.contains("المواقع") || h == "locations" -> map.putIfAbsent("locations", idx)
                h == "الوحدة" || h == "unit" -> map.putIfAbsent("unit", idx)
                h == "الدرس" || h == "lesson" -> map.putIfAbsent("lesson", idx)
                h == "الصفحة" || h == "page" -> map.putIfAbsent("page", idx)
                h.contains("موقعها") || h.contains("الكلام") || h == "pos" || h.contains("part of speech") -> map.putIfAbsent("pos", idx)
                h.contains("مرادف") || h.contains("synonym") -> map.putIfAbsent("synonyms", idx)
                h.contains("مضاد") || h.contains("antonym") -> map.putIfAbsent("antonyms", idx)
                h.contains("تعريف") || h == "definition" -> map.putIfAbsent("definition", idx)
                h.contains("اشتقاق") || h.contains("derivative") -> map.putIfAbsent("derivatives", idx)
                h.contains("تعبيرات") || h.contains("expression") -> map.putIfAbsent("expressions", idx)
                h.contains("م&ملاحظات") || h.contains("هامة") || h.contains("important") -> map.putIfAbsent("importantNotes", idx)
                h.contains("ملاحظات") || h == "notes" -> map.putIfAbsent("notes", idx)
                h.contains("مفضلة") || h.contains("favorite") -> map.putIfAbsent("favorite", idx)
            }
        }
        return map
    }

    private fun splitTags(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split("،", ",", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun parseLocationsString(raw: String): List<WordLocation> {
        return raw.split("|").mapNotNull { segment ->
            val tokens = segment.split("•").map { it.trim() }.filter { it.isNotBlank() }
            if (tokens.isEmpty()) return@mapNotNull null

            var book = ""
            var unit = ""
            var lesson = ""
            var page = ""
            for (token in tokens) {
                when {
                    token.startsWith("وحدة ") -> unit = token.removePrefix("وحدة ").trim()
                    token.startsWith("درس ") -> lesson = token.removePrefix("درس ").trim()
                    token.startsWith("ص ") -> page = token.removePrefix("ص ").trim()
                    book.isEmpty() -> book = token
                }
            }
            val loc = WordLocation(book = book, unit = unit, lesson = lesson, page = page)
            if (loc.isNotBlank()) loc else null
        }
    }
}
