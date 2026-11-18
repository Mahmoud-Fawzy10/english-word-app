package com.example.data.local

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ThemeMode(val labelAr: String) {
    SYSTEM("تلقائي (حسب النظام)"),
    LIGHT("فاتح"),
    DARK("داكن")
}

data class DailyStudyStats(
    val reviewsToday: Int = 0,
    val currentStreakDays: Int = 0,
    val lastStudyDateKey: String = ""
)

class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("qamusi_prefs", Context.MODE_PRIVATE)

    fun getThemeMode(): ThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getDailyStudyStats(nowMillis: Long = System.currentTimeMillis()): DailyStudyStats {
        val todayKey = dateKey(nowMillis)
        val yesterdayKey = dateKey(nowMillis - 24L * 60L * 60L * 1000L)
        val savedDate = prefs.getString(KEY_LAST_STUDY_DATE, "") ?: ""
        val savedReviewsToday = if (savedDate == todayKey) prefs.getInt(KEY_REVIEWS_TODAY, 0) else 0
        val savedStreak = prefs.getInt(KEY_STREAK_DAYS, 0)
        val activeStreak = when (savedDate) {
            todayKey, yesterdayKey -> savedStreak
            else -> 0
        }
        return DailyStudyStats(
            reviewsToday = savedReviewsToday,
            currentStreakDays = activeStreak,
            lastStudyDateKey = savedDate
        )
    }

    fun recordReviewActivity(nowMillis: Long = System.currentTimeMillis()): DailyStudyStats {
        val todayKey = dateKey(nowMillis)
        val yesterdayKey = dateKey(nowMillis - 24L * 60L * 60L * 1000L)
        val savedDate = prefs.getString(KEY_LAST_STUDY_DATE, "") ?: ""
        val prevCount = if (savedDate == todayKey) prefs.getInt(KEY_REVIEWS_TODAY, 0) else 0
        val prevStreak = prefs.getInt(KEY_STREAK_DAYS, 0)

        val newStreak = when (savedDate) {
            todayKey -> prevStreak.coerceAtLeast(1)
            yesterdayKey -> prevStreak + 1
            else -> 1
        }
        val newCount = prevCount + 1

        prefs.edit()
            .putString(KEY_LAST_STUDY_DATE, todayKey)
            .putInt(KEY_REVIEWS_TODAY, newCount)
            .putInt(KEY_STREAK_DAYS, newStreak)
            .apply()

        return DailyStudyStats(
            reviewsToday = newCount,
            currentStreakDays = newStreak,
            lastStudyDateKey = todayKey
        )
    }

    private fun dateKey(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(cal.timeInMillis))
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LAST_STUDY_DATE = "last_study_date"
        private const val KEY_REVIEWS_TODAY = "reviews_today"
        private const val KEY_STREAK_DAYS = "streak_days"
    }
}
