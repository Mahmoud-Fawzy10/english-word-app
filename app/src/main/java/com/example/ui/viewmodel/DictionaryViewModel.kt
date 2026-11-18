package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.OfflineTtsManager
import com.example.data.local.AppPreferences
import com.example.data.local.DailyStudyStats
import com.example.data.local.QamusiDatabase
import com.example.data.local.ThemeMode
import com.example.data.model.AdvancedSearchFilter
import com.example.data.model.BrowseGroupMode
import com.example.data.model.CurriculumDefaults
import com.example.data.model.DuplicatePolicy
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
import com.example.domain.SrsMaturity
import com.example.domain.SrsScheduler
import com.example.export.DictionaryExporter
import com.example.export.DictionaryImporter
import com.example.export.ExportFormat
import com.example.export.JsonBackupManager
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LearningStatistics(
    val totalWords: Int = 0,
    val newWords: Int = 0,
    val learningWords: Int = 0,
    val reviewingWords: Int = 0,
    val masteredWords: Int = 0,
    val dueToday: Int = 0,
    val totalReviewsAllTime: Int = 0,
    val overallAccuracyPercent: Int = 0,
    val reviewsToday: Int = 0,
    val streakDays: Int = 0,
    val bookDistribution: List<Pair<String, Int>> = emptyList(),
    val posDistribution: List<Pair<String, Int>> = emptyList()
)

data class DictionaryUiState(
    val allWordsCount: Int = 0,
    val favoriteCount: Int = 0,
    val dueReviewCount: Int = 0,
    val masteredCount: Int = 0,
    val allWords: List<WordEntry> = emptyList(),
    val filteredWords: List<WordEntry> = emptyList(),
    val duplicateGroups: List<List<WordEntry>> = emptyList(),
    val stats: LearningStatistics = LearningStatistics(),
    val availableBooks: List<String> = CurriculumDefaults.DEFAULT_BOOKS,
    val availablePartsOfSpeech: List<String> = CurriculumDefaults.DEFAULT_PARTS_OF_SPEECH,
    val availableUnits: List<String> = emptyList(),
    val availableLessons: List<String> = emptyList(),
    val searchQuery: String = "",
    val searchField: SearchField = SearchField.ALL,
    val quickBookFilter: String? = null,
    val browseGroupMode: BrowseGroupMode = BrowseGroupMode.FLAT_LIST,
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
    val requeuedWordIds: Set<Long> = emptySet(),
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
    private val preferences = AppPreferences(application)
    private val ttsManager = OfflineTtsManager(application)

    init {
        val db = QamusiDatabase.getInstance(application)
        repository = WordRepository(db.wordDao())
    }

    private val _themeMode = MutableStateFlow(preferences.getThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dailyStats = MutableStateFlow(preferences.getDailyStudyStats())
    private val _timeRefreshTick = MutableStateFlow(System.currentTimeMillis())

    private val _searchQuery = MutableStateFlow("")
    private val _searchField = MutableStateFlow(SearchField.ALL)
    private val _quickBookFilter = MutableStateFlow<String?>(null)
    private val _browseGroupMode = MutableStateFlow(BrowseGroupMode.FLAT_LIST)
    private val _advancedFilter = MutableStateFlow(AdvancedSearchFilter())
    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    private val _isBusy = MutableStateFlow(false)

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
        _browseGroupMode,
        _advancedFilter,
        _sortOption,
        _isBusy,
        _dailyStats,
        _timeRefreshTick
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allWords = values[0] as List<WordEntry>
        val query = values[1] as String
        val field = values[2] as SearchField
        val quickBook = values[3] as String?
        val groupMode = values[4] as BrowseGroupMode
        val advFilter = values[5] as AdvancedSearchFilter
        val sort = values[6] as SortOption
        val busy = values[7] as Boolean
        val daily = values[8] as DailyStudyStats
        val now = values[9] as Long

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

        val sorted = if (query.isNotBlank()) {
            applySearchRelevanceSort(filtered, query, sort)
        } else {
            applySorting(filtered, sort)
        }

        val stats = buildLearningStats(allWords, daily, now)
        val duplicateGroups = repository.findDuplicateGroups(allWords)

        DictionaryUiState(
            allWordsCount = allWords.size,
            favoriteCount = allWords.count { it.isFavorite },
            dueReviewCount = stats.dueToday,
            masteredCount = stats.masteredWords,
            allWords = allWords,
            filteredWords = sorted,
            duplicateGroups = duplicateGroups,
            stats = stats,
            availableBooks = dynamicBooks,
            availablePartsOfSpeech = dynamicPos,
            availableUnits = dynamicUnits,
            availableLessons = dynamicLessons,
            searchQuery = query,
            searchField = field,
            quickBookFilter = quickBook,
            browseGroupMode = groupMode,
            advancedFilter = advFilter,
            sortOption = sort,
            isExporting = busy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DictionaryUiState()
    )

    override fun onCleared() {
        ttsManager.shutdown()
        super.onCleared()
    }

    fun refreshDueClock() {
        _timeRefreshTick.value = System.currentTimeMillis()
        _dailyStats.value = preferences.getDailyStudyStats()
    }

    fun setThemeMode(mode: ThemeMode) {
        preferences.setThemeMode(mode)
        _themeMode.value = mode
    }

    fun cycleThemeMode() {
        val next = when (_themeMode.value) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        setThemeMode(next)
    }

    fun speakWordOffline(text: String) {
        if (text.isBlank()) return
        val ok = ttsManager.speakOffline(text)
        if (!ok) {
            viewModelScope.launch {
                _events.emit(
                    UiEvent.ShowMessage("النطق الصوتي المحلي غير متاح حالياً على هذا الجهاز (تأكد من تفعيل محرك الصوت دون نت)")
                )
            }
        }
    }

    fun observeWord(id: Long): Flow<WordEntry?> = repository.observeWordById(id)

    suspend fun getWordOnce(id: Long): WordEntry? = repository.getWordOnce(id)

    suspend fun findDuplicatesFor(word: String, arabicMeaning: String, excludeId: Long): List<WordEntry> {
        return withContext(Dispatchers.IO) {
            repository.findDuplicatesFor(word, arabicMeaning, excludeId)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSearchField(field: SearchField) {
        _searchField.value = field
    }

    fun toggleQuickBookFilter(book: String?) {
        _quickBookFilter.value = if (_quickBookFilter.value == book) null else book
    }

    fun updateBrowseGroupMode(mode: BrowseGroupMode) {
        _browseGroupMode.value = mode
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

    fun saveWord(
        entry: WordEntry,
        mergeTarget: WordEntry? = null,
        onSaved: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val cleaned = cleanEntry(entry)
            val savedId = if (mergeTarget != null) {
                repository.mergeEntries(mergeTarget, cleaned).id
            } else {
                repository.saveWord(cleaned)
            }
            _events.emit(
                UiEvent.ShowMessage(
                    if (mergeTarget != null) "تم دمج البيانات مع الكلمة الموجودة" else "تم حفظ الكلمة في قاموسك"
                )
            )
            onSaved(savedId)
        }
    }

    fun mergeDuplicateGroup(group: List<WordEntry>) {
        viewModelScope.launch {
            repository.mergeDuplicateGroup(group)
            _events.emit(UiEvent.ShowMessage("تم دمج الكلمات المكررة بنجاح"))
        }
    }

    fun mergeAllDuplicates() {
        viewModelScope.launch {
            val groups = uiState.value.duplicateGroups
            if (groups.isEmpty()) return@launch
            groups.forEach { repository.mergeDuplicateGroup(it) }
            _events.emit(UiEvent.ShowMessage("تم دمج جميع المجموعات المكررة (${groups.size})"))
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

    fun startQuiz(overrideConfig: QuizConfig? = null): Boolean {
        val activeConfig = overrideConfig ?: _quizConfig.value
        if (overrideConfig != null) _quizConfig.value = overrideConfig

        val pool = uiState.value.allWords
        val matched = QuizEngine.filterWordsForConfig(pool, activeConfig)
        if (matched.isEmpty()) {
            viewModelScope.launch {
                val msg = when (activeConfig.questionType) {
                    QuizQuestionType.SYNONYMS -> "لا توجد كلمات تحتوي على مرادفات ضمن هذا النطاق"
                    QuizQuestionType.ANTONYMS -> "لا توجد كلمات تحتوي على مضادات ضمن هذا النطاق"
                    QuizQuestionType.MULTIPLE_CHOICE, QuizQuestionType.TYPING ->
                        "لا توجد كلمات تحتوي على الكلمة والمعنى معاً ضمن هذا النطاق"
                    else -> "لا توجد كلمات مطابقة للنطاق المحدد حالياً"
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
            requeueForgottenCards = false,
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
            _dailyStats.value = preferences.recordReviewActivity()
            _timeRefreshTick.value = System.currentTimeMillis()
        }
        val record = QuizAnswerRecord(
            entry = question.entry,
            isCorrect = grade != SrsGrade.AGAIN,
            grade = grade
        )

        val shouldRequeue = grade == SrsGrade.AGAIN &&
            session.config.requeueForgottenCards &&
            !session.requeuedWordIds.contains(question.entry.id) &&
            session.questions.size > 1

        val updatedQuestions = if (shouldRequeue) {
            session.questions + question
        } else {
            session.questions
        }
        val updatedRequeued = if (shouldRequeue) {
            session.requeuedWordIds + question.entry.id
        } else {
            session.requeuedWordIds
        }

        advanceWithRecord(
            session = session.copy(questions = updatedQuestions, requeuedWordIds = updatedRequeued),
            record = record
        )
    }

    fun submitMcqOption(option: String) {
        val session = _quizSession.value ?: return
        if (session.selectedMcqOption != null) return
        val question = session.currentQuestion ?: return
        val isCorrect = question.acceptedAnswers.any {
            normalize(it) == normalize(option)
        } || normalize(option) == normalize(question.answerText)
        val grade = if (isCorrect) SrsGrade.GOOD else SrsGrade.AGAIN
        viewModelScope.launch {
            repository.recordSrsReview(question.entry, grade)
            _dailyStats.value = preferences.recordReviewActivity()
            _timeRefreshTick.value = System.currentTimeMillis()
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
        val normInput = normalize(session.typedInput)
        val isCorrect = normInput.isNotEmpty() && (
            question.acceptedAnswers.any { normalize(it) == normInput } ||
                normalize(question.answerText) == normInput
            )
        val grade = if (isCorrect) SrsGrade.GOOD else SrsGrade.AGAIN
        viewModelScope.launch {
            repository.recordSrsReview(question.entry, grade)
            _dailyStats.value = preferences.recordReviewActivity()
            _timeRefreshTick.value = System.currentTimeMillis()
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
        refreshDueClock()
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
            _timeRefreshTick.value = System.currentTimeMillis()
            val msg = when {
                resetProgress -> "تمت إعادة ضبط جدول المراجعة للكلمة"
                intervalDays == 0 -> "تم ضبط الكلمة للمراجعة الفورية الآن"
                else -> "تم تحديث موعد المراجعة القادمة بعد $intervalDays يوم"
            }
            _events.emit(UiEvent.ShowMessage(msg))
        }
    }

    // -------------------------------------------------------------------------
    // Offline Export, Import (CSV/XLSX) & JSON Backup/Restore
    // -------------------------------------------------------------------------
    fun exportAndShare(format: ExportFormat, onlyFiltered: Boolean) {
        viewModelScope.launch {
            val wordsToExport = if (onlyFiltered) uiState.value.filteredWords else repository.getAllWordsOnce()
            if (wordsToExport.isEmpty()) {
                _events.emit(UiEvent.ShowMessage("لا توجد كلمات لتصديرها حالياً"))
                return@launch
            }
            _isBusy.value = true
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
                _isBusy.value = false
            }
        }
    }

    fun exportToPickedUri(uri: Uri, format: ExportFormat, onlyFiltered: Boolean) {
        viewModelScope.launch {
            val wordsToExport = if (onlyFiltered) uiState.value.filteredWords else repository.getAllWordsOnce()
            if (wordsToExport.isEmpty()) {
                _events.emit(UiEvent.ShowMessage("لا توجد كلمات لتصديرها حالياً"))
                return@launch
            }
            _isBusy.value = true
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
                _isBusy.value = false
            }
        }
    }

    fun importSpreadsheetFromUri(uri: Uri, isXlsx: Boolean, policy: DuplicatePolicy) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val summary = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                        val parsed = if (isXlsx) {
                            DictionaryImporter.parseXlsx(stream)
                        } else {
                            DictionaryImporter.parseCsv(stream)
                        }
                        if (parsed.isEmpty()) {
                            error("لم يتم العثور على كلمات صالحة داخل الملف")
                        }
                        repository.importWords(parsed, policy)
                    } ?: error("تعذر قراءة الملف المحدد")
                }
                _events.emit(
                    UiEvent.ShowMessage(
                        "تم الاستيراد: أُضيف ${summary.insertedCount} • دُمج ${summary.mergedCount} • تُخطي ${summary.skippedCount}"
                    )
                )
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage("فشل الاستيراد: ${e.localizedMessage ?: "ملف غير صالح"}"))
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun exportJsonBackupToUri(uri: Uri) {
        viewModelScope.launch {
            val words = repository.getAllWordsOnce()
            if (words.isEmpty()) {
                _events.emit(UiEvent.ShowMessage("القاموس فارغ حالياً"))
                return@launch
            }
            _isBusy.value = true
            try {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        JsonBackupManager.exportBackup(words, out)
                    } ?: error("تعذر إنشاء ملف النسخة الاحتياطية")
                }
                _events.emit(UiEvent.ShowMessage("تم حفظ النسخة الاحتياطية JSON (${words.size} كلمة)"))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage("تعذر حفظ النسخة الاحتياطية: ${e.localizedMessage ?: "خطأ"}"))
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun restoreJsonBackupFromUri(uri: Uri, replaceExisting: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val summary = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                        val restored = JsonBackupManager.parseBackup(stream)
                        if (restored.isEmpty()) error("ملف النسخة الاحتياطية فارغ")
                        repository.restoreFullJsonBackup(restored, replaceExisting)
                    } ?: error("تعذر قراءة ملف النسخة الاحتياطية")
                }
                val msg = if (replaceExisting) {
                    "تمت استعادة النسخة الاحتياطية (${summary.insertedCount} كلمة)"
                } else {
                    "تمت الاستعادة والدمج: أُضيف ${summary.insertedCount} • دُمج ${summary.mergedCount}"
                }
                _events.emit(UiEvent.ShowMessage(msg))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage("تعذر استعادة النسخة: ${e.localizedMessage ?: "ملف غير صالح"}"))
            } finally {
                _isBusy.value = false
            }
        }
    }

    // -------------------------------------------------------------------------
    // Search, Relevance, Sorting & Analytics Helpers
    // -------------------------------------------------------------------------
    private fun cleanEntry(entry: WordEntry): WordEntry = entry.copy(
        word = entry.word.trim(),
        ipa = entry.ipa.trim(),
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

    private fun buildLearningStats(
        allWords: List<WordEntry>,
        daily: DailyStudyStats,
        now: Long
    ): LearningStatistics {
        var newW = 0
        var learningW = 0
        var reviewingW = 0
        var masteredW = 0
        var dueW = 0
        var totalRev = 0
        var totalCorrect = 0

        val bookCounts = mutableMapOf<String, Int>()
        val posCounts = mutableMapOf<String, Int>()

        for (w in allWords) {
            when (SrsScheduler.maturityOf(w)) {
                SrsMaturity.NEW -> newW++
                SrsMaturity.LEARNING -> learningW++
                SrsMaturity.REVIEWING -> reviewingW++
                SrsMaturity.MASTERED -> masteredW++
            }
            if (w.isDueForReview(now)) dueW++
            totalRev += w.srsTotalReviews
            totalCorrect += w.srsCorrectCount

            val uniqueBooks = (w.books + w.locations.map { it.book })
                .map { it.trim() }.filter { it.isNotBlank() }.distinct()
            if (uniqueBooks.isEmpty()) {
                bookCounts["غير محدد"] = (bookCounts["غير محدد"] ?: 0) + 1
            } else {
                uniqueBooks.forEach { b -> bookCounts[b] = (bookCounts[b] ?: 0) + 1 }
            }

            if (w.partsOfSpeech.isEmpty()) {
                posCounts["غير مصنف"] = (posCounts["غير مصنف"] ?: 0) + 1
            } else {
                w.partsOfSpeech.forEach { p -> posCounts[p] = (posCounts[p] ?: 0) + 1 }
            }
        }

        val accuracy = if (totalRev > 0) ((totalCorrect * 100) / totalRev).coerceIn(0, 100) else 0

        return LearningStatistics(
            totalWords = allWords.size,
            newWords = newW,
            learningWords = learningW,
            reviewingWords = reviewingW,
            masteredWords = masteredW,
            dueToday = dueW,
            totalReviewsAllTime = totalRev,
            overallAccuracyPercent = accuracy,
            reviewsToday = daily.reviewsToday,
            streakDays = daily.currentStreakDays,
            bookDistribution = bookCounts.entries.sortedByDescending { it.value }.map { it.key to it.value },
            posDistribution = posCounts.entries.sortedByDescending { it.value }.map { it.key to it.value }
        )
    }

    private fun matchesSearch(entry: WordEntry, rawQuery: String, field: SearchField): Boolean {
        val norm = normalize(rawQuery)
        if (norm.isEmpty()) return true
        val tokens = norm.split(' ').filter { it.isNotBlank() }

        val targetStrings: List<String> = when (field) {
            SearchField.ALL -> listOf(
                entry.word,
                entry.ipa,
                entry.arabicMeaning,
                entry.books.joinToString(" "),
                entry.locations.joinToString(" ") { "${it.book} ${it.unit} ${it.lesson} ${it.page} وحدة ${it.unit} درس ${it.lesson} ص ${it.page}" },
                entry.partsOfSpeech.joinToString(" "),
                entry.synonyms.joinToString(" "),
                entry.antonyms.joinToString(" "),
                entry.definition,
                entry.derivatives,
                entry.expressions,
                entry.notes,
                entry.importantNotes
            )
            SearchField.WORD -> listOf(entry.word)
            SearchField.IPA -> listOf(entry.ipa)
            SearchField.ARABIC_MEANING -> listOf(entry.arabicMeaning)
            SearchField.BOOK -> entry.books + entry.locations.map { it.book }
            SearchField.LOCATION -> entry.locations.map {
                "${it.book} ${it.unit} ${it.lesson} ${it.page} وحدة ${it.unit} درس ${it.lesson} ص ${it.page}"
            }
            SearchField.PART_OF_SPEECH -> entry.partsOfSpeech
            SearchField.SYNONYMS -> entry.synonyms
            SearchField.ANTONYMS -> entry.antonyms
            SearchField.DEFINITION -> listOf(entry.definition)
            SearchField.DERIVATIVES -> listOf(entry.derivatives)
            SearchField.EXPRESSIONS -> listOf(entry.expressions)
            SearchField.NOTES -> listOf(entry.notes, entry.importantNotes)
        }

        val haystack = normalize(targetStrings.joinToString(" "))
        return tokens.all { token -> haystack.contains(token) }
    }

    private fun applySearchRelevanceSort(
        list: List<WordEntry>,
        rawQuery: String,
        fallbackSort: SortOption
    ): List<WordEntry> {
        val q = normalize(rawQuery)
        val secondary = applySorting(list, fallbackSort)
        return secondary.sortedByDescending { entry ->
            val w = normalize(entry.word)
            val m = normalize(entry.arabicMeaning)
            when {
                w == q || m == q -> 4
                w.startsWith(q) || m.startsWith(q) -> 3
                w.contains(q) || m.contains(q) -> 2
                else -> 1
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

    private fun normalize(text: String): String = WordRepository.normalizeKey(text)
}
