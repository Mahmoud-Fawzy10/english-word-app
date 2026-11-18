package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.WordTypeConverters
import com.example.data.model.WordEntry
import com.example.data.model.WordLocation
import com.example.domain.QuizConfig
import com.example.domain.QuizEngine
import com.example.domain.QuizQuestionType
import com.example.domain.QuizScopeMode
import com.example.domain.SrsGrade
import com.example.domain.SrsScheduler
import com.example.export.DictionaryExporter
import com.example.export.ExportFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("قاموسي", appName)
  }

  @Test
  fun `type converters round trip multiple locations and strings`() {
    val converters = WordTypeConverters()
    val originalLocations = listOf(
      WordLocation(book = "المعاصر", unit = "1", lesson = "1, 2", page = "24, 27"),
      WordLocation(book = "كتاب الوزارة", unit = "1", lesson = "3", page = "15")
    )
    val encoded = converters.fromLocationList(originalLocations)
    val decoded = converters.toLocationList(encoded)
    assertEquals(originalLocations, decoded)

    val books = listOf("المعاصر", "A1", "كتاب الوزارة")
    assertEquals(books, converters.toStringList(converters.fromStringList(books)))
  }

  @Test
  fun `srs scheduler progresses and resets intervals properly`() {
    val now = 1_700_000_000_000L
    val newWord = WordEntry(id = 1L, word = "resilient", arabicMeaning = "مرن")
    assertTrue(newWord.isDueForReview(now))

    // First GOOD review -> 1 day
    val firstGood = SrsScheduler.scheduleNext(newWord, SrsGrade.GOOD, now)
    assertEquals(1, firstGood.repetitions)
    assertEquals(1, firstGood.intervalDays)
    assertEquals(now + SrsScheduler.DAY_MILLIS, firstGood.nextReviewAt)

    val afterFirst = newWord.copy(
      srsRepetitions = firstGood.repetitions,
      srsIntervalDays = firstGood.intervalDays,
      srsEaseFactor = firstGood.easeFactor,
      srsNextReviewAt = firstGood.nextReviewAt
    )
    assertFalse(afterFirst.isDueForReview(now + 1000L))
    assertTrue(afterFirst.isDueForReview(now + SrsScheduler.DAY_MILLIS + 1L))

    // Second GOOD review -> 6 days
    val secondGood = SrsScheduler.scheduleNext(afterFirst, SrsGrade.GOOD, now)
    assertEquals(2, secondGood.repetitions)
    assertEquals(6, secondGood.intervalDays)

    // Third GOOD review -> scaled by ease factor (~15 days)
    val afterSecond = afterFirst.copy(
      srsRepetitions = secondGood.repetitions,
      srsIntervalDays = secondGood.intervalDays,
      srsEaseFactor = secondGood.easeFactor
    )
    val thirdGood = SrsScheduler.scheduleNext(afterSecond, SrsGrade.GOOD, now)
    assertEquals(3, thirdGood.repetitions)
    assertTrue(thirdGood.intervalDays >= 14)

    // AGAIN resets repetitions to 0 and interval to 1 day
    val forgot = SrsScheduler.scheduleNext(afterSecond, SrsGrade.AGAIN, now)
    assertEquals(0, forgot.repetitions)
    assertEquals(1, forgot.intervalDays)
    assertTrue(forgot.easeFactor < afterSecond.srsEaseFactor)
  }

  @Test
  fun `quiz engine filters by unit lesson page and random range`() {
    val words = listOf(
      WordEntry(
        id = 1L,
        word = "innovate",
        arabicMeaning = "يبتكر",
        books = listOf("المعاصر"),
        locations = listOf(WordLocation("المعاصر", "1", "1, 2", "12"))
      ),
      WordEntry(
        id = 2L,
        word = "heritage",
        arabicMeaning = "تراث",
        books = listOf("A1"),
        locations = listOf(WordLocation("A1", "2", "3", "45"))
      ),
      WordEntry(
        id = 3L,
        word = "inspire",
        arabicMeaning = "يلهم",
        books = listOf("كتاب الوزارة"),
        locations = listOf(WordLocation("كتاب الوزارة", "4", "5", "92"))
      )
    )

    val byUnit = QuizEngine.filterWordsForConfig(
      words,
      QuizConfig(scopeMode = QuizScopeMode.BY_UNIT, unitFrom = "1", unitTo = "2")
    )
    assertEquals(setOf(1L, 2L), byUnit.map { it.id }.toSet())

    val byLesson = QuizEngine.filterWordsForConfig(
      words,
      QuizConfig(scopeMode = QuizScopeMode.BY_LESSON, lessonFrom = "2", lessonTo = "3")
    )
    assertEquals(setOf(1L, 2L), byLesson.map { it.id }.toSet())

    val byPageRange = QuizEngine.filterWordsForConfig(
      words,
      QuizConfig(scopeMode = QuizScopeMode.BY_PAGE, pageFrom = "40", pageTo = "100")
    )
    assertEquals(setOf(2L, 3L), byPageRange.map { it.id }.toSet())

    val randomScoped = QuizEngine.filterWordsForConfig(
      words,
      QuizConfig(
        scopeMode = QuizScopeMode.RANDOM_RANGE,
        selectedBook = "A1",
        unitFrom = "1",
        unitTo = "5",
        questionLimit = 1
      )
    )
    assertEquals(listOf(2L), randomScoped.map { it.id })

    val mcq = QuizEngine.buildQuizQuestions(
      selectedWords = listOf(words[0]),
      allWordsPool = words,
      config = QuizConfig(questionType = QuizQuestionType.MULTIPLE_CHOICE)
    )
    assertEquals(3, mcq.first().options.size)
    assertTrue(mcq.first().options.contains("يبتكر"))
  }

  @Test
  fun `offline xlsx and google sheets csv exporters preserve arabic and english`() {
    val sample = listOf(
      WordEntry(
        id = 1L,
        word = "sustainable",
        arabicMeaning = "مستدام",
        books = listOf("المعاصر", "A1"),
        locations = listOf(WordLocation("المعاصر", "2", "1, 2", "45, 48")),
        partsOfSpeech = listOf("صفة (Adj)"),
        synonyms = listOf("renewable", "eco-friendly"),
        antonyms = listOf("unsustainable"),
        definition = "Able to continue over time",
        derivatives = "sustain, sustainability",
        expressions = "sustainable development",
        notes = "تكررت في امتحان 2024",
        importantNotes = "فرق بينها وبين continuous"
      )
    )

    val xlsxOut = ByteArrayOutputStream()
    DictionaryExporter.exportToStream(sample, ExportFormat.EXCEL, xlsxOut)
    val entries = mutableMapOf<String, String>()
    ZipInputStream(ByteArrayInputStream(xlsxOut.toByteArray())).use { zis ->
      var ze = zis.nextEntry
      while (ze != null) {
        entries[ze.name] = zis.readBytes().toString(Charsets.UTF_8)
        ze = zis.nextEntry
      }
    }
    assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
    val sheetXml = entries.getValue("xl/worksheets/sheet1.xml")
    assertTrue(sheetXml.contains("rightToLeft=\"1\""))
    assertTrue(sheetXml.contains("sustainable"))
    assertTrue(sheetXml.contains("مستدام"))

    val csvOut = ByteArrayOutputStream()
    DictionaryExporter.exportToStream(sample, ExportFormat.GOOGLE_SHEETS, csvOut)
    val csvBytes = csvOut.toByteArray()
    assertEquals(0xEF.toByte(), csvBytes[0])
    assertEquals(0xBB.toByte(), csvBytes[1])
    assertEquals(0xBF.toByte(), csvBytes[2])
    val csvText = String(csvBytes, 3, csvBytes.size - 3, Charsets.UTF_8)
    assertTrue(csvText.contains("sustainable"))
    assertTrue(csvText.contains("مستدام"))
  }
}
