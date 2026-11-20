package com.example.domain

import com.example.data.local.DailyStudyStats
import com.example.data.model.WordEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class StudyMapLessonNode(
    val lessonName: String,
    val totalCount: Int,
    val masteredCount: Int,
    val dueCount: Int
)

data class StudyMapUnitNode(
    val bookName: String,
    val unitName: String,
    val totalCount: Int,
    val masteredCount: Int,
    val dueCount: Int,
    val hardCount: Int,
    val lessons: List<StudyMapLessonNode>
) {
    val masteryPercent: Int
        get() = if (totalCount > 0) (masteredCount * 100) / totalCount else 0
}

data class StudyMapBookNode(
    val bookName: String,
    val totalCount: Int,
    val masteredCount: Int,
    val dueCount: Int,
    val units: List<StudyMapUnitNode>
) {
    val masteryPercent: Int
        get() = if (totalCount > 0) (masteredCount * 100) / totalCount else 0
}

data class DailyChallengeMission(
    val id: String,
    val title: String,
    val subtitle: String,
    val current: Int,
    val target: Int,
    val isCompleted: Boolean,
    val actionLabel: String
)

data class StudyGroupSummary(
    val tagName: String,
    val wordCount: Int,
    val dueCount: Int,
    val masteredCount: Int
)

object StudyMapBuilder {

    fun pickWordOfTheDay(allWords: List<WordEntry>, nowMillis: Long = System.currentTimeMillis()): WordEntry? {
        val candidates = allWords.filter { it.word.isNotBlank() || it.arabicMeaning.isNotBlank() }
        if (candidates.isEmpty()) return null
        val sorted = candidates.sortedBy { it.id }
        val dayKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(nowMillis))
        val seed = abs(dayKey.hashCode())
        return sorted[seed % sorted.size]
    }

    fun buildCurriculumMap(
        allWords: List<WordEntry>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<StudyMapBookNode> {
        data class LocRef(val book: String, val unit: String, val lesson: String, val word: WordEntry)

        val refs = mutableListOf<LocRef>()
        allWords.forEach { w ->
            val locs = w.locations.filter { it.isNotBlank() }
            if (locs.isEmpty()) {
                val b = w.books.firstOrNull().orEmpty().ifBlank { "كلمات عامة" }
                refs.add(LocRef(b, "بدون وحدة", "عام", w))
            } else {
                locs.forEach { loc ->
                    val b = loc.book.ifBlank { w.books.firstOrNull().orEmpty() }.ifBlank { "كلمات عامة" }
                    val u = loc.unit.ifBlank { "بدون وحدة" }
                    val l = loc.lesson.ifBlank { "عام" }
                    refs.add(LocRef(b, u, l, w))
                }
            }
        }

        return refs.groupBy { it.book }.entries.map { (bookName, bookRefs) ->
            val uniqueBookWords = bookRefs.map { it.word }.distinctBy { it.id }
            val unitNodes = bookRefs.groupBy { it.unit }.entries.map { (unitName, unitRefs) ->
                val uniqueUnitWords = unitRefs.map { it.word }.distinctBy { it.id }
                val lessonNodes = unitRefs.groupBy { it.lesson }.entries.map { (lessonName, lessonRefs) ->
                    val uniqueLessonWords = lessonRefs.map { it.word }.distinctBy { it.id }
                    StudyMapLessonNode(
                        lessonName = lessonName,
                        totalCount = uniqueLessonWords.size,
                        masteredCount = uniqueLessonWords.count { SrsScheduler.maturityOf(it) == SrsMaturity.MASTERED },
                        dueCount = uniqueLessonWords.count { it.isDueForReview(nowMillis) }
                    )
                }.sortedBy { firstIntOrMax(it.lessonName) }

                StudyMapUnitNode(
                    bookName = bookName,
                    unitName = unitName,
                    totalCount = uniqueUnitWords.size,
                    masteredCount = uniqueUnitWords.count { SrsScheduler.maturityOf(it) == SrsMaturity.MASTERED },
                    dueCount = uniqueUnitWords.count { it.isDueForReview(nowMillis) },
                    hardCount = uniqueUnitWords.count { it.isHardOrMistake() },
                    lessons = lessonNodes
                )
            }.sortedBy { firstIntOrMax(it.unitName) }

            StudyMapBookNode(
                bookName = bookName,
                totalCount = uniqueBookWords.size,
                masteredCount = uniqueBookWords.count { SrsScheduler.maturityOf(it) == SrsMaturity.MASTERED },
                dueCount = uniqueBookWords.count { it.isDueForReview(nowMillis) },
                units = unitNodes
            )
        }.sortedByDescending { it.totalCount }
    }

    fun buildStudyGroups(
        allWords: List<WordEntry>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<StudyGroupSummary> {
        val map = linkedMapOf<String, MutableList<WordEntry>>()
        allWords.forEach { w ->
            w.studyTags.forEach { tag ->
                val clean = tag.trim()
                if (clean.isNotEmpty()) {
                    map.getOrPut(clean) { mutableListOf() }.add(w)
                }
            }
        }
        return map.entries.map { (tag, list) ->
            val unique = list.distinctBy { it.id }
            StudyGroupSummary(
                tagName = tag,
                wordCount = unique.size,
                dueCount = unique.count { it.isDueForReview(nowMillis) },
                masteredCount = unique.count { SrsScheduler.maturityOf(it) == SrsMaturity.MASTERED }
            )
        }.sortedByDescending { it.wordCount }
    }

    fun buildDailyChallenges(
        allWords: List<WordEntry>,
        dailyStats: DailyStudyStats,
        nowMillis: Long = System.currentTimeMillis()
    ): List<DailyChallengeMission> {
        val goal = dailyStats.dailyGoalTarget.coerceAtLeast(5)
        val dueCount = allWords.count { it.isDueForReview(nowMillis) }
        val hardCount = allWords.count { it.isHardOrMistake() }

        val mission1 = DailyChallengeMission(
            id = "daily_goal",
            title = "هدف المراجعة اليومي ($goal كلمة)",
            subtitle = "راجع كلماتك بالبطاقات أو الاختبارات للوصول للهدف",
            current = dailyStats.reviewsToday.coerceAtMost(goal),
            target = goal,
            isCompleted = dailyStats.reviewsToday >= goal,
            actionLabel = if (dueCount > 0) "راجع المستحق ($dueCount)" else "تدرب الآن"
        )

        val mission2 = DailyChallengeMission(
            id = "mini_quiz",
            title = "تحدي الميني-كويز اليومي",
            subtitle = "أنهِ جولة توصيل أزواج أو ترتيب حروف واحدة على الأقل",
            current = dailyStats.miniQuizzesToday.coerceAtMost(1),
            target = 1,
            isCompleted = dailyStats.miniQuizzesToday >= 1,
            actionLabel = "ابدأ ميني-كويز"
        )

        val mission3 = DailyChallengeMission(
            id = "hard_vault",
            title = "تصفية الكلمات الصعبة والأخطاء",
            subtitle = if (hardCount > 0) "لديك $hardCount كلمة صعبة أو بها أخطاء سابقة" else "ممتاز! لا توجد أخطاء متراكمة الآن",
            current = if (hardCount == 0 && allWords.isNotEmpty()) 1 else 0,
            target = 1,
            isCompleted = hardCount == 0 && allWords.isNotEmpty(),
            actionLabel = if (hardCount > 0) "صفّي الصعبة ($hardCount)" else "أضف كلمات"
        )

        return listOf(mission1, mission2, mission3)
    }

    /**
     * Parses bulk lines like:
     *   sustainable : مستدام
     *   resilient - مرن
     *   perseverance = مثابرة
     *   eloquent
     */
    fun parseBulkQuickLines(
        rawText: String,
        defaultBook: String?,
        defaultTag: String?
    ): List<WordEntry> {
        val books = listOfNotNull(defaultBook?.trim()?.takeIf { it.isNotBlank() })
        val tags = listOfNotNull(defaultTag?.trim()?.takeIf { it.isNotBlank() })
        return rawText.lines().mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            val separators = listOf(":", " - ", "=", "—", "|", "\t")
            val foundSep = separators.firstOrNull { trimmed.contains(it) }
            val (wordPart, meaningPart) = if (foundSep != null) {
                val idx = trimmed.indexOf(foundSep)
                trimmed.substring(0, idx).trim() to trimmed.substring(idx + foundSep.length).trim()
            } else {
                trimmed to ""
            }
            if (wordPart.isBlank() && meaningPart.isBlank()) {
                null
            } else {
                WordEntry(
                    word = wordPart,
                    arabicMeaning = meaningPart,
                    books = books,
                    studyTags = tags
                )
            }
        }
    }

    private fun firstIntOrMax(raw: String): Int =
        Regex("\\d+").find(raw)?.value?.toIntOrNull() ?: Int.MAX_VALUE
}
