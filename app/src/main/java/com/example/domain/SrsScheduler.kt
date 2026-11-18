package com.example.domain

import com.example.data.model.WordEntry
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class SrsGrade(val labelAr: String, val subtitleAr: String) {
    AGAIN("نسيتها", "إعادة قريباً"),
    HARD("صعبة", "تذكرتها بصعوبة"),
    GOOD("جيدة", "تذكر سليم"),
    EASY("سهلة", "متقنة تماماً")
}

enum class SrsMaturity(val labelAr: String) {
    NEW("جديدة"),
    LEARNING("قيد التعلم"),
    REVIEWING("قيد التثبيت"),
    MASTERED("متقنة")
}

data class SrsScheduleResult(
    val repetitions: Int,
    val intervalDays: Int,
    val easeFactor: Float,
    val nextReviewAt: Long
)

object SrsScheduler {
    const val DAY_MILLIS: Long = 24L * 60L * 60L * 1000L

    fun maturityOf(entry: WordEntry): SrsMaturity = when {
        entry.isNewForSrs() -> SrsMaturity.NEW
        entry.srsIntervalDays >= 14 && entry.srsRepetitions >= 3 -> SrsMaturity.MASTERED
        entry.srsIntervalDays >= 3 -> SrsMaturity.REVIEWING
        else -> SrsMaturity.LEARNING
    }

    fun scheduleNext(
        entry: WordEntry,
        grade: SrsGrade,
        nowMillis: Long = System.currentTimeMillis()
    ): SrsScheduleResult {
        val currentEase = entry.srsEaseFactor.coerceIn(1.3f, 3.2f)
        val currentReps = entry.srsRepetitions.coerceAtLeast(0)
        val currentInterval = entry.srsIntervalDays.coerceAtLeast(0)

        return when (grade) {
            SrsGrade.AGAIN -> {
                val newEase = max(1.3f, currentEase - 0.20f)
                SrsScheduleResult(
                    repetitions = 0,
                    intervalDays = 1,
                    easeFactor = newEase,
                    nextReviewAt = nowMillis + DAY_MILLIS
                )
            }

            SrsGrade.HARD -> {
                val newEase = max(1.3f, currentEase - 0.15f)
                val nextInterval = when {
                    currentReps == 0 -> 1
                    currentInterval <= 1 -> 2
                    else -> max(currentInterval + 1, (currentInterval * 1.2f).roundToInt())
                }
                SrsScheduleResult(
                    repetitions = currentReps + 1,
                    intervalDays = nextInterval,
                    easeFactor = newEase,
                    nextReviewAt = nowMillis + nextInterval * DAY_MILLIS
                )
            }

            SrsGrade.GOOD -> {
                val nextInterval = when (currentReps) {
                    0 -> 1
                    1 -> 6
                    else -> max(currentInterval + 1, (currentInterval * currentEase).roundToInt())
                }
                SrsScheduleResult(
                    repetitions = currentReps + 1,
                    intervalDays = nextInterval,
                    easeFactor = currentEase,
                    nextReviewAt = nowMillis + nextInterval * DAY_MILLIS
                )
            }

            SrsGrade.EASY -> {
                val newEase = min(3.2f, currentEase + 0.15f)
                val nextInterval = when (currentReps) {
                    0 -> 4
                    1 -> 8
                    else -> max(currentInterval + 2, (currentInterval * newEase * 1.3f).roundToInt())
                }
                SrsScheduleResult(
                    repetitions = currentReps + 1,
                    intervalDays = nextInterval,
                    easeFactor = newEase,
                    nextReviewAt = nowMillis + nextInterval * DAY_MILLIS
                )
            }
        }
    }

    fun formatDaysAr(days: Int): String = when {
        days <= 0 -> "الآن"
        days == 1 -> "غداً (1 يوم)"
        days == 2 -> "يومان"
        days in 3..10 -> "$days أيام"
        else -> "$days يوم"
    }

    fun statusLabelAr(entry: WordEntry, nowMillis: Long = System.currentTimeMillis()): String {
        if (entry.isNewForSrs()) return "جديدة للمراجعة"
        if (entry.isDueForReview(nowMillis)) return "مستحقة للمراجعة الآن"
        val remainingMillis = entry.srsNextReviewAt - nowMillis
        val remainingDays = ((remainingMillis + DAY_MILLIS - 1L) / DAY_MILLIS).toInt().coerceAtLeast(1)
        return "المراجعة بعد ${formatDaysAr(remainingDays)}"
    }
}
