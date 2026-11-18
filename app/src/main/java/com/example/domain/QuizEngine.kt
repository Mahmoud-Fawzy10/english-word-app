package com.example.domain

import com.example.data.model.WordEntry

enum class QuizScopeMode(val labelAr: String, val descriptionAr: String) {
    SRS_DUE("مراجعة ذكية (SRS)", "الكلمات المستحقة للمراجعة الآن لتعزيز الحفظ"),
    BY_UNIT("حسب الوحدة", "اختبار على وحدة محددة أو نطاق من الوحدات"),
    BY_LESSON("حسب الدرس", "اختبار على درس محدد أو نطاق من الدروس"),
    BY_PAGE("حسب الصفحة", "اختبار على صفحة محددة أو نطاق من الصفحات"),
    RANDOM_RANGE("عشوائي حسب نطاق", "اختيار عشوائي ضمن النطاق والكتاب الذي تحدده")
}

enum class QuizQuestionType(val labelAr: String) {
    FLASHCARD_SRS("بطاقات استرجاع (SRS)"),
    MULTIPLE_CHOICE("اختيار من متعدد"),
    TYPING("كتابة الكلمة")
}

enum class QuizDirection(val labelAr: String) {
    WORD_TO_MEANING("الكلمة ← المعنى"),
    MEANING_TO_WORD("المعنى ← الكلمة")
}

data class QuizConfig(
    val scopeMode: QuizScopeMode = QuizScopeMode.SRS_DUE,
    val questionType: QuizQuestionType = QuizQuestionType.FLASHCARD_SRS,
    val direction: QuizDirection = QuizDirection.WORD_TO_MEANING,
    val selectedBook: String? = null,
    val unitFrom: String = "",
    val unitTo: String = "",
    val lessonFrom: String = "",
    val lessonTo: String = "",
    val pageFrom: String = "",
    val pageTo: String = "",
    val favoritesOnly: Boolean = false,
    val shuffle: Boolean = true,
    val questionLimit: Int = 10 // 0 means All matching words
)

data class QuizQuestionItem(
    val entry: WordEntry,
    val promptText: String,
    val answerText: String,
    val hintText: String,
    val options: List<String> = emptyList()
)

data class QuizAnswerRecord(
    val entry: WordEntry,
    val isCorrect: Boolean,
    val grade: SrsGrade,
    val userAnswer: String = ""
)

object QuizEngine {

    fun filterWordsForConfig(
        allWords: List<WordEntry>,
        config: QuizConfig,
        nowMillis: Long = System.currentTimeMillis()
    ): List<WordEntry> {
        val base = allWords.filter { entry ->
            when (config.questionType) {
                QuizQuestionType.FLASHCARD_SRS -> entry.hasAnyContent()
                QuizQuestionType.MULTIPLE_CHOICE,
                QuizQuestionType.TYPING -> entry.word.isNotBlank() && entry.arabicMeaning.isNotBlank()
            }
        }

        val scoped = base.filter { entry ->
            if (config.favoritesOnly && !entry.isFavorite) return@filter false

            if (!config.selectedBook.isNullOrBlank()) {
                val bookMatch = entry.books.any { it.equals(config.selectedBook, ignoreCase = true) } ||
                    entry.locations.any { it.book.equals(config.selectedBook, ignoreCase = true) }
                if (!bookMatch) return@filter false
            }

            when (config.scopeMode) {
                QuizScopeMode.SRS_DUE -> entry.isDueForReview(nowMillis)
                QuizScopeMode.BY_UNIT -> matchesFieldRange(
                    entry = entry,
                    from = config.unitFrom,
                    to = config.unitTo,
                    selector = { it.unit }
                )
                QuizScopeMode.BY_LESSON -> matchesFieldRange(
                    entry = entry,
                    from = config.lessonFrom,
                    to = config.lessonTo,
                    selector = { it.lesson }
                )
                QuizScopeMode.BY_PAGE -> matchesFieldRange(
                    entry = entry,
                    from = config.pageFrom,
                    to = config.pageTo,
                    selector = { it.page }
                )
                QuizScopeMode.RANDOM_RANGE -> {
                    matchesFieldRange(entry, config.unitFrom, config.unitTo) { it.unit } &&
                        matchesFieldRange(entry, config.lessonFrom, config.lessonTo) { it.lesson } &&
                        matchesFieldRange(entry, config.pageFrom, config.pageTo) { it.page }
                }
            }
        }

        val ordered = when {
            config.scopeMode == QuizScopeMode.RANDOM_RANGE || config.shuffle -> scoped.shuffled()
            config.scopeMode == QuizScopeMode.SRS_DUE -> scoped.sortedWith(
                compareBy<WordEntry> { it.srsNextReviewAt }.thenBy { it.srsRepetitions }
            )
            else -> scoped
        }

        return if (config.questionLimit > 0 && ordered.size > config.questionLimit) {
            ordered.take(config.questionLimit)
        } else {
            ordered
        }
    }

    fun buildQuizQuestions(
        selectedWords: List<WordEntry>,
        allWordsPool: List<WordEntry>,
        config: QuizConfig
    ): List<QuizQuestionItem> {
        return selectedWords.map { entry ->
            val prompt = when (config.direction) {
                QuizDirection.WORD_TO_MEANING -> entry.word.ifBlank { entry.displayTitle() }
                QuizDirection.MEANING_TO_WORD -> entry.arabicMeaning.ifBlank { entry.definition.ifBlank { entry.displayTitle() } }
            }
            val answer = when (config.direction) {
                QuizDirection.WORD_TO_MEANING -> entry.arabicMeaning.ifBlank { entry.definition.ifBlank { entry.displayTitle() } }
                QuizDirection.MEANING_TO_WORD -> entry.word.ifBlank { entry.displayTitle() }
            }
            val hint = buildString {
                if (entry.partsOfSpeech.isNotEmpty()) append(entry.partsOfSpeech.joinToString("، "))
                val locs = entry.locations.filter { it.isNotBlank() }.take(2).joinToString(" • ") { it.formattedLabel() }
                if (locs.isNotBlank()) {
                    if (isNotEmpty()) append("  |  ")
                    append(locs)
                }
            }

            val options = if (config.questionType == QuizQuestionType.MULTIPLE_CHOICE) {
                val distractors = allWordsPool
                    .asSequence()
                    .filter { it.id != entry.id }
                    .map { candidate ->
                        when (config.direction) {
                            QuizDirection.WORD_TO_MEANING -> candidate.arabicMeaning
                            QuizDirection.MEANING_TO_WORD -> candidate.word
                        }.trim()
                    }
                    .filter { it.isNotBlank() && !it.equals(answer.trim(), ignoreCase = true) }
                    .distinct()
                    .shuffled()
                    .take(3)
                    .toList()
                (distractors + answer).distinct().shuffled()
            } else {
                emptyList()
            }

            QuizQuestionItem(
                entry = entry,
                promptText = prompt,
                answerText = answer,
                hintText = hint,
                options = options
            )
        }
    }

    private fun matchesFieldRange(
        entry: WordEntry,
        from: String,
        to: String,
        selector: (com.example.data.model.WordLocation) -> String
    ): Boolean {
        val f = from.trim()
        val t = to.trim()
        if (f.isEmpty() && t.isEmpty()) return true

        val values = entry.locations.map(selector).filter { it.isNotBlank() }
        if (values.isEmpty()) return false

        val fromNum = extractNumbers(f).firstOrNull()
        val toNum = extractNumbers(t).firstOrNull()

        return if (fromNum != null || toNum != null) {
            val minVal = fromNum ?: toNum!!
            val maxVal = toNum ?: fromNum!!
            val low = minOf(minVal, maxVal)
            val high = maxOf(minVal, maxVal)
            values.any { raw ->
                extractNumbers(raw).any { n -> n in low..high }
            }
        } else {
            val q = (f.ifEmpty { t }).lowercase()
            values.any { it.lowercase().contains(q) }
        }
    }

    private fun extractNumbers(text: String): List<Int> {
        return Regex("\\d+").findAll(text).mapNotNull { it.value.toIntOrNull() }.toList()
    }
}
