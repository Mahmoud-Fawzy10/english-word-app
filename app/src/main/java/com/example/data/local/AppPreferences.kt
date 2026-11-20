package com.example.data.local

import android.content.Context
import com.example.data.model.WordFormConfig
import org.json.JSONArray
import org.json.JSONObject
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
    val lastStudyDateKey: String = "",
    val dailyGoalTarget: Int = 15,
    val miniQuizzesToday: Int = 0
)

data class DailyHistoryPoint(
    val dateKey: String,
    val dayLabelAr: String,
    val reviewsCount: Int,
    val masteryPercent: Int
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

    fun getDailyGoalTarget(): Int = prefs.getInt(KEY_DAILY_GOAL, 15).coerceIn(5, 100)

    fun setDailyGoalTarget(target: Int) {
        prefs.edit().putInt(KEY_DAILY_GOAL, target.coerceIn(5, 100)).apply()
    }

    fun getTtsSpeechRate(): Float = prefs.getFloat(KEY_TTS_RATE, 0.92f).coerceIn(0.6f, 1.4f)

    fun setTtsSpeechRate(rate: Float) {
        prefs.edit().putFloat(KEY_TTS_RATE, rate.coerceIn(0.6f, 1.4f)).apply()
    }

    fun getFormConfig(): WordFormConfig = WordFormConfig(
        showIpa = prefs.getBoolean("form_show_ipa", true),
        showPartOfSpeech = prefs.getBoolean("form_show_pos", true),
        showBooksAndLocations = prefs.getBoolean("form_show_books_loc", true),
        showSynonymsAntonyms = prefs.getBoolean("form_show_syn_ant", true),
        showPrepositionsCollocations = prefs.getBoolean("form_show_prep_col", true),
        showDefinitionDerivativesExpressions = prefs.getBoolean("form_show_def_der_exp", true),
        showExamples = prefs.getBoolean("form_show_examples", true),
        showNotesAndLinguistic = prefs.getBoolean("form_show_notes", true),
        showDifficultyAndTags = prefs.getBoolean("form_show_diff_tags", true),
        showPersonalHint = prefs.getBoolean("form_show_hint", true)
    )

    fun setFormConfig(config: WordFormConfig) {
        prefs.edit()
            .putBoolean("form_show_ipa", config.showIpa)
            .putBoolean("form_show_pos", config.showPartOfSpeech)
            .putBoolean("form_show_books_loc", config.showBooksAndLocations)
            .putBoolean("form_show_syn_ant", config.showSynonymsAntonyms)
            .putBoolean("form_show_prep_col", config.showPrepositionsCollocations)
            .putBoolean("form_show_def_der_exp", config.showDefinitionDerivativesExpressions)
            .putBoolean("form_show_examples", config.showExamples)
            .putBoolean("form_show_notes", config.showNotesAndLinguistic)
            .putBoolean("form_show_diff_tags", config.showDifficultyAndTags)
            .putBoolean("form_show_hint", config.showPersonalHint)
            .apply()
    }

    fun getRecentSearches(): List<String> {
        val raw = prefs.getString(KEY_RECENT_SEARCHES, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i).trim()
                    if (v.isNotBlank()) add(v)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addRecentSearch(query: String): List<String> {
        val clean = query.trim()
        if (clean.length < 2) return getRecentSearches()
        val updated = (listOf(clean) + getRecentSearches().filterNot { it.equals(clean, ignoreCase = true) })
            .take(8)
        val arr = JSONArray()
        updated.forEach { arr.put(it) }
        prefs.edit().putString(KEY_RECENT_SEARCHES, arr.toString()).apply()
        return updated
    }

    fun clearRecentSearches(): List<String> {
        prefs.edit().putString(KEY_RECENT_SEARCHES, "[]").apply()
        return emptyList()
    }

    fun getDailyStudyStats(nowMillis: Long = System.currentTimeMillis()): DailyStudyStats {
        val todayKey = dateKey(nowMillis)
        val yesterdayKey = dateKey(nowMillis - 24L * 60L * 60L * 1000L)
        val savedDate = prefs.getString(KEY_LAST_STUDY_DATE, "") ?: ""
        val savedReviewsToday = if (savedDate == todayKey) prefs.getInt(KEY_REVIEWS_TODAY, 0) else 0
        val savedMiniQuizzes = if (savedDate == todayKey) prefs.getInt(KEY_MINI_QUIZZES_TODAY, 0) else 0
        val savedStreak = prefs.getInt(KEY_STREAK_DAYS, 0)
        val activeStreak = when (savedDate) {
            todayKey, yesterdayKey -> savedStreak
            else -> 0
        }
        return DailyStudyStats(
            reviewsToday = savedReviewsToday,
            currentStreakDays = activeStreak,
            lastStudyDateKey = savedDate,
            dailyGoalTarget = getDailyGoalTarget(),
            miniQuizzesToday = savedMiniQuizzes
        )
    }

    fun recordReviewActivity(
        nowMillis: Long = System.currentTimeMillis(),
        incrementMiniQuiz: Boolean = false
    ): DailyStudyStats {
        val todayKey = dateKey(nowMillis)
        val yesterdayKey = dateKey(nowMillis - 24L * 60L * 60L * 1000L)
        val savedDate = prefs.getString(KEY_LAST_STUDY_DATE, "") ?: ""
        val prevCount = if (savedDate == todayKey) prefs.getInt(KEY_REVIEWS_TODAY, 0) else 0
        val prevMini = if (savedDate == todayKey) prefs.getInt(KEY_MINI_QUIZZES_TODAY, 0) else 0
        val prevStreak = prefs.getInt(KEY_STREAK_DAYS, 0)

        val newStreak = when (savedDate) {
            todayKey -> prevStreak.coerceAtLeast(1)
            yesterdayKey -> prevStreak + 1
            else -> 1
        }
        val newCount = prevCount + 1
        val newMini = prevMini + if (incrementMiniQuiz) 1 else 0

        prefs.edit()
            .putString(KEY_LAST_STUDY_DATE, todayKey)
            .putInt(KEY_REVIEWS_TODAY, newCount)
            .putInt(KEY_MINI_QUIZZES_TODAY, newMini)
            .putInt(KEY_STREAK_DAYS, newStreak)
            .apply()

        updateDailyHistoryPoint(dateKey = todayKey, reviewsCount = newCount, masteryPercent = null)

        return DailyStudyStats(
            reviewsToday = newCount,
            currentStreakDays = newStreak,
            lastStudyDateKey = todayKey,
            dailyGoalTarget = getDailyGoalTarget(),
            miniQuizzesToday = newMini
        )
    }

    fun updateTodayMasterySnapshot(masteryPercent: Int, nowMillis: Long = System.currentTimeMillis()) {
        val todayKey = dateKey(nowMillis)
        val stats = getDailyStudyStats(nowMillis)
        updateDailyHistoryPoint(
            dateKey = todayKey,
            reviewsCount = stats.reviewsToday,
            masteryPercent = masteryPercent.coerceIn(0, 100)
        )
    }

    fun getLast7DaysHistory(
        currentMasteryPercent: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): List<DailyHistoryPoint> {
        val rawMap = readHistoryJson()
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val todayKey = dateKey(nowMillis)

        return buildList {
            for (i in 0 until 7) {
                val key = dateKey(cal.timeInMillis)
                val dayLabel = arabicDayShort(cal.get(Calendar.DAY_OF_WEEK))
                val entry = rawMap.optJSONObject(key)
                val rev = entry?.optInt("reviews", 0) ?: 0
                val mast = if (key == todayKey) {
                    currentMasteryPercent.coerceIn(0, 100)
                } else {
                    entry?.optInt("mastery", 0) ?: 0
                }
                add(
                    DailyHistoryPoint(
                        dateKey = key,
                        dayLabelAr = dayLabel,
                        reviewsCount = rev,
                        masteryPercent = mast
                    )
                )
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    private fun updateDailyHistoryPoint(dateKey: String, reviewsCount: Int, masteryPercent: Int?) {
        val obj = readHistoryJson()
        val cur = obj.optJSONObject(dateKey) ?: JSONObject()
        cur.put("reviews", reviewsCount)
        if (masteryPercent != null) {
            cur.put("mastery", masteryPercent)
        }
        obj.put(dateKey, cur)

        // Keep max 30 days in prefs so SharedPreferences stays tiny on Android 9
        val keys = obj.keys().asSequence().toList().sorted()
        if (keys.size > 30) {
            keys.take(keys.size - 30).forEach { oldKey -> obj.remove(oldKey) }
        }
        prefs.edit().putString(KEY_DAILY_HISTORY, obj.toString()).apply()
    }

    private fun readHistoryJson(): JSONObject {
        val raw = prefs.getString(KEY_DAILY_HISTORY, "{}") ?: "{}"
        return runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
    }

    private fun arabicDayShort(dayOfWeek: Int): String = when (dayOfWeek) {
        Calendar.SATURDAY -> "السبت"
        Calendar.SUNDAY -> "الأحد"
        Calendar.MONDAY -> "الإثنين"
        Calendar.TUESDAY -> "الثلاثاء"
        Calendar.WEDNESDAY -> "الأربعاء"
        Calendar.THURSDAY -> "الخميس"
        else -> "الجمعة"
    }

    private fun dateKey(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(cal.timeInMillis))
    }

    companion object {
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_LAST_STUDY_DATE = "last_study_date"
        private const val KEY_REVIEWS_TODAY = "reviews_today"
        private const val KEY_MINI_QUIZZES_TODAY = "mini_quizzes_today"
        private const val KEY_STREAK_DAYS = "streak_days"
        private const val KEY_DAILY_GOAL = "daily_goal_target"
        private const val KEY_TTS_RATE = "tts_speech_rate"
        private const val KEY_RECENT_SEARCHES = "recent_searches"
        private const val KEY_DAILY_HISTORY = "daily_history_map"
    }
}
