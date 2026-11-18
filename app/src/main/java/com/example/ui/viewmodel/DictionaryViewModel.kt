package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.QamusiDatabase
import com.example.data.model.AdvancedSearchFilter
import com.example.data.model.CurriculumDefaults
import com.example.data.model.SearchField
import com.example.data.model.SortOption
import com.example.data.model.WordEntry
import com.example.data.repository.WordRepository
import com.example.domain.QuizAnswerRecord
import com.example.domain.QuizConfig
import com.example.domain.QuizDirection
import com.example.domain.QuizEngine
import com.example.domain.QuizQuestionItem
import com.example.domain.QuizQuestionType
import com.example.domain.QuizScopeMode
import com.example.domain.SrsGrade
import com.example.export.DictionaryExporter
import com.example.export.ExportFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DictionaryUiState(
    val allWordsCount: Int = 0,
    val favoriteCount: Int = 0,
    val dueReviewCount: Int = 0,
    val masteredCount: Int = 0,
    val allWords: List<WordEntry> = emptyList(),
    val filteredWords: List<WordEntry> = emptyList(),
    val availableBooks: List<String> = CurriculumDefaults.DEFAULT_BOOKS,
    val availablePartsOfSpeech: List<String> = CurriculumDefaults.DEFAULT_PARTS_OF_SPEECH,
    val availableUnits: List<String> = emptyList(),
    val availableLessons: List<String> = emptyList(),
    val searchQuery: String = "",
    val searchField: SearchField = SearchField.ALL,
    val quickBookFilter: String? = null,
    val advancedFilter: AdvancedSearchFilter = AdvancedSearchFilter(),
    val sortOption: SortOption = SortOption.NEWEST,
    val isExporting: Boolean = false
) {
    val isSearchOrFilterActive: Boolean
        get() = searchQuery.isNotBlank() || quickBookFilter != null || advancedFilter.isActive

    val hasNoSearchResults: Boolean
        get() = allWordsCount > 0 && filteredWords.isEmpty() && isSearchOrFilterActive
}

data class QuizSessionState(
    val config: QuizConfig,
    val questions: List<QuizQuestionItem>,
    val currentIndex: Int = 0,
    val isAnswerRevealed: Boolean = false,
    val selectedMcqOption: String? = null,
    val typedInput: String = "",
    val lastTypingCorrect: Boolean? = null,
    val records: List<QuizAnswerRecord> = emptyList(),
    val isFinished: Boolean = false
) {
    val currentQuestion: QuizQuestionItem?
        get() = questions.getOrNull(currentIndex)
}

sealed class UiEvent {
    data class ShowMessage(val message: String) : UiEvent()
    data class LaunchShareIntent(val uri: Uri, val format: ExportFormat) : UiEvent()
}

class DictionaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WordRepository

    init {
        val db = QamusiDatabase.getInstance(application)
        repository = WordRepository(db.wordDao())
    }

    private val _searchQuery = MutableStateFlow("")
    private val _searchField = MutableStateFlow(SearchField.ALL)
    private val _quickBookFilter = MutableStateFlow<String?>(null)
    private val _advancedFilter = MutableStateFlow(AdvancedSearchFilter())
    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    private val _isExporting = MutableStateFlow(false)

    private val _quizConfig = MutableStateFlow(QuizConfig())
    val quizConfig: StateFlow<QuizConfig> = _quizConfig.asStateFlow()

    private val _quizSession = MutableStateFlow<QuizSessionState?>(null)
    val quizSession: StateFlow<QuizSessionState?> = _quizSession.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val uiState: StateFlow<DictionaryUiState> = combine(
        repository.allWords,
        _searchQuery,
        _searchField,
        _quickBookFilter,
        _advancedFilter,
        _sortOption,
        _isExporting
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allWords = values[0] as List<WordEntry>
        val query = values[1] as String
        val field = values[2] as SearchField
        val quickBook = values[3] as String?
        val advFilter = values[4] as AdvancedSearchFilter
        val sort = values[5] as SortOption
        val exporting = values[6] as Boolean

        val now = System.currentTimeMillis()
        val dynamicBooks = (CurriculumDefaults.DEFAULT_BOOKS +
            allWords.flatMap { it.books } +
            allWords.flatMap { it.locations.map { loc -> loc.book } })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val dynamicPos = (CurriculumDefaults.DEFAULT_PARTS_OF_SPEECH +
            allWords.flatMap { it.partsOfSpeech })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val dynamicUnits = allWords.flatMap { it.locations.map { loc -> loc.unit.trim() } }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { Regex("\\d+").find(it)?.value?.toIntOrNull() ?: Int.MAX_VALUE }

        val dynamicLessons = allWords.flatMap { it.locations.map { loc -> loc.lesson.trim() } }
            .filter { it.isNotBlank() }
            .distinct()

        val filtered = allWords
            .filter { entry -> matchesSearch(entry, query, field) }
            .filter { entry ->
                quickBook == null ||
                    entry.books.any { it.equals(quickBook, ignoreCase = true) } ||
                    entry.locations.any { it.book.equals(quickBook, ignoreCase = true) }
            }
            .filter { entry -> matchesAdvancedFilter(entry, advFilter) }

        val sorted = applySorting(filtered, sort)

        DictionaryUiState(
            allWordsCount = allWords.size,
            favoriteCount = allWords.count { it.isFavorite },
            dueReviewCount = allWords.count { it.isDueForReview(now) },
            masteredCount = allWords.count { it.srsIntervalDays >= 14 },
            allWords = allWords,
            filteredWords = sorted,
            availableBooks = dynamicBooks,
            availablePartsOfSpeech = dynamicPos,
            availableUnits = dynamicUnits,
            availableLessons = dynamicLessons,
            searchQuery = query,
            searchField = field,
            quickBookFilter = quickBook,
            advancedFilter = advFilter,
            sortOption = sort,
            isExporting = exporting
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DictionaryUiState()
    )

    fun observeWord(id: Long): Flow<WordEntry?> = repository.observeWordById(id)

    suspend fun getWordOnce(id: Long): WordEntry? = repository.getWordOnce(id)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSearchField(field: SearchField) {
        _searchField.value = field
    }

    fun toggleQuickBookFilter(book: String?) {
        _quickBookFilter.value = if (_quickBookFilter.value == book) null else book
    }

    fun updateAdvancedFilter(filter: AdvancedSearchFilter) {
        _advancedFilter.value = filter
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _quickBookFilter.value = null
        _advancedFilter.value = AdvancedSearchFilter()
    }

    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun saveWord(entry: WordEntry, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val cleaned = entry.copy(
                word = entry.word.trim(),
                arabicMeaning = entry.arabicMeaning.trim(),
                books = entry.books.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
                locations = entry.locations.filter { it.isNotBlank() },
                partsOfSpeech = entry.partsOfSpeech.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
                synonyms = entry.synonyms.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
                antonyms = entry.antonyms.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
                definition = entry.definition.trim(),
                derivatives = entry.derivatives.trim(),
                expressions = entry.expressions.trim(),
                notes = entry.notes.trim(),
                importantNotes = entry.importantNotes.trim()
            )
            val savedId = repository.saveWord(cleaned)
            _events.emit(UiEvent.ShowMessage("تم حفظ الكلمة في قاموسك"))
            onSaved(savedId)
        }
    }

    fun deleteWord(id: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteWord(id)
            _events.emit(UiEvent.ShowMessage("تم حذف الكلمة"))
            onDeleted()
        }
    }

    fun toggleFavorite(entry: WordEntry) {
        viewModelScope.launch {
            repository.toggleFavorite(entry.id, entry.isFavorite)
        }
    }

    // -------------------------------------------------------------------------
    // Spaced Repetition (SRS) & Quiz Session Actions
    // -------------------------------------------------------------------------
    fun updateQuizConfig(transform: (QuizConfig) -> QuizConfig) {
        _quizConfig.value = transform(_quizConfig.value)
    }

    fun previewMatchingQuizCount(config: QuizConfig = _quizConfig.value): Int {
        return QuizEngine.filterWordsForConfig(uiState.value.allWords, config).size
    }

    fun startQuiz(overrideConfig: QuizConfig? = null): Boolean {
        val activeConfig = overrideConfig ?: _quizConfig.value
        if (overrideConfig != null) _quizConfig.value = overrideConfig

        val pool = uiState.value.allWords
        val matched = QuizEngine.filterWordsForConfig(pool, activeConfig)
        if (matched.isEmpty()) {
            viewModelScope.launch {
                val msg = if (activeConfig.questionType != QuizQuestionType.FLASHCARD_SRS) {
                    "لا توجد كلمات تحتوي على الكلمة والمعنى معاً ضمن هذا النطاق"
                } else {
                    "لا توجد كلمات مطابقة للنطاق المحدد حالياً"
                }
                _events.emit(UiEvent.ShowMessage(msg))
            }
            return false
        }

        val questions = QuizEngine.buildQuizQuestions(matched, pool, activeConfig)
        _quizSession.value = QuizSessionState(
            config = activeConfig,
            questions = questions
        )
        return true
    }

    fun startSingleWordReview(word: WordEntry): Boolean {
        val config = QuizConfig(
            scopeMode = QuizScopeMode.SRS_DUE,
            questionType = QuizQuestionType.FLASHCARD_SRS,
            direction = QuizDirection.WORD_TO_MEANING,
            questionLimit = 1
        )
        val questions = QuizEngine.buildQuizQuestions(listOf(word), uiState.value.allWords, config)
        _quizSession.value = QuizSessionState(
            config = config,
            questions = questions
        )
        return true
    }

    fun revealFlashcard() {
        _quizSession.value = _quizSession.value?.copy(isAnswerRevealed = true)
    }

    fun submitFlashcardGrade(grade: SrsGrade) {
        val session = _quizSession.value ?: return
        val question = session.currentQuestion ?: return
        viewModelScope.launch {
            repository.recordSrsReview(question.entry, grade)
        }
        val record = QuizAnswerRecord(
            entry = question.entry,
            isCorrect = grade != SrsGrade.AGAIN,
            grade = grade
        )
        advanceWithRecord(session, record)
    }

    fun submitMcqOption(option: String) {
        val session = _quizSession.value ?: return
        if (session.selectedMcqOption != null) return
        val question = session.currentQuestion ?: return
        val isCorrect = option.trim().equals(question.answerText.trim(), ignoreCase = true)
        val grade = if (isCorrect) SrsGrade.GOOD else SrsGrade.AGAIN
        viewModelScope.launch {
            repository.recordSrsReview(question.entry, grade)
        }
        val record = QuizAnswerRecord(
            entry = question.entry,
            isCorrect = isCorrect,
            grade = grade,
            userAnswer = option
        )
        _quizSession.value = session.copy(
            selectedMcqOption = option,
            isAnswerRevealed = true,
            records = session.records + record
        )
    }

    fun updateTypedInput(input: String) {
        _quizSession.value = _quizSession.value?.copy(typedInput = input)
    }

    fun submitTypingAnswer() {
        val session = _quizSession.value ?: return
        if (session.lastTypingCorrect != null) return
        val question = session.currentQuestion ?: return
        val isCorrect = normalize(session.typedInput) == normalize(question.answerText)
        val grade = if (isCorrect) SrsGrade.GOOD else SrsGrade.AGAIN
        viewModelScope.launch {
            repository.recordSrsReview(question.entry, grade)
        }
        val record = QuizAnswerRecord(
            entry = question.entry,
            isCorrect = isCorrect,
            grade = grade,
            userAnswer = session.typedInput.trim()
        )
        _quizSession.value = session.copy(
            lastTypingCorrect = isCorrect,
            isAnswerRevealed = true,
            records = session.records + record
        )
    }

    fun nextQuizQuestion() {
        val session = _quizSession.value ?: return
        val nextIndex = session.currentIndex + 1
        if (nextIndex >= session.questions.size) {
            _quizSession.value = session.copy(isFinished = true)
        } else {
            _quizSession.value = session.copy(
                currentIndex = nextIndex,
                isAnswerRevealed = false,
                selectedMcqOption = null,
                typedInput = "",
                lastTypingCorrect = null
            )
        }
    }

    private fun advanceWithRecord(session: QuizSessionState, record: QuizAnswerRecord) {
        val updatedRecords = session.records + record
        val nextIndex = session.currentIndex + 1
        if (nextIndex >= session.questions.size) {
            _quizSession.value = session.copy(
                records = updatedRecords,
                isFinished = true
            )
        } else {
            _quizSession.value = session.copy(
                currentIndex = nextIndex,
                isAnswerRevealed = false,
                selectedMcqOption = null,
                typedInput = "",
                lastTypingCorrect = null,
                records = updatedRecords
            )
        }
    }

    fun restartCurrentQuiz() {
        val currentConfig = _quizSession.value?.config ?: _quizConfig.value
        startQuiz(currentConfig)
    }

    fun endQuizSession() {
        _quizSession.value = null
    }

    fun updateManualSrsSchedule(
        entry: WordEntry,
        intervalDays: Int,
        easeFactor: Float,
        resetProgress: Boolean
    ) {
        viewModelScope.launch {
            repository.updateManualSrsSchedule(
                entry = entry,
                intervalDays = intervalDays,
                easeFactor = easeFactor,
                resetProgress = resetProgress
            )
            val msg = when {
                resetProgress -> "تمت إعادة ضبط جدول المراجعة للكلمة"
                intervalDays == 0 -> "تم ضبط الكلمة للمراجعة الفورية الآن"
                else -> "تم تحديث موعد المراجعة القادمة بعد $intervalDays يوم"
            }
            _events.emit(UiEvent.ShowMessage(msg))
        }
    }

    // -------------------------------------------------------------------------
    // Offline Export Actions
    // -------------------------------------------------------------------------
    fun exportAndShare(format: ExportFormat, onlyFiltered: Boolean) {
        viewModelScope.launch {
            val wordsToExport = if (onlyFiltered) uiState.value.filteredWords else getAllWordsSnapshot()
            if (wordsToExport.isEmpty()) {
                _events.emit(UiEvent.ShowMessage("لا توجد كلمات لتصديرها حالياً"))
                return@launch
            }
            _isExporting.value = true
            try {
                val uri = withContext(Dispatchers.IO) {
                    DictionaryExporter.exportToShareableUri(
                        context = getApplication(),
                        words = wordsToExport,
                        format = format
                    )
                }
                _events.emit(UiEvent.LaunchShareIntent(uri, format))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage("حدث خطأ أثناء التصدير: ${e.localizedMessage ?: "غير معروف"}"))
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportToPickedUri(uri: Uri, format: ExportFormat, onlyFiltered: Boolean) {
        viewModelScope.launch {
            val wordsToExport = if (onlyFiltered) uiState.value.filteredWords else getAllWordsSnapshot()
            if (wordsToExport.isEmpty()) {
                _events.emit(UiEvent.ShowMessage("لا توجد كلمات لتصديرها حالياً"))
                return@launch
            }
            _isExporting.value = true
            try {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        DictionaryExporter.exportToStream(wordsToExport, format, out)
                    } ?: error("تعذر فتح الملف للحفظ")
                }
                _events.emit(UiEvent.ShowMessage("تم حفظ ${format.titleAr} بنجاح أوفلاين"))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage("تعذر حفظ الملف: ${e.localizedMessage ?: "خطأ غير معروف"}"))
            } finally {
                _isExporting.value = false
            }
        }
    }

    private suspend fun getAllWordsSnapshot(): List<WordEntry> {
        return withContext(Dispatchers.IO) {
            repository.allWords.first()
        }
    }

    private fun matchesSearch(entry: WordEntry, rawQuery: String, field: SearchField): Boolean {
        val q = normalize(rawQuery)
        if (q.isEmpty()) return true

        return when (field) {
            SearchField.ALL -> listOf(
                entry.word,
                entry.arabicMeaning,
                entry.books.joinToString(" "),
                entry.locations.joinToString(" ") { "${it.book} ${it.unit} ${it.lesson} ${it.page}" },
                entry.partsOfSpeech.joinToString(" "),
                entry.synonyms.joinToString(" "),
                entry.antonyms.joinToString(" "),
                entry.definition,
                entry.derivatives,
                entry.expressions,
                entry.notes,
                entry.importantNotes
            ).any { normalize(it).contains(q) }

            SearchField.WORD -> normalize(entry.word).contains(q)
            SearchField.ARABIC_MEANING -> normalize(entry.arabicMeaning).contains(q)
            SearchField.BOOK -> {
                entry.books.any { normalize(it).contains(q) } ||
                    entry.locations.any { normalize(it.book).contains(q) }
            }
            SearchField.LOCATION -> {
                entry.locations.any {
                    normalize(it.unit).contains(q) ||
                        normalize(it.lesson).contains(q) ||
                        normalize(it.page).contains(q) ||
                        normalize(it.book).contains(q)
                }
            }
            SearchField.PART_OF_SPEECH -> entry.partsOfSpeech.any { normalize(it).contains(q) }
            SearchField.SYNONYMS -> entry.synonyms.any { normalize(it).contains(q) }
            SearchField.ANTONYMS -> entry.antonyms.any { normalize(it).contains(q) }
            SearchField.DEFINITION -> normalize(entry.definition).contains(q)
            SearchField.DERIVATIVES -> normalize(entry.derivatives).contains(q)
            SearchField.EXPRESSIONS -> normalize(entry.expressions).contains(q)
            SearchField.NOTES -> {
                normalize(entry.notes).contains(q) || normalize(entry.importantNotes).contains(q)
            }
        }
    }

    private fun matchesAdvancedFilter(entry: WordEntry, f: AdvancedSearchFilter): Boolean {
        if (!f.isActive) return true

        if (f.selectedBooks.isNotEmpty()) {
            val entryBooks = (entry.books + entry.locations.map { it.book })
                .map { normalize(it) }
                .filter { it.isNotEmpty() }
            val matched = f.selectedBooks.any { sel -> entryBooks.contains(normalize(sel)) }
            if (!matched) return false
        }

        if (f.unitQuery.isNotBlank()) {
            val uq = normalize(f.unitQuery)
            if (entry.locations.none { normalize(it.unit).contains(uq) }) return false
        }

        if (f.lessonQuery.isNotBlank()) {
            val lq = normalize(f.lessonQuery)
            if (entry.locations.none { normalize(it.lesson).contains(lq) }) return false
        }

        if (f.pageQuery.isNotBlank()) {
            val pq = normalize(f.pageQuery)
            if (entry.locations.none { normalize(it.page).contains(pq) }) return false
        }

        if (f.selectedPartsOfSpeech.isNotEmpty()) {
            val entryPos = entry.partsOfSpeech.map { normalize(it) }
            val matched = f.selectedPartsOfSpeech.any { sel -> entryPos.contains(normalize(sel)) }
            if (!matched) return false
        }

        if (f.wordQuery.isNotBlank()) {
            if (!normalize(entry.word).contains(normalize(f.wordQuery))) return false
        }

        if (f.meaningQuery.isNotBlank()) {
            if (!normalize(entry.arabicMeaning).contains(normalize(f.meaningQuery))) return false
        }

        if (f.synonymAntonymQuery.isNotBlank()) {
            val saq = normalize(f.synonymAntonymQuery)
            val inSyn = entry.synonyms.any { normalize(it).contains(saq) }
            val inAnt = entry.antonyms.any { normalize(it).contains(saq) }
            if (!inSyn && !inAnt) return false
        }

        if (f.favoritesOnly && !entry.isFavorite) return false
        if (f.hasNotesOnly && entry.notes.isBlank() && entry.importantNotes.isBlank()) return false
        if (f.hasExpressionsOnly && entry.expressions.isBlank()) return false

        return true
    }

    private fun applySorting(list: List<WordEntry>, sortOption: SortOption): List<WordEntry> {
        return when (sortOption) {
            SortOption.NEWEST -> list.sortedByDescending { it.createdAt }
            SortOption.OLDEST -> list.sortedBy { it.createdAt }
            SortOption.ALPHA_ASC -> list.sortedBy { it.displayTitle().lowercase() }
            SortOption.ALPHA_DESC -> list.sortedByDescending { it.displayTitle().lowercase() }
            SortOption.UNIT_ASC -> list.sortedWith(
                compareBy<WordEntry> { it.firstUnitNumberOrNull() ?: Int.MAX_VALUE }
                    .thenBy { it.firstPageNumberOrNull() ?: Int.MAX_VALUE }
                    .thenBy { it.displayTitle().lowercase() }
            )
            SortOption.PAGE_ASC -> list.sortedWith(
                compareBy<WordEntry> { it.firstPageNumberOrNull() ?: Int.MAX_VALUE }
                    .thenBy { it.displayTitle().lowercase() }
            )
            SortOption.BOOK -> list.sortedWith(
                compareBy<WordEntry> { it.books.firstOrNull() ?: "\uFFFF" }
                    .thenBy { it.displayTitle().lowercase() }
            )
            SortOption.FAVORITES_FIRST -> list.sortedWith(
                compareByDescending<WordEntry> { it.isFavorite }
                    .thenByDescending { it.createdAt }
            )
        }
    }

    private fun normalize(text: String): String {
        return text.trim()
            .lowercase()
            .replace(Regex("[\u064B-\u065F\u0670]"), "")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ة', 'ه')
            .replace('ى', 'ي')
    }
}
