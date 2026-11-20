package com.example.domain

import com.example.data.model.WordEntry

data class WordIntelligenceProfile(
    val masteryScorePercent: Int,
    val completenessPercent: Int,
    val readinessLabelAr: String,
    val missingFieldsAr: List<String>,
    val isIncomplete: Boolean
)

data class AchievementBadge(
    val id: String,
    val titleAr: String,
    val descriptionAr: String,
    val current: Int,
    val target: Int,
    val isUnlocked: Boolean,
    val categoryAr: String
) {
    val progressFraction: Float
        get() = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
}

data class UserRankInfo(
    val rankTitleAr: String,
    val nextRankThreshold: Int,
    val totalWords: Int
)

data class SentencePracticePrompt(
    val titleWord: String,
    val targetPhrase: String,
    val arabicMeaning: String,
    val promptGuideAr: String
)

object LearningAnalyticsEngine {

    fun computeWordIntelligence(entry: WordEntry): WordIntelligenceProfile {
        val missing = buildList {
            if (entry.arabicMeaning.isBlank()) add("المعنى العربي")
            if (entry.ipa.isBlank()) add("النطق الصوتي IPA")
            if (entry.synonyms.isEmpty()) add("المرادفات")
            if (entry.antonyms.isEmpty()) add("المضادات")
            if (entry.prepositions.isEmpty()) add("حروف الجر")
            if (entry.examples.isEmpty()) add("الأمثلة السياقية")
            if (entry.derivatives.isBlank()) add("الاشتقاقات")
        }
        val totalChecks = 7
        val filledChecks = totalChecks - missing.size
        val completeness = (filledChecks * 100) / totalChecks

        // Mastery combines SRS maturity + usage richness
        val srsScore = when (SrsScheduler.maturityOf(entry)) {
            SrsMaturity.NEW -> 10
            SrsMaturity.LEARNING -> 40
            SrsMaturity.REVIEWING -> 75
            SrsMaturity.MASTERED -> 100
        }
        val accuracyBonus = if (entry.srsTotalReviews > 0) {
            ((entry.srsCorrectCount * 100) / entry.srsTotalReviews)
        } else {
            srsScore
        }
        val mastery = ((srsScore * 0.55f) + (accuracyBonus * 0.25f) + (completeness * 0.20f))
            .toInt()
            .coerceIn(0, 100)

        val readiness = when {
            mastery >= 85 && missing.size <= 1 -> "إتقان شامل واستخدام متكامل"
            mastery >= 65 -> "معرفة قوية قابلة للاستعمال"
            entry.srsTotalReviews > 0 -> "قيد التثبيت بالمراجعة"
            entry.arabicMeaning.isNotBlank() -> "تم حفظ المعنى الأساسي"
            else -> "ملف يحتاج استكمال البيانات"
        }

        val isIncomplete = entry.arabicMeaning.isBlank() ||
            (entry.examples.isEmpty() && entry.synonyms.isEmpty() && entry.prepositions.isEmpty())

        return WordIntelligenceProfile(
            masteryScorePercent = mastery,
            completenessPercent = completeness,
            readinessLabelAr = readiness,
            missingFieldsAr = missing,
            isIncomplete = isIncomplete
        )
    }

    fun computeRank(totalWords: Int): UserRankInfo {
        return when {
            totalWords >= 500 -> UserRankInfo("خبير المفردات", 1000, totalWords)
            totalWords >= 200 -> UserRankInfo("مستكشف الكلمات", 500, totalWords)
            totalWords >= 50 -> UserRankInfo("صياد الكلمات", 200, totalWords)
            totalWords >= 10 -> UserRankInfo("متعلم نشط", 50, totalWords)
            else -> UserRankInfo("بداية القاموس", 10, totalWords)
        }
    }

    fun buildAchievements(
        words: List<WordEntry>,
        totalReviewsAllTime: Int,
        overallAccuracyPercent: Int,
        streakDays: Int,
        miniQuizzesToday: Int,
        duplicateGroupsCount: Int
    ): List<AchievementBadge> {
        val total = words.size
        val mastered = words.count { SrsScheduler.maturityOf(it) == SrsMaturity.MASTERED }
        val prepColCount = words.count { it.prepositions.isNotEmpty() || it.collocations.isNotEmpty() }
        val completeProfiles = words.count { !computeWordIntelligence(it).isIncomplete }
        val hardCount = words.count { it.isHardOrMistake() }

        return listOf(
            AchievementBadge(
                id = "vocab_expert",
                titleAr = "خبير المفردات",
                descriptionAr = "أضف 50 كلمة فأكثر إلى قاموسك الشخصي",
                current = total.coerceAtMost(50),
                target = 50,
                isUnlocked = total >= 50,
                categoryAr = "الحصيلة"
            ),
            AchievementBadge(
                id = "quiz_champion",
                titleAr = "بطل الاختبارات",
                descriptionAr = "أكمل 30 مراجعة بنسبة دقة 75% أو أعلى",
                current = if (overallAccuracyPercent >= 75) totalReviewsAllTime.coerceAtMost(30) else (totalReviewsAllTime / 2).coerceAtMost(29),
                target = 30,
                isUnlocked = totalReviewsAllTime >= 30 && overallAccuracyPercent >= 75,
                categoryAr = "الاختبارات"
            ),
            AchievementBadge(
                id = "review_master",
                titleAr = "سيد المراجعة",
                descriptionAr = "حافظ على سلسلة مذاكرة متواصلة لمدة 7 أيام",
                current = streakDays.coerceAtMost(7),
                target = 7,
                isUnlocked = streakDays >= 7,
                categoryAr = "الاستمرارية"
            ),
            AchievementBadge(
                id = "first_ten",
                titleAr = "الانطلاقة الأولى",
                descriptionAr = "أضف أول 10 كلمات في قاموسك",
                current = total.coerceAtMost(10),
                target = 10,
                isUnlocked = total >= 10,
                categoryAr = "الحصيلة"
            ),
            AchievementBadge(
                id = "mastery_milestone",
                titleAr = "حافظ متمكن",
                descriptionAr = "أتقن 20 كلمة بالكامل عبر نظام التكرار المتباعد SRS",
                current = mastered.coerceAtMost(20),
                target = 20,
                isUnlocked = mastered >= 20,
                categoryAr = "الإتقان"
            ),
            AchievementBadge(
                id = "prep_specialist",
                titleAr = "مهندس التراكيب",
                descriptionAr = "سجّل حروف الجر أو المتلازمات اللفظية لـ 15 كلمة",
                current = prepColCount.coerceAtMost(15),
                target = 15,
                isUnlocked = prepColCount >= 15,
                categoryAr = "العمق اللغوي"
            ),
            AchievementBadge(
                id = "rich_cards",
                titleAr = "بطاقات مكتملة",
                descriptionAr = "أكمل أمثلة ومرادفات لـ 25 كلمة دون بيانات ناقصة",
                current = completeProfiles.coerceAtMost(25),
                target = 25,
                isUnlocked = completeProfiles >= 25,
                categoryAr = "الجودة"
            ),
            AchievementBadge(
                id = "clean_dict",
                titleAr = "قاموس نقي بلا تكرار",
                descriptionAr = "احتفظ بـ 5 كلمات على الأقل مع 0 كلمات مكررة",
                current = if (duplicateGroupsCount == 0 && total >= 5) 1 else 0,
                target = 1,
                isUnlocked = duplicateGroupsCount == 0 && total >= 5,
                categoryAr = "التنظيم"
            ),
            AchievementBadge(
                id = "mini_quiz_spark",
                titleAr = "شرارة التحدي",
                descriptionAr = "أنهِ جولة ميني-كويز واحدة اليوم",
                current = miniQuizzesToday.coerceAtMost(1),
                target = 1,
                isUnlocked = miniQuizzesToday >= 1,
                categoryAr = "التحديات"
            ),
            AchievementBadge(
                id = "vault_cleared",
                titleAr = "قاهر الكلمات الصعبة",
                descriptionAr = "صفِّ خزنة الأخطاء الصعبة مع وجود 10 كلمات فصاعداً",
                current = if (total >= 10 && hardCount == 0) 1 else 0,
                target = 1,
                isUnlocked = total >= 10 && hardCount == 0,
                categoryAr = "الإتقان"
            )
        )
    }

    fun buildSentencePrompts(words: List<WordEntry>, limit: Int = 5): List<SentencePracticePrompt> {
        return words
            .filter { it.word.isNotBlank() }
            .take(limit)
            .map { entry ->
                val targetPhrase = when {
                    entry.collocations.isNotEmpty() -> entry.collocations.first()
                    entry.prepositions.isNotEmpty() -> entry.prepositions.first()
                    else -> entry.word.trim()
                }
                val guide = when {
                    entry.collocations.isNotEmpty() -> "كوّن جملة مفيدة تستخدم المتلازمة «$targetPhrase»"
                    entry.prepositions.isNotEmpty() -> "استخدم الفعل أو الكلمة مع حرف الجر «$targetPhrase» في جملة"
                    entry.examples.isNotEmpty() -> "أعد صياغة مثال مشابه باستخدام «${entry.word.trim()}»"
                    else -> "اكتب جملة قصيرة توضّح معنى «${entry.word.trim()}»"
                }
                SentencePracticePrompt(
                    titleWord = entry.word.trim(),
                    targetPhrase = targetPhrase,
                    arabicMeaning = entry.arabicMeaning,
                    promptGuideAr = guide
                )
            }
    }
}
