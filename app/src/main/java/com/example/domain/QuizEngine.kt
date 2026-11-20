package com.example.domain

import com.example.data.model.WordEntry

enum class QuizScopeMode(val labelAr: String, val descriptionAr: String) {
    SRS_DUE("مراجعة ذكية (SRS)", "الكلمات المستحقة للمراجعة الآن لتعزيز الحفظ"),
    BY_UNIT("حسب الوحدة", "اختبار على وحدة محددة أو نطاق من الوحدات"),
    BY_LESSON("حسب الدرس", "اختبار على درس محدد أو نطاق من الدروس"),
    BY_PAGE("حسب الصفحة", "اختبار على صفحة محددة أو نطاق من الصفحات"),
    BY_STUDY_TAG("حسب المجموعة", "اختبار كلمات مجموعة دراسية محددة"),
    HARD_AND_MISTAKES("الصعبة والأخطاء", "تركيز على الكلمات الصعبة والتي أخطأت بها مؤخراً"),
    RANDOM_RANGE("عشوائي حسب نطاق", "اختيار عشوائي ضمن النطاق والكتاب الذي تحدده")
}

enum class QuizQuestionType(val labelAr: String) {
    FLASHCARD_SRS("بطاقات استرجاع (SRS)"),
    MULTIPLE_CHOICE("اختيار من متعدد"),
    TYPING("كتابة الكلمة / المعنى"),
    SYNONYMS("اختبار المرادفات"),
    ANTONYMS("اختبار المضادات"),
    PREPOSITIONS_COLLOCATIONS("حروف الجر والمتلازمات"),
    LETTER_SCRAMBLE("ترتيب الحروف (ميني-كويز)"),
    MATCH_PAIRS("توصيل الأزواج (ميني-كويز)"),
    MIXED("اختبار شامل متنوع")
}

enum class QuizDirection(val labelAr: String) {
    WORD_TO_MEANING("الكلمة ← المعنى"),
    MEANING_TO_WORD("المعنى ← الكلمة")
}

enum class QuestionInteractionKind {
    FLASHCARD,
    MCQ,
    TYPING,
    SCRAMBLE,
    MATCH_PAIRS
}

data class MatchPairItem(
    val entry: WordEntry,
    val leftText: String,
    val rightText: String
)

data class QuizConfig(
    val scopeMode: QuizScopeMode = QuizScopeMode.SRS_DUE,
    val questionType: QuizQuestionType = QuizQuestionType.FLASHCARD_SRS,
    val direction: QuizDirection = QuizDirection.WORD_TO_MEANING,
    val selectedBook: String? = null,
    val selectedStudyTag: String? = null,
    val unitFrom: String = "",
    val unitTo: String = "",
    val lessonFrom: String = "",
    val lessonTo: String = "",
    val pageFrom: String = "",
    val pageTo: String = "",
    val favoritesOnly: Boolean = false,
    val shuffle: Boolean = true,
    val requeueForgottenCards: Boolean = true,
    val questionLimit: Int = 10 // 0 means All matching words
)

data class QuizQuestionItem(
    val entry: WordEntry,
    val interactionKind: QuestionInteractionKind,
    val badgeLabel: String,
    val promptText: String,
    val answerText: String,
    val acceptedAnswers: List<String>,
    val hintText: String,
    val options: List<String> = emptyList(),
    val scrambleTiles: List<String> = emptyList(),
    val matchPairs: List<MatchPairItem> = emptyList()
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
                QuizQuestionType.TYPING,
                QuizQuestionType.MATCH_PAIRS -> entry.word.isNotBlank() && entry.arabicMeaning.isNotBlank()
                QuizQuestionType.LETTER_SCRAMBLE -> entry.word.trim().length >= 3
                QuizQuestionType.SYNONYMS -> entry.word.isNotBlank() && entry.synonyms.isNotEmpty()
                QuizQuestionType.ANTONYMS -> entry.word.isNotBlank() && entry.antonyms.isNotEmpty()
                QuizQuestionType.PREPOSITIONS_COLLOCATIONS ->
                    entry.word.isNotBlank() && (entry.prepositions.isNotEmpty() || entry.collocations.isNotEmpty())
                QuizQuestionType.MIXED -> {
                    (entry.word.isNotBlank() && entry.arabicMeaning.isNotBlank()) ||
                        (entry.word.isNotBlank() && entry.synonyms.isNotEmpty()) ||
                        (entry.word.isNotBlank() && entry.antonyms.isNotEmpty())
                }
            }
        }

        val scoped = base.filter { entry ->
            if (config.favoritesOnly && !entry.isFavorite) return@filter false

            if (!config.selectedBook.isNullOrBlank()) {
                val bookMatch = entry.books.any { it.equals(config.selectedBook, ignoreCase = true) } ||
                    entry.locations.any { it.book.equals(config.selectedBook, ignoreCase = true) }
                if (!bookMatch) return@filter false
            }

            if (!config.selectedStudyTag.isNullOrBlank()) {
                val tagMatch = entry.studyTags.any { it.equals(config.selectedStudyTag, ignoreCase = true) }
                if (!tagMatch) return@filter false
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
                QuizScopeMode.BY_STUDY_TAG -> {
                    config.selectedStudyTag.isNullOrBlank() ||
                        entry.studyTags.any { it.equals(config.selectedStudyTag, ignoreCase = true) }
                }
                QuizScopeMode.HARD_AND_MISTAKES -> entry.isHardOrMistake()
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
        if (config.questionType == QuizQuestionType.MATCH_PAIRS) {
            return buildMatchPairsRounds(selectedWords)
        }

        return selectedWords.mapIndexed { index, entry ->
            val effectiveType = when (config.questionType) {
                QuizQuestionType.MIXED -> pickMixedTypeFor(entry, index)
                else -> config.questionType
            }

            when (effectiveType) {
                QuizQuestionType.FLASHCARD_SRS -> buildFlashcardQuestion(entry, config)
                QuizQuestionType.MULTIPLE_CHOICE -> buildMeaningMcqQuestion(entry, allWordsPool, config)
                QuizQuestionType.TYPING -> buildMeaningTypingQuestion(entry, config)
                QuizQuestionType.SYNONYMS -> buildSynonymQuestion(entry, allWordsPool)
                QuizQuestionType.ANTONYMS -> buildAntonymQuestion(entry, allWordsPool)
                QuizQuestionType.PREPOSITIONS_COLLOCATIONS -> buildPrepCollocationQuestion(entry, allWordsPool)
                QuizQuestionType.LETTER_SCRAMBLE -> buildScrambleQuestion(entry)
                QuizQuestionType.MATCH_PAIRS -> buildMeaningMcqQuestion(entry, allWordsPool, config)
                QuizQuestionType.MIXED -> buildMeaningMcqQuestion(entry, allWordsPool, config)
            }
        }
    }

    private fun buildMatchPairsRounds(selectedWords: List<WordEntry>): List<QuizQuestionItem> {
        val valid = selectedWords.filter { it.word.isNotBlank() && it.arabicMeaning.isNotBlank() }
        if (valid.isEmpty()) return emptyList()
        return valid.chunked(5).mapIndexed { roundIdx, chunk ->
            val pairs = chunk.map { entry ->
                MatchPairItem(
                    entry = entry,
                    leftText = entry.word.trim(),
                    rightText = entry.arabicMeaning.trim()
                )
            }
            QuizQuestionItem(
                entry = chunk.first(),
                interactionKind = QuestionInteractionKind.MATCH_PAIRS,
                badgeLabel = "توصيل الأزواج — جولة ${roundIdx + 1}",
                promptText = "صل كل كلمة إنجليزية بمعناها العربي الصحيح",
                answerText = pairs.joinToString(" • ") { "${it.leftText} = ${it.rightText}" },
                acceptedAnswers = pairs.map { it.leftText },
                hintText = "${pairs.size} أزواج للتوصيل",
                matchPairs = pairs
            )
        }
    }

    private fun buildScrambleQuestion(entry: WordEntry): QuizQuestionItem {
        val target = entry.word.trim()
        val chars = target.map { it.toString() }
        var shuffled = chars.shuffled()
        if (shuffled == chars && chars.size > 1) {
            shuffled = chars.reversed()
        }
        val meaningHint = entry.arabicMeaning.ifBlank { entry.definition.ifBlank { "رتب الحروف لتكوين الكلمة" } }
        return QuizQuestionItem(
            entry = entry,
            interactionKind = QuestionInteractionKind.SCRAMBLE,
            badgeLabel = "ميني-كويز: ترتيب الحروف",
            promptText = meaningHint,
            answerText = target,
            acceptedAnswers = listOf(target),
            hintText = buildHint(entry, includeIpa = false),
            scrambleTiles = shuffled
        )
    }

    private fun buildPrepCollocationQuestion(entry: WordEntry, allWordsPool: List<WordEntry>): QuizQuestionItem {
        val validPreps = entry.prepositions.map { it.trim() }.filter { it.isNotBlank() }
        val validCols = entry.collocations.map { it.trim() }.filter { it.isNotBlank() }
        val usePreps = validPreps.isNotEmpty()
        val targets = if (usePreps) validPreps else validCols
        val primary = targets.firstOrNull().orEmpty()
        val prompt = if (usePreps) {
            "ما حرف الجر المستخدم مع «${entry.word}» (${entry.arabicMeaning})؟"
        } else {
            "ما المتلازمة اللفظية (Collocation) مع «${entry.word}» (${entry.arabicMeaning})؟"
        }
        val distractors = allWordsPool
            .asSequence()
            .filter { it.id != entry.id }
            .flatMap { cand ->
                (if (usePreps) cand.prepositions else cand.collocations).asSequence()
            }
            .map { it.trim() }
            .filter { cand -> cand.isNotBlank() && targets.none { it.equals(cand, ignoreCase = true) } }
            .distinct()
            .shuffled()
            .take(3)
            .toList()

        val options = if (distractors.isNotEmpty() && primary.isNotBlank()) {
            (distractors + primary).distinct().shuffled()
        } else {
            emptyList()
        }

        return QuizQuestionItem(
            entry = entry,
            interactionKind = if (options.size >= 2) QuestionInteractionKind.MCQ else QuestionInteractionKind.TYPING,
            badgeLabel = if (usePreps) "اختبار حروف الجر" else "اختبار المتلازمات اللفظية",
            promptText = prompt,
            answerText = targets.joinToString(" / "),
            acceptedAnswers = targets,
            hintText = buildHint(entry, includeIpa = true),
            options = options
        )
    }

    private fun pickMixedTypeFor(entry: WordEntry, index: Int): QuizQuestionType {
        val candidates = buildList {
            if (entry.word.isNotBlank() && entry.arabicMeaning.isNotBlank()) {
                add(QuizQuestionType.MULTIPLE_CHOICE)
                add(QuizQuestionType.TYPING)
                if (entry.word.trim().length >= 4) add(QuizQuestionType.LETTER_SCRAMBLE)
            }
            if (entry.word.isNotBlank() && entry.synonyms.isNotEmpty()) {
                add(QuizQuestionType.SYNONYMS)
            }
            if (entry.word.isNotBlank() && entry.antonyms.isNotEmpty()) {
                add(QuizQuestionType.ANTONYMS)
            }
            if (entry.word.isNotBlank() && (entry.prepositions.isNotEmpty() || entry.collocations.isNotEmpty())) {
                add(QuizQuestionType.PREPOSITIONS_COLLOCATIONS)
            }
        }
        return if (candidates.isEmpty()) QuizQuestionType.FLASHCARD_SRS else candidates[index % candidates.size]
    }

    private fun buildFlashcardQuestion(entry: WordEntry, config: QuizConfig): QuizQuestionItem {
        val prompt = when (config.direction) {
            QuizDirection.WORD_TO_MEANING -> entry.word.ifBlank { entry.displayTitle() }
            QuizDirection.MEANING_TO_WORD -> entry.arabicMeaning.ifBlank { entry.definition.ifBlank { entry.displayTitle() } }
        }
        val answer = when (config.direction) {
            QuizDirection.WORD_TO_MEANING -> entry.arabicMeaning.ifBlank { entry.definition.ifBlank { entry.displayTitle() } }
            QuizDirection.MEANING_TO_WORD -> entry.word.ifBlank { entry.displayTitle() }
        }
        return QuizQuestionItem(
            entry = entry,
            interactionKind = QuestionInteractionKind.FLASHCARD,
            badgeLabel = "بطاقة مراجعة ذكية",
            promptText = prompt,
            answerText = answer,
            acceptedAnswers = listOf(answer),
            hintText = buildHint(entry, includeIpa = config.direction == QuizDirection.WORD_TO_MEANING)
        )
    }

    private fun buildMeaningMcqQuestion(
        entry: WordEntry,
        allWordsPool: List<WordEntry>,
        config: QuizConfig
    ): QuizQuestionItem {
        val prompt = when (config.direction) {
            QuizDirection.WORD_TO_MEANING -> entry.word.ifBlank { entry.displayTitle() }
            QuizDirection.MEANING_TO_WORD -> entry.arabicMeaning.ifBlank { entry.definition.ifBlank { entry.displayTitle() } }
        }
        val answer = when (config.direction) {
            QuizDirection.WORD_TO_MEANING -> entry.arabicMeaning.ifBlank { entry.definition.ifBlank { entry.displayTitle() } }
            QuizDirection.MEANING_TO_WORD -> entry.word.ifBlank { entry.displayTitle() }
        }
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

        val options = (distractors + answer).distinct().shuffled()
        return QuizQuestionItem(
            entry = entry,
            interactionKind = if (options.size >= 2) QuestionInteractionKind.MCQ else QuestionInteractionKind.TYPING,
            badgeLabel = "اختر المعنى الصحيح",
            promptText = prompt,
            answerText = answer,
            acceptedAnswers = listOf(answer),
            hintText = buildHint(entry, includeIpa = config.direction == QuizDirection.WORD_TO_MEANING),
            options = options
        )
    }

    private fun buildMeaningTypingQuestion(entry: WordEntry, config: QuizConfig): QuizQuestionItem {
        val prompt = when (config.direction) {
            QuizDirection.WORD_TO_MEANING -> entry.word.ifBlank { entry.displayTitle() }
            QuizDirection.MEANING_TO_WORD -> entry.arabicMeaning.ifBlank { entry.definition.ifBlank { entry.displayTitle() } }
        }
        val answer = when (config.direction) {
            QuizDirection.WORD_TO_MEANING -> entry.arabicMeaning.ifBlank { entry.definition.ifBlank { entry.displayTitle() } }
            QuizDirection.MEANING_TO_WORD -> entry.word.ifBlank { entry.displayTitle() }
        }
        return QuizQuestionItem(
            entry = entry,
            interactionKind = QuestionInteractionKind.TYPING,
            badgeLabel = if (config.direction == QuizDirection.MEANING_TO_WORD) "اكتب الكلمة بالإنجليزية" else "اكتب المعنى بالعربي",
            promptText = prompt,
            answerText = answer,
            acceptedAnswers = listOf(answer),
            hintText = buildHint(entry, includeIpa = config.direction == QuizDirection.WORD_TO_MEANING)
        )
    }

    private fun buildSynonymQuestion(entry: WordEntry, allWordsPool: List<WordEntry>): QuizQuestionItem {
        val validSynonyms = entry.synonyms.map { it.trim() }.filter { it.isNotBlank() }
        val primarySyn = validSynonyms.firstOrNull().orEmpty()
        val prompt = if (entry.arabicMeaning.isNotBlank()) {
            "ما مرادف كلمة «${entry.word}» (${entry.arabicMeaning})؟"
        } else {
            "ما مرادف كلمة «${entry.word}»؟"
        }
        val distractorPool = allWordsPool
            .asSequence()
            .filter { it.id != entry.id }
            .flatMap { cand -> (cand.synonyms + cand.antonyms + listOf(cand.word)).asSequence() }
            .map { it.trim() }
            .filter { cand ->
                cand.isNotBlank() &&
                    !cand.equals(entry.word.trim(), ignoreCase = true) &&
                    validSynonyms.none { it.equals(cand, ignoreCase = true) }
            }
            .distinct()
            .shuffled()
            .take(3)
            .toList()

        val options = if (distractorPool.isNotEmpty() && primarySyn.isNotBlank()) {
            (distractorPool + primarySyn).distinct().shuffled()
        } else {
            emptyList()
        }

        return QuizQuestionItem(
            entry = entry,
            interactionKind = if (options.size >= 2) QuestionInteractionKind.MCQ else QuestionInteractionKind.TYPING,
            badgeLabel = "اختبار المرادفات (Synonym)",
            promptText = prompt,
            answerText = validSynonyms.joinToString(" / "),
            acceptedAnswers = validSynonyms,
            hintText = buildHint(entry, includeIpa = true),
            options = options
        )
    }

    private fun buildAntonymQuestion(entry: WordEntry, allWordsPool: List<WordEntry>): QuizQuestionItem {
        val validAntonyms = entry.antonyms.map { it.trim() }.filter { it.isNotBlank() }
        val primaryAnt = validAntonyms.firstOrNull().orEmpty()
        val prompt = if (entry.arabicMeaning.isNotBlank()) {
            "ما مضاد كلمة «${entry.word}» (${entry.arabicMeaning})؟"
        } else {
            "ما مضاد كلمة «${entry.word}»؟"
        }
        val distractorPool = allWordsPool
            .asSequence()
            .filter { it.id != entry.id }
            .flatMap { cand -> (cand.antonyms + cand.synonyms + listOf(cand.word)).asSequence() }
            .map { it.trim() }
            .filter { cand ->
                cand.isNotBlank() &&
                    !cand.equals(entry.word.trim(), ignoreCase = true) &&
                    validAntonyms.none { it.equals(cand, ignoreCase = true) }
            }
            .distinct()
            .shuffled()
            .take(3)
            .toList()

        val options = if (distractorPool.isNotEmpty() && primaryAnt.isNotBlank()) {
            (distractorPool + primaryAnt).distinct().shuffled()
        } else {
            emptyList()
        }

        return QuizQuestionItem(
            entry = entry,
            interactionKind = if (options.size >= 2) QuestionInteractionKind.MCQ else QuestionInteractionKind.TYPING,
            badgeLabel = "اختبار المضادات (Antonym)",
            promptText = prompt,
            answerText = validAntonyms.joinToString(" / "),
            acceptedAnswers = validAntonyms,
            hintText = buildHint(entry, includeIpa = true),
            options = options
        )
    }

    private fun buildHint(entry: WordEntry, includeIpa: Boolean): String = buildString {
        if (includeIpa && entry.ipa.isNotBlank()) {
            append(entry.ipa)
        }
        if (entry.partsOfSpeech.isNotEmpty()) {
            if (isNotEmpty()) append("  •  ")
            append(entry.partsOfSpeech.joinToString("، "))
        }
        val locs = entry.locations.filter { it.isNotBlank() }.take(2).joinToString(" • ") { it.formattedLabel() }
        if (locs.isNotBlank()) {
            if (isNotEmpty()) append("  |  ")
            append(locs)
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
