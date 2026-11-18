package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.QamusiDatabase
import com.example.data.local.WordTypeConverters
import com.example.data.model.DuplicatePolicy
import com.example.data.model.WordEntry
import com.example.data.model.WordLocation
import com.example.data.repository.WordRepository
import com.example.domain.QuestionInteractionKind
import com.example.domain.QuizConfig
import com.example.domain.QuizEngine
import com.example.domain.QuizQuestionType
import com.example.domain.QuizScopeMode
import com.example.domain.SrsGrade
import com.example.domain.SrsMaturity
import com.example.domain.SrsScheduler
import com.example.export.DictionaryExporter
import com.example.export.DictionaryImporter
import com.example.export.ExportFormat
import com.example.export.JsonBackupManager
import com.example.ui.components.buildCurriculumGroups
import com.example.ui.components.buildPosGroups
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context and verify strictly offline manifest`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("قاموسي", appName)

        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requestedPerms = pkgInfo.requestedPermissions?.toList().orEmpty()
        assertFalse("Offline app must not request INTERNET", requestedPerms.contains(Manifest.permission.INTERNET))
        assertFalse("Offline app must not request ACCESS_NETWORK_STATE", requestedPerms.contains(Manifest.permission.ACCESS_NETWORK_STATE))
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
    fun `room crud persists across database close and reopen`() {
        runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbFile = File(context.filesDir, "test_persist_${System.nanoTime()}.db")
        if (dbFile.exists()) dbFile.delete()

        val db1 = Room.databaseBuilder(context, QamusiDatabase::class.java, dbFile.name)
            .allowMainThreadQueries()
            .build()
        val repo1 = WordRepository(db1.wordDao())
        val savedId = repo1.saveWord(
            WordEntry(
                word = "resilient",
                ipa = "/rɪˈzɪliənt/",
                arabicMeaning = "مَرِن وقادر على التكيف",
                books = listOf("المعاصر"),
                locations = listOf(WordLocation("المعاصر", "3", "2", "78")),
                partsOfSpeech = listOf("صفة (Adj)"),
                synonyms = listOf("adaptable", "flexible"),
                antonyms = listOf("fragile")
            )
        )
        assertTrue(savedId > 0L)
        db1.close()

        // Re-open database simulating app restart
        val db2 = Room.databaseBuilder(context, QamusiDatabase::class.java, dbFile.name)
            .allowMainThreadQueries()
            .build()
        val repo2 = WordRepository(db2.wordDao())
        val loaded = repo2.getWordOnce(savedId)
        assertNotNull(loaded)
        assertEquals("resilient", loaded!!.word)
        assertEquals("/rɪˈzɪliənt/", loaded.ipa)
        assertEquals("مَرِن وقادر على التكيف", loaded.arabicMeaning)
        assertEquals(listOf("adaptable", "flexible"), loaded.synonyms)

        // Update and delete check
        repo2.saveWord(loaded.copy(notes = "ملاحظة مهمة"))
        val updated = repo2.getWordOnce(savedId)!!
        assertEquals("ملاحظة مهمة", updated.notes)

        repo2.deleteWord(savedId)
        assertTrue(repo2.getAllWordsOnce().isEmpty())
        db2.close()
        dbFile.delete()
        }
    }

    @Test
    fun `room migrations 1 to 3 preserve existing rows cleanly`() {
        runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migrate_${System.nanoTime()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        if (dbFile.exists()) dbFile.delete()

        // Manually construct schema version 1 database with a pre-existing row
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS word_entries (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            word TEXT NOT NULL,
                            arabicMeaning TEXT NOT NULL,
                            books TEXT NOT NULL,
                            locations TEXT NOT NULL,
                            partsOfSpeech TEXT NOT NULL,
                            synonyms TEXT NOT NULL,
                            antonyms TEXT NOT NULL,
                            definition TEXT NOT NULL,
                            derivatives TEXT NOT NULL,
                            expressions TEXT NOT NULL,
                            notes TEXT NOT NULL,
                            importantNotes TEXT NOT NULL,
                            isFavorite INTEGER NOT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO word_entries (
                            id, word, arabicMeaning, books, locations, partsOfSpeech,
                            synonyms, antonyms, definition, derivatives, expressions,
                            notes, importantNotes, isFavorite, createdAt, updatedAt
                        ) VALUES (
                            10, 'legacyWord', 'كلمة قديمة', '["المعاصر"]', '[]', '[]',
                            '[]', '[]', '', '', '', 'note', '', 1, 1000, 1000
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val rawHelper = FrameworkSQLiteOpenHelperFactory().create(config)
        rawHelper.writableDatabase.close()
        rawHelper.close()

        // Open with QamusiDatabase v3 + migrations
        val migratedDb = Room.databaseBuilder(context, QamusiDatabase::class.java, dbName)
            .addMigrations(QamusiDatabase.MIGRATION_1_2, QamusiDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()
        val rows = migratedDb.wordDao().getAllWordsOnce()
        assertEquals(1, rows.size)
        assertEquals("legacyWord", rows.first().word)
        assertEquals("كلمة قديمة", rows.first().arabicMeaning)
        assertEquals("", rows.first().ipa)
        assertEquals(2.5f, rows.first().srsEaseFactor)
        assertTrue(rows.first().isFavorite)
        migratedDb.close()
        dbFile.delete()
        }
    }

    @Test
    fun `duplicate detection arabic normalization and merge work accurately`() {
        runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, QamusiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = WordRepository(db.wordDao())

        val id1 = repo.saveWord(
            WordEntry(
                word = "  Sustainable ",
                arabicMeaning = "مُستدامة",
                books = listOf("المعاصر"),
                locations = listOf(WordLocation("المعاصر", "1", "1", "10")),
                synonyms = listOf("renewable")
            )
        )
        repo.saveWord(
            WordEntry(
                word = "sustainable",
                ipa = "/səˈsteɪnəbl/",
                arabicMeaning = "صديق للبيئة",
                books = listOf("A1"),
                locations = listOf(WordLocation("A1", "2", "3", "40")),
                synonyms = listOf("eco-friendly")
            )
        )

        // Normalize Arabic alef/teh marbuta/tashkeel check
        assertEquals("اداره", WordRepository.normalizeKey("إدارةٌ"))

        val dupes = repo.findDuplicatesFor("SUSTAINABLE", "", excludeId = 0L)
        assertEquals(2, dupes.size)

        val groups = repo.findDuplicateGroups(repo.getAllWordsOnce())
        assertEquals(1, groups.size)

        val merged = repo.mergeDuplicateGroup(groups.first())!!
        assertEquals(id1, merged.id)
        assertEquals("/səˈsteɪnəbl/", merged.ipa)
        assertEquals(setOf("المعاصر", "A1"), merged.books.toSet())
        assertEquals(setOf("renewable", "eco-friendly"), merged.synonyms.toSet())
        assertEquals(2, merged.locations.size)
        assertEquals(1, repo.getAllWordsOnce().size)

        db.close()
        }
    }

    @Test
    fun `srs scheduler progresses resets and supports manual schedule adjustment`() {
        runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, QamusiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = WordRepository(db.wordDao())

        val now = 1_700_000_000_000L
        val newWord = WordEntry(id = 1L, word = "resilient", arabicMeaning = "مرن")
        assertEquals(SrsMaturity.NEW, SrsScheduler.maturityOf(newWord))
        assertTrue(newWord.isDueForReview(now))

        val firstGood = SrsScheduler.scheduleNext(newWord, SrsGrade.GOOD, now)
        assertEquals(1, firstGood.repetitions)
        assertEquals(1, firstGood.intervalDays)
        assertEquals(now + SrsScheduler.DAY_MILLIS, firstGood.nextReviewAt)

        val afterFirst = newWord.copy(
            srsRepetitions = firstGood.repetitions,
            srsIntervalDays = firstGood.intervalDays,
            srsEaseFactor = firstGood.easeFactor,
            srsNextReviewAt = firstGood.nextReviewAt,
            srsTotalReviews = 1
        )
        assertEquals(SrsMaturity.LEARNING, SrsScheduler.maturityOf(afterFirst))
        assertFalse(afterFirst.isDueForReview(now + 1000L))
        assertTrue(afterFirst.isDueForReview(now + SrsScheduler.DAY_MILLIS + 1L))

        val secondGood = SrsScheduler.scheduleNext(afterFirst, SrsGrade.GOOD, now)
        assertEquals(2, secondGood.repetitions)
        assertEquals(6, secondGood.intervalDays)

        val afterSecond = afterFirst.copy(
            srsRepetitions = secondGood.repetitions,
            srsIntervalDays = secondGood.intervalDays,
            srsEaseFactor = secondGood.easeFactor
        )
        assertEquals(SrsMaturity.REVIEWING, SrsScheduler.maturityOf(afterSecond))

        val thirdGood = SrsScheduler.scheduleNext(afterSecond, SrsGrade.GOOD, now)
        assertEquals(3, thirdGood.repetitions)
        assertTrue(thirdGood.intervalDays >= 14)
        assertEquals(
            SrsMaturity.MASTERED,
            SrsScheduler.maturityOf(
                afterSecond.copy(
                    srsIntervalDays = thirdGood.intervalDays,
                    srsRepetitions = thirdGood.repetitions
                )
            )
        )

        val forgot = SrsScheduler.scheduleNext(afterSecond, SrsGrade.AGAIN, now)
        assertEquals(0, forgot.repetitions)
        assertEquals(1, forgot.intervalDays)
        assertTrue(forgot.easeFactor < afterSecond.srsEaseFactor)

        // Manual schedule update check in Room
        val id = repo.saveWord(WordEntry(word = "eloquent", arabicMeaning = "فصيح"))
        val saved = repo.getWordOnce(id)!!
        repo.updateManualSrsSchedule(saved, intervalDays = 14, easeFactor = 2.6f, resetProgress = false, nowMillis = now)
        val adjusted = repo.getWordOnce(id)!!
        assertEquals(14, adjusted.srsIntervalDays)
        assertEquals(now + 14 * SrsScheduler.DAY_MILLIS, adjusted.srsNextReviewAt)
        assertFalse(adjusted.isDueForReview(now + 1000L))

        db.close()
        }
    }

    @Test
    fun `quiz engine builds synonyms antonyms typing mixed and curriculum scopes`() {
        val words = listOf(
            WordEntry(
                id = 1L,
                word = "innovate",
                ipa = "/ˈɪnəveɪt/",
                arabicMeaning = "يبتكر",
                books = listOf("المعاصر"),
                locations = listOf(WordLocation("المعاصر", "1", "1, 2", "12")),
                partsOfSpeech = listOf("فعل (Verb)"),
                synonyms = listOf("invent", "create"),
                antonyms = listOf("imitate")
            ),
            WordEntry(
                id = 2L,
                word = "heritage",
                arabicMeaning = "تراث",
                books = listOf("A1"),
                locations = listOf(WordLocation("A1", "2", "3", "45")),
                partsOfSpeech = listOf("اسم (Noun)"),
                synonyms = listOf("legacy"),
                antonyms = listOf("innovation")
            ),
            WordEntry(
                id = 3L,
                word = "inspire",
                arabicMeaning = "يلهم",
                books = listOf("كتاب الوزارة"),
                locations = listOf(WordLocation("كتاب الوزارة", "4", "5", "92")),
                partsOfSpeech = listOf("فعل (Verb)"),
                synonyms = listOf("motivate"),
                antonyms = listOf("discourage")
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

        val synQuiz = QuizEngine.buildQuizQuestions(
            selectedWords = listOf(words[0]),
            allWordsPool = words,
            config = QuizConfig(questionType = QuizQuestionType.SYNONYMS)
        )
        assertEquals(QuestionInteractionKind.MCQ, synQuiz.first().interactionKind)
        assertTrue(synQuiz.first().options.contains("invent"))
        assertEquals(listOf("invent", "create"), synQuiz.first().acceptedAnswers)

        val antQuiz = QuizEngine.buildQuizQuestions(
            selectedWords = listOf(words[0]),
            allWordsPool = words,
            config = QuizConfig(questionType = QuizQuestionType.ANTONYMS)
        )
        assertEquals(QuestionInteractionKind.MCQ, antQuiz.first().interactionKind)
        assertTrue(antQuiz.first().options.contains("imitate"))

        val typingQuiz = QuizEngine.buildQuizQuestions(
            selectedWords = listOf(words[0]),
            allWordsPool = words,
            config = QuizConfig(questionType = QuizQuestionType.TYPING)
        )
        assertEquals(QuestionInteractionKind.TYPING, typingQuiz.first().interactionKind)
        assertEquals("يبتكر", typingQuiz.first().answerText)

        val mixedQuiz = QuizEngine.buildQuizQuestions(
            selectedWords = words,
            allWordsPool = words,
            config = QuizConfig(questionType = QuizQuestionType.MIXED)
        )
        assertEquals(3, mixedQuiz.size)
        assertTrue(mixedQuiz.map { it.interactionKind }.toSet().containsAll(listOf(QuestionInteractionKind.MCQ, QuestionInteractionKind.TYPING)))

        // Browse grouping helpers verification
        val posGroups = buildPosGroups(words)
        assertEquals("فعل (Verb)", posGroups.first().first)
        assertEquals(2, posGroups.first().second.size)

        val curriculumGroups = buildCurriculumGroups(words)
        assertEquals(3, curriculumGroups.size)
        assertTrue(curriculumGroups.first().unitTitle.contains("الوحدة 1"))
    }

    @Test
    fun `offline xlsx and csv export round trips through DictionaryImporter`() {
        val sample = listOf(
            WordEntry(
                id = 1L,
                word = "sustainable",
                ipa = "/səˈsteɪnəbl/",
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
                importantNotes = "فرق بينها وبين continuous",
                isFavorite = true
            )
        )

        // XLSX Export + Re-import
        val xlsxOut = ByteArrayOutputStream()
        DictionaryExporter.exportToStream(sample, ExportFormat.EXCEL, xlsxOut)
        val xlsxBytes = xlsxOut.toByteArray()
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(xlsxBytes)).use { zis ->
            var ze = zis.nextEntry
            while (ze != null) {
                entries[ze.name] = zis.readBytes().toString(Charsets.UTF_8)
                ze = zis.nextEntry
            }
        }
        assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
        assertTrue(entries.getValue("xl/worksheets/sheet1.xml").contains("rightToLeft=\"1\""))

        val parsedFromXlsx = DictionaryImporter.parseXlsx(ByteArrayInputStream(xlsxBytes))
        assertEquals(1, parsedFromXlsx.size)
        assertEquals("sustainable", parsedFromXlsx.first().word)
        assertEquals("/səˈsteɪnəbl/", parsedFromXlsx.first().ipa)
        assertEquals("مستدام", parsedFromXlsx.first().arabicMeaning)
        assertEquals(listOf("المعاصر", "A1"), parsedFromXlsx.first().books)
        assertEquals(listOf("renewable", "eco-friendly"), parsedFromXlsx.first().synonyms)
        assertTrue(parsedFromXlsx.first().isFavorite)

        // CSV Export + Re-import
        val csvOut = ByteArrayOutputStream()
        DictionaryExporter.exportToStream(sample, ExportFormat.GOOGLE_SHEETS, csvOut)
        val csvBytes = csvOut.toByteArray()
        assertEquals(0xEF.toByte(), csvBytes[0])
        assertEquals(0xBB.toByte(), csvBytes[1])
        assertEquals(0xBF.toByte(), csvBytes[2])

        val parsedFromCsv = DictionaryImporter.parseCsv(ByteArrayInputStream(csvBytes))
        assertEquals(1, parsedFromCsv.size)
        assertEquals("sustainable", parsedFromCsv.first().word)
        assertEquals("/səˈsteɪnəbl/", parsedFromCsv.first().ipa)
        assertEquals("مستدام", parsedFromCsv.first().arabicMeaning)
        assertEquals(1, parsedFromCsv.first().locations.size)
        assertEquals("2", parsedFromCsv.first().locations.first().unit)
    }

    @Test
    fun `json backup and restore preserves full srs progress and locations`() {
        runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, QamusiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repo = WordRepository(db.wordDao())

        val original = listOf(
            WordEntry(
                word = "perseverance",
                ipa = "/ˌpɜːrsəˈvɪrəns/",
                arabicMeaning = "مثابرة",
                books = listOf("كتاب الوزارة"),
                locations = listOf(WordLocation("كتاب الوزارة", "5", "2", "110")),
                partsOfSpeech = listOf("اسم (Noun)"),
                synonyms = listOf("persistence"),
                antonyms = listOf("laziness"),
                srsRepetitions = 3,
                srsIntervalDays = 15,
                srsEaseFactor = 2.65f,
                srsNextReviewAt = 1_800_000_000_000L,
                srsTotalReviews = 4,
                srsCorrectCount = 4
            )
        )

        val jsonOut = ByteArrayOutputStream()
        JsonBackupManager.exportBackup(original, jsonOut)
        val parsed = JsonBackupManager.parseBackup(ByteArrayInputStream(jsonOut.toByteArray()))
        assertEquals(1, parsed.size)

        // Insert a dummy word first, then test replaceExisting = true
        repo.saveWord(WordEntry(word = "temporary", arabicMeaning = "مؤقت"))
        val summary = repo.restoreFullJsonBackup(parsed, replaceExisting = true)
        assertEquals(1, summary.insertedCount)

        val all = repo.getAllWordsOnce()
        assertEquals(1, all.size)
        val restored = all.first()
        assertEquals("perseverance", restored.word)
        assertEquals("/ˌpɜːrsəˈvɪrəns/", restored.ipa)
        assertEquals(3, restored.srsRepetitions)
        assertEquals(15, restored.srsIntervalDays)
        assertEquals(1_800_000_000_000L, restored.srsNextReviewAt)
        assertEquals(4, restored.srsCorrectCount)

        // Also test importWords with SKIP policy
        val skipSummary = repo.importWords(
            listOf(WordEntry(word = "perseverance", arabicMeaning = "إصرار"), WordEntry(word = "vital", arabicMeaning = "حيوي")),
            DuplicatePolicy.SKIP
        )
        assertEquals(1, skipSummary.insertedCount)
        assertEquals(1, skipSummary.skippedCount)
        assertEquals(2, repo.getAllWordsOnce().size)

        db.close()
        }
    }
}
