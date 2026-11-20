package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.OfflineTtsManager
import com.example.data.local.AppPreferences
import com.example.data.local.DailyHistoryPoint
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
import com.example.data.model.WordFormConfig
import com.example.data.model.WordRevision
import com.example.data.repository.ImportSummary
import com.example.data.repository.SaveWordResult
import com.example.data.repository.WordRepository
import com.example.domain.AchievementBadge
import com.example.domain.DailyChallengeMission
import com.example.domain.LearningAnalyticsEngine
import com.example.domain.QuizAnswerRecord
import com.example.domain.QuizConfig
import com.example.domain.QuizDirection
import com.example.domain.QuizEngine
import com.example.domain.QuizQuestionItem
import com.example.domain.QuizQuestionType
import com.example.domain.QuizScopeMode
import com.example.domain.SentencePracticePrompt
import com.example.domain.SrsGrade
import com.example.domain.SrsMaturity
import com.example.domain.SrsScheduler
import com.example.domain.StudyGroupSummary
import com.example.domain.StudyMapBookNode
import com.example.domain.StudyMapBuilder
import com.example.domain.UserRankInfo
import com.example.export.DictionaryExporter
import com.example.export.DictionaryImporter
import com.example.export.ExportFormat
import com.example.export.JsonBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

data class LearningStatistics(
    val totalWords: Int = 0,
    val newWords: Int = 0,
    val learningWords: Int = 0,
    val reviewingWords: Int = 0,
    val masteredWords: Int = 0,
    val masteryPercent: Int = 0,
    val dueToday: Int = 0,
    val hardOrMistakesCount: Int = 0,
    val incompleteWordsCount: Int = 0,
    val totalReviewsAllTime: Int = 0,
    val overallAccuracyPercent: Int = 0,
    val reviewsToday: Int = 0,
    val miniQuizzesToday: Int = 0,
    val streakDays: Int = 0,
    val dailyGoalTarget: Int = 15,
    val bookDistribution: List<Pair<String, Int>> = emptyList(),
    val posDistribution: List<Pair<String, Int>> = emptyList()
)

data class AudioPlaylistState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val currentIndex: Int = 0,
    val queue: List<WordEntry> = emptyList(),
    val repeatWordTwice: Boolean = false
) {
    val currentWord: WordEntry? get() = queue.getOrNull(currentIndex)
}

data class DictionaryUiState(
    val allWordsCount: Int = 0,
    val favoriteCount: Int = 0,
    val dueReviewCount: Int = 0,
    val masteredCount: Int = 0,
    val masteryPercent: Int = 0,
    val hardOrMistakesCount: Int = 0,
    val incompleteWordsCount: Int = 0,
    val allWords: List<WordEntry> = emptyList(),
    val filteredWords: List<WordEntry> = emptyList(),
    val duplicateGroups: List<List<WordEntry>> = emptyList(),
    val stats: LearningStatistics = LearningStatistics(),
    val availableBooks: List<String> = CurriculumDefaults.DEFAULT_BOOKS,
    val availablePartsOfSpeech: List<String> = CurriculumDefaults.DEFAULT_PARTS_OF_SPEECH,
    val availableStudyTags: List<String> = CurriculumDefaults.DEFAULT_STUDY_TAGS,
    val availableUnits: List<String> = emptyList(),
    val availableLessons: List<String> = emptyList(),
    val searchQuery: String = "",
    val searchField: SearchField = SearchField.ALL,
    val quickBookFilter: String? = null,
    val browseGroupMode: BrowseGroupMode = BrowseGroupMode.FLAT_LIST,
    val advancedFilter: AdvancedSearchFilter = AdvancedSearchFilter(),
    val sortOption: SortOption = SortOption.NEWEST,
    val isExporting: Boolean = false,
    val wordOfTheDay: WordEntry? = null,
    val curriculumMap: List<StudyMapBookNode> = emptyList(),
    val studyGroups: List<StudyGroupSummary> = emptyList(),
    val dailyChallenges: List<DailyChallengeMission> = emptyList(),
    val achievements: List<AchievementBadge> = emptyList(),
    val userRank: UserRankInfo = UserRankInfo("بداية القاموس", 10, 0),
    val dailyHistory: List<DailyHistoryPoint> = emptyList(),
    val hardestWords: List<WordEntry> = emptyList(),
    val sentencePrompts: List<SentencePracticePrompt> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val formConfig: WordFormConfig = WordFormConfig(),
    val dailyGoalTarget: Int = 15,
    val ttsSpeechRate: Float = 0.92f,
    val selectedWordIds: Set<Long> = emptySet(),
    val exactSearchMatch: WordEntry? = null
) {
    val isSearchOrFilterActive: Boolean
        get() = searchQuery.isNotBlank() || quickBookFilter != null || advancedFilter.isActive

    val hasNoSearchResults: Boolean
        get() = allWordsCount > 0 && filteredWords.isEmpty() && isSearchOrFilterActive

    val isSelectionMode: Boolean
        get() = selectedWordIds.isNotEmpty()
}

data class QuizSessionState(
    val config: QuizConfig,
    val questions: List<QuizQuestionItem>,
    val currentIndex: Int = 0,
    val isAnswerRevealed: Boolean = false,
    val selectedMcqOption: String? = null,
    val typedInput: String = "",
    val lastTypingCorrect: Boolean? = null,
    val scrambleAnswer: String = "",
    val lastScrambleCorrect: Boolean? = null,
    val matchRoundCompleted: Boolean = false,
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

private data class FilterInputs(
    val query: String,
    val field: SearchField,
    val quickBook: String?,
    val groupMode: BrowseGroupMode,
    val advFilter: AdvancedSearchFilter,
    val sort: SortOption
)

private data class StudyPrefsSnapshot(
    val daily: DailyStudyStats,
    val now: Long,
    val formConfig: WordFormConfig,
    val recentSearches: List<String>,
    val ttsRate: Float,
    val selectedIds: Set<Long>,
    val isBusy: Boolean
)

class DictionaryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WordRepository
    private val preferences = AppPreferences(application)
    private val ttsManager = OfflineTtsManager(application)

    init {
        val db = QamusiDatabase.getInstance(application)
        repository = WordRepository(db.wordDao(), db.wordRevisionDao())
        ttsManager.setSpeechRate(preferences.getTtsSpeechRate())
    }

    private val _themeMode = MutableStateFlow(preferences.getThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dailyStats = MutableStateFlow(preferences.getDailyStudyStats())
    private val _timeRefreshTick = MutableStateFlow(System.currentTimeMillis())
    private val _formConfig = MutableStateFlow(preferences.getFormConfig())
    private val _recentSearches = MutableStateFlow(preferences.getRecentSearches())
    private val _ttsSpeechRate = MutableStateFlow(preferences.getTtsSpeechRate())
    private val _selectedWordIds = MutableStateFlow<Set<Long>>(emptySet())

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

    private val _audioPlaylist = MutableStateFlow(AudioPlaylistState())
    val audioPlaylist: StateFlow<AudioPlaylistState> = _audioPlaylist.asStateFlow()
    private var audioPlaylistJob: Job? = null

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val filterInputsFlow = combine(
        _searchQuery,
        _searchField,
        _quickBookFilter,
        _browseGroupMode,
        _advancedFilter
    ) { q, f, qb, gm, adv ->
        FilterInputs(q, f, qb, gm, adv, SortOption.NEWEST)
    }.combine(_sortOption) { base, sort ->
        base.copy(sort = sort)
    }

    private val studyPrefsFlow = combine(
        _dailyStats,
        _timeRefreshTick,
        _formConfig,
        _recentSearches,
        _ttsSpeechRate
    ) { daily, now, fc, recents, rate ->
        StudyPrefsSnapshot(
            daily = daily,
            now = now,
            formConfig = fc,
            recentSearches = recents,
            ttsRate = rate,
            selectedIds = emptySet(),
            isBusy = false
        )
    }.combine(_selectedWordIds) { snap, sel ->
        snap.copy(selectedIds = sel)
    }.combine(_isBusy) { snap, busy ->
        snap.copy(isBusy = busy)
    }

    val uiState: StateFlow<DictionaryUiState> = combine(
        repository.allWords,
        filterInputsFlow,
        studyPrefsFlow
    ) { allWords, filters, prefs ->
        val query = filters.query
        val field = filters.field
        val quickBook = filters.quickBook
        val groupMode = filters.groupMode
        val advFilter = filters.advFilter
        val sort = filters.sort

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

        val dynamicTags = (CurriculumDefaults.DEFAULT_STUDY_TAGS +
            allWords.flatMap { it.studyTags })
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

        val normQ = normalize(query)
        val exactMatch = if (normQ.isNotEmpty()) {
            allWords.firstOrNull {
                normalize(it.word) == normQ || normalize(it.arabicMeaning) == normQ
            }
        } else {
            null
        }

        val stats = buildLearningStats(allWords, prefs.daily, prefs.now)
        val duplicateGroups = repository.findDuplicateGroups(allWords)
        val validSelection = prefs.selectedIds.intersect(allWords.map { it.id }.toSet())
        val achievements = LearningAnalyticsEngine.buildAchievements(
            words = allWords,
            totalReviewsAllTime = stats.totalReviewsAllTime,
            overallAccuracyPercent = stats.overallAccuracyPercent,
            streakDays = stats.streakDays,
            miniQuizzesToday = stats.miniQuizzesToday,
            duplicateGroupsCount = duplicateGroups.size
        )
        val userRank = LearningAnalyticsEngine.computeRank(allWords.size)
        val dailyHistory = preferences.getLast7DaysHistory(stats.masteryPercent, prefs.now)
        val hardestWords = allWords
            .filter { it.isHardOrMistake() }
            .sortedWith(compareByDescending<WordEntry> { it.difficulty }.thenByDescending { it.lastWrongAt })
            .take(8)
        val sentencePrompts = LearningAnalyticsEngine.buildSentencePrompts(allWords, limit = 6)

        DictionaryUiState(
            allWordsCount = allWords.size,
            favoriteCount = allWords.count { it.isFavorite },
            dueReviewCount = stats.dueToday,
            masteredCount = stats.masteredWords,
            masteryPercent = stats.masteryPercent,
            hardOrMistakesCount = stats.hardOrMistakesCount,
            incompleteWordsCount = stats.incompleteWordsCount,
            allWords = allWords,
            filteredWords = sorted,
            duplicateGroups = duplicateGroups,
            stats = stats,
            availableBooks = dynamicBooks,
            availablePartsOfSpeech = dynamicPos,
            availableStudyTags = dynamicTags,
            availableUnits = dynamicUnits,
            availableLessons = dynamicLessons,
            searchQuery = query,
            searchField = field,
            quickBookFilter = quickBook,
            browseGroupMode = groupMode,
            advancedFilter = advFilter,
            sortOption = sort,
            isExporting = prefs.isBusy,
            wordOfTheDay = StudyMapBuilder.pickWordOfTheDay(allWords, prefs.now),
            curriculumMap = StudyMapBuilder.buildCurriculumMap(allWords, prefs.now),
            studyGroups = StudyMapBuilder.buildStudyGroups(allWords, prefs.now),
            dailyChallenges = StudyMapBuilder.buildDailyChallenges(allWords, prefs.daily, prefs.now),
            achievements = achievements,
            userRank = userRank,
            dailyHistory = dailyHistory,
            hardestWords = hardestWords,
            sentencePrompts = sentencePrompts,
            recentSearches = prefs.recentSearches,
            formConfig = prefs.formConfig,
            dailyGoalTarget = prefs.daily.dailyGoalTarget,
            ttsSpeechRate = prefs.ttsRate,
            selectedWordIds = validSelection,
            exactSearchMatch = exactMatch
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DictionaryUiState()
    )

    override fun onCleared() {
        stopAudioPlaylist()
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

    fun setDailyGoalTarget(target: Int) {
        preferences.setDailyGoalTarget(target)
        _dailyStats.value = preferences.getDailyStudyStats()
    }

    fun updateFormConfig(config: WordFormConfig) {
        preferences.setFormConfig(config)
        _formConfig.value = config
    }

    fun setTtsSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.6f, 1.4f)
        preferences.setTtsSpeechRate(clamped)
        _ttsSpeechRate.value = clamped
        ttsManager.setSpeechRate(clamped)
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

    // -------------------------------------------------------------------------
    // Hands-Free Offline Audio Study Playlist
    // -------------------------------------------------------------------------
    fun startAudioPlaylist(words: List<WordEntry>, repeatWordTwice: Boolean = false) {
        val speakable = words.filter { it.word.isNotBlank() }
        if (speakable.isEmpty()) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage("لا توجد كلمات إنجليزية قابلة للنطق في القائمة الحالية"))
            }
            return
        }
        if (!ttsManager.isAvailable()) {
            viewModelScope.launch {
                _events.emit(UiEvent.ShowMessage("محرك النطق المحلي (TTS) غير متاح على هذا الجهاز"))
            }
            return
        }
        audioPlaylistJob?.cancel()
        _audioPlaylist.value = AudioPlaylistState(
            isActive = true,
            isPaused = false,
            currentIndex = 0,
            queue = speakable,
            repeatWordTwice = repeatWordTwice
        )
        launchPlaylistLoop()
    }

    fun togglePauseAudioPlaylist() {
        val cur = _audioPlaylist.value
        if (!cur.isActive) return
        if (cur.isPaused) {
            _audioPlaylist.value = cur.copy(isPaused = false)
            launchPlaylistLoop()
        } else {
            audioPlaylistJob?.cancel()
            ttsManager.stop()
            _audioPlaylist.value = cur.copy(isPaused = true)
        }
    }

    fun setAudioRepeatTwice(repeatTwice: Boolean) {
        _audioPlaylist.value = _audioPlaylist.value.copy(repeatWordTwice = repeatTwice)
    }

    fun skipAudioPlaylist(delta: Int) {
        val cur = _audioPlaylist.value
        if (!cur.isActive || cur.queue.isEmpty()) return
        val nextIdx = (cur.currentIndex + delta).coerceIn(0, cur.queue.lastIndex)
        audioPlaylistJob?.cancel()
        ttsManager.stop()
        _audioPlaylist.value = cur.copy(currentIndex = nextIdx, isPaused = false)
        launchPlaylistLoop()
    }

    fun stopAudioPlaylist() {
        audioPlaylistJob?.cancel()
        ttsManager.setUtteranceListener(null)
        ttsManager.stop()
        _audioPlaylist.value = AudioPlaylistState()
    }

    private fun launchPlaylistLoop() {
        audioPlaylistJob?.cancel()
        audioPlaylistJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val state = _audioPlaylist.value
                if (!state.isActive || state.isPaused || state.currentIndex >= state.queue.size) {
                    if (state.isActive && !state.isPaused && state.currentIndex >= state.queue.size) {
                        _audioPlaylist.value = AudioPlaylistState()
                    }
                    break
                }
                val entry = state.queue[state.currentIndex]
                speakAndAwait(entry.word)
                delay(650)
                if (_audioPlaylist.value.repeatWordTwice && _audioPlaylist.value.isActive && !_audioPlaylist.value.isPaused) {
                    speakAndAwait(entry.word)
                    delay(750)
                }
                val syn = entry.synonyms.firstOrNull()
                if (!syn.isNullOrBlank() && _audioPlaylist.value.isActive && !_audioPlaylist.value.isPaused) {
                    speakAndAwait(syn)
                    delay(650)
                }
                delay(900)
                val latest = _audioPlaylist.value
                if (!latest.isActive || latest.isPaused) break
                val nextIdx = latest.currentIndex + 1
                if (nextIdx >= latest.queue.size) {
                    _audioPlaylist.value = AudioPlaylistState()
                    break
                } else {
                    _audioPlaylist.value = latest.copy(currentIndex = nextIdx)
                }
            }
        }
    }

    private fun speakAndAwait(text: String) {
        val uid = "playlist_${System.nanoTime()}"
        val latch = CountDownLatch(1)
        ttsManager.setUtteranceListener { finishedId ->
            if (finishedId == uid) latch.countDown()
        }
        val started = ttsManager.speakOffline(text, uid)
        if (started) {
            runCatching { latch.await(6, TimeUnit.SECONDS) }
        }
    }

    // -------------------------------------------------------------------------
    // Dictionary Queries, Multi-Select Batch & Anti-Duplicate Persistence
    // -------------------------------------------------------------------------
    fun observeWord(id: Long): Flow<WordEntry?> = repository.observeWordById(id)

    fun observeRevisions(wordId: Long): Flow<List<WordRevision>> = repository.observeRevisions(wordId)

    fun restoreWordRevision(revisionId: Long) {
        viewModelScope.launch {
            val restored = withContext(Dispatchers.IO) {
                repository.restoreRevision(revisionId)
            }
            if (restored != null) {
                _events.emit(UiEvent.ShowMessage("تمت استعادة النسخة السابقة من «${restored.displayTitle()}»"))
            }
        }
    }

    suspend fun getWordOnce(id: Long): WordEntry? = repository.getWordOnce(id)

    suspend fun findDuplicatesFor(word: String, arabicMeaning: String, excludeId: Long): List<WordEntry> {
        return withContext(Dispatchers.IO) {
            repository.findDuplicatesFor(word, arabicMeaning, excludeId)
        }
    }

    fun findRelatedWords(target: WordEntry): List<WordEntry> {
        return repository.findRelatedWords(target, uiState.value.allWords)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun commitSearchHistory(query: String) {
        _recentSearches.value = preferences.addRecentSearch(query)
    }

    fun clearSearchHistory() {
        _recentSearches.value = preferences.clearRecentSearches()
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

    fun toggleFavoritesFilter() {
        val cur = _advancedFilter.value
        _advancedFilter.value = cur.copy(favoritesOnly = !cur.favoritesOnly)
    }

    fun toggleHardMistakesFilter() {
        val cur = _advancedFilter.value
        _advancedFilter.value = cur.copy(hardOrMistakesOnly = !cur.hardOrMistakesOnly)
    }

    fun toggleIncompleteFilter() {
        val cur = _advancedFilter.value
        _advancedFilter.value = cur.copy(incompleteOnly = !cur.incompleteOnly)
    }

    fun applyIncompleteOnlyFilter() {
        _searchQuery.value = ""
        _quickBookFilter.value = null
        _advancedFilter.value = AdvancedSearchFilter(incompleteOnly = true)
    }

    fun applyQuickTagFilter(tag: String) {
        _advancedFilter.value = _advancedFilter.value.copy(selectedStudyTags = setOf(tag))
    }

    fun applyUnitFilter(book: String, unit: String) {
        _searchQuery.value = ""
        _quickBookFilter.value = null
        _advancedFilter.value = AdvancedSearchFilter(
            selectedBooks = if (book == "كلمات عامة") emptySet() else setOf(book),
            unitQuery = if (unit == "بدون وحدة") "" else unit
        )
    }

    fun clearAllFilters() {
        _searchQuery.value = ""
        _quickBookFilter.value = null
        _advancedFilter.value = AdvancedSearchFilter()
    }

    fun updateSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun toggleWordSelection(wordId: Long) {
        val next = _selectedWordIds.value.toMutableSet()
        if (!next.add(wordId)) next.remove(wordId)
        _selectedWordIds.value = next
    }

    fun clearWordSelection() {
        _selectedWordIds.value = emptySet()
    }

    fun selectAllFilteredWords() {
        _selectedWordIds.value = uiState.value.filteredWords.map { it.id }.toSet()
    }

    fun batchSetFavorite(isFavorite: Boolean) {
        val ids = _selectedWordIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.setFavoriteBatch(ids, isFavorite)
            _selectedWordIds.value = emptySet()
            _events.emit(
                UiEvent.ShowMessage(
                    if (isFavorite) "تمت إضافة ${ids.size} كلمة للمفضلة" else "تمت إزالة ${ids.size} كلمة من المفضلة"
                )
            )
        }
    }

    fun batchSetDifficulty(difficulty: Int) {
        val ids = _selectedWordIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.setDifficultyBatch(ids, difficulty)
            _selectedWordIds.value = emptySet()
            _events.emit(UiEvent.ShowMessage("تم تحديث مستوى الصعوبة لـ ${ids.size} كلمة"))
        }
    }

    fun batchAddStudyTag(tag: String) {
        val ids = _selectedWordIds.value.toList()
        if (ids.isEmpty() || tag.isBlank()) return
        viewModelScope.launch {
            repository.addStudyTagBatch(ids, tag)
            _selectedWordIds.value = emptySet()
            _events.emit(UiEvent.ShowMessage("تمت إضافة ${ids.size} كلمة إلى مجموعة «$tag»"))
        }
    }

    fun batchDeleteSelected() {
        val ids = _selectedWordIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteWordsBatch(ids)
            _selectedWordIds.value = emptySet()
            _events.emit(UiEvent.ShowMessage("تم حذف ${ids.size} كلمة من قاموسك"))
        }
    }

    fun startQuizWithSelectedWords(): Boolean {
        val ids = _selectedWordIds.value
        val words = uiState.value.allWords.filter { ids.contains(it.id) }
        if (words.isEmpty()) return false
        _selectedWordIds.value = emptySet()
        val cfg = QuizConfig(
            scopeMode = QuizScopeMode.RANDOM_RANGE,
            questionType = QuizQuestionType.FLASHCARD_SRS,
            questionLimit = words.size
        )
        val questions = QuizEngine.buildQuizQuestions(words, uiState.value.allWords, cfg)
        _quizSession.value = QuizSessionState(config = cfg, questions = questions)
        return questions.isNotEmpty()
    }

    fun saveWord(
        entry: WordEntry,
        mergeTarget: WordEntry? = null,
        onSaved: (Long) -> Unit = {},
        onDuplicateRejected: (WordEntry) -> Unit = {}
    ) {
        viewModelScope.launch {
            val cleaned = cleanEntry(entry)
            when (val res = repository.saveWordChecked(cleaned, mergeTarget)) {
                is SaveWordResult.Saved -> {
                    _events.emit(UiEvent.ShowMessage("تم حفظ الكلمة في قاموسك"))
                    onSaved(res.id)
                }
                is SaveWordResult.Merged -> {
                    _events.emit(UiEvent.ShowMessage("تم دمج البيانات مع الكلمة الموجودة بدون تكرار"))
                    onSaved(res.id)
                }
                is SaveWordResult.DuplicateRejected -> {
                    _events.emit(
                        UiEvent.ShowMessage(
                            "مرفوض التكرار: «${res.existing.displayTitle()}» موجودة بالفعل في قاموسك"
                        )
                    )
                    onDuplicateRejected(res.existing)
                }
                SaveWordResult.EmptyRejected -> {
                    _events.emit(UiEvent.ShowMessage("أدخل الكلمة أو المعنى أو أي معلومة واحدة على الأقل"))
                }
            }
        }
    }

    fun bulkQuickAdd(
        rawText: String,
        defaultBook: String?,
        defaultTag: String?,
        onDone: (ImportSummary) -> Unit = {}
    ) {
        viewModelScope.launch {
            val parsed = StudyMapBuilder.parseBulkQuickLines(rawText, defaultBook, defaultTag)
            if (parsed.isEmpty()) {
                _events.emit(UiEvent.ShowMessage("لم يتم التعرف على كلمات صالحة في النص المدخل"))
                return@launch
            }
            val summary = withContext(Dispatchers.IO) {
                repository.importWords(parsed, DuplicatePolicy.SKIP)
            }
            _events.emit(
                UiEvent.ShowMessage(
                    "الإضافة السريعة: أُضيف ${summary.insertedCount} • رُفض التكرار ${summary.skippedCount}"
                )
            )
            onDone(summary)
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
    // Spaced Repetition (SRS) & Quiz / Mini-Quiz Session Actions
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
                    QuizQuestionType.PREPOSITIONS_COLLOCATIONS -> "لا توجد كلمات تحتوي على حروف جر أو متلازمات ضمن هذا النطاق"
                    QuizQuestionType.LETTER_SCRAMBLE -> "لا توجد كلمات من 3 حروف فأكثر ضمن هذا النطاق"
                    QuizQuestionType.MULTIPLE_CHOICE,
                    QuizQuestionType.TYPING,
                    QuizQuestionType.MATCH_PAIRS -> "لا توجد كلمات تحتوي على الكلمة والمعنى معاً ضمن هذا النطاق"
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

        val updatedQuestions = if (shouldRequeue) session.questions + question else session.questions
        val updatedRequeued = if (shouldRequeue) session.requeuedWordIds + question.entry.id else session.requeuedWordIds

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

    fun submitScrambleAnswer(answer: String) {
        val session = _quizSession.value ?: return
        if (session.lastScrambleCorrect != null) return
        val question = session.currentQuestion ?: return
        val isCorrect = normalize(answer) == normalize(question.answerText)
        val grade = if (isCorrect) SrsGrade.GOOD else SrsGrade.AGAIN
        val isLast = session.currentIndex + 1 >= session.questions.size
        viewModelScope.launch {
            repository.recordSrsReview(question.entry, grade)
            _dailyStats.value = preferences.recordReviewActivity(incrementMiniQuiz = isLast)
            _timeRefreshTick.value = System.currentTimeMillis()
        }
        val record = QuizAnswerRecord(
            entry = question.entry,
            isCorrect = isCorrect,
            grade = grade,
            userAnswer = answer
        )
        _quizSession.value = session.copy(
            scrambleAnswer = answer,
            lastScrambleCorrect = isCorrect,
            isAnswerRevealed = true,
            records = session.records + record
        )
    }

    fun completeMatchPairsRound(mistakenEntryIds: Set<Long>) {
        val session = _quizSession.value ?: return
        if (session.matchRoundCompleted) return
        val question = session.currentQuestion ?: return
        val pairs = question.matchPairs
        if (pairs.isEmpty()) return
        val isLast = session.currentIndex + 1 >= session.questions.size

        val roundRecords = pairs.map { pair ->
            val ok = !mistakenEntryIds.contains(pair.entry.id)
            QuizAnswerRecord(
                entry = pair.entry,
                isCorrect = ok,
                grade = if (ok) SrsGrade.GOOD else SrsGrade.AGAIN
            )
        }
        viewModelScope.launch {
            roundRecords.forEach { rec ->
                repository.recordSrsReview(rec.entry, rec.grade)
            }
            _dailyStats.value = preferences.recordReviewActivity(incrementMiniQuiz = isLast)
            _timeRefreshTick.value = System.currentTimeMillis()
        }
        _quizSession.value = session.copy(
            matchRoundCompleted = true,
            isAnswerRevealed = true,
            records = session.records + roundRecords
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
                lastTypingCorrect = null,
                scrambleAnswer = "",
                lastScrambleCorrect = null,
                matchRoundCompleted = false
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
                scrambleAnswer = "",
                lastScrambleCorrect = null,
                matchRoundCompleted = false,
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
    fun exportAndShare(format: ExportFormat, onlyFiltered: Boolean, customWords: List<WordEntry>? = null) {
        viewModelScope.launch {
            val wordsToExport = customWords ?: if (onlyFiltered) uiState.value.filteredWords else repository.getAllWordsOnce()
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

    fun exportToPickedUri(uri: Uri, format: ExportFormat, onlyFiltered: Boolean, customWords: List<WordEntry>? = null) {
        viewModelScope.launch {
            val wordsToExport = customWords ?: if (onlyFiltered) uiState.value.filteredWords else repository.getAllWordsOnce()
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
                        "تم الاستيراد: أُضيف ${summary.insertedCount} كلمة جديدة • دُمج ${summary.mergedCount} • تم تجاهل المكرر ${summary.skippedCount}"
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
        prepositions = entry.prepositions.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
        collocations = entry.collocations.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
        definition = entry.definition.trim(),
        derivatives = entry.derivatives.trim(),
        expressions = entry.expressions.trim(),
        linguisticNotes = entry.linguisticNotes.trim(),
        examples = entry.examples.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
        personalHint = entry.personalHint.trim(),
        notes = entry.notes.trim(),
        importantNotes = entry.importantNotes.trim(),
        studyTags = entry.studyTags.map { it.trim() }.filter { it.isNotBlank() }.distinct(),
        difficulty = entry.difficulty.coerceIn(0, 3)
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
        var hardW = 0
        var incompleteW = 0
        var totalRev = 0
        var totalCorrect = 0
        var totalIntelligenceScore = 0

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
            if (w.isHardOrMistake()) hardW++
            val intel = LearningAnalyticsEngine.computeWordIntelligence(w)
            if (intel.isIncomplete) incompleteW++
            totalIntelligenceScore += intel.masteryScorePercent
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
        val masteryPct = if (allWords.isNotEmpty()) {
            (totalIntelligenceScore / allWords.size).coerceIn(0, 100)
        } else {
            0
        }
        if (allWords.isNotEmpty()) {
            preferences.updateTodayMasterySnapshot(masteryPct, now)
        }

        return LearningStatistics(
            totalWords = allWords.size,
            newWords = newW,
            learningWords = learningW,
            reviewingWords = reviewingW,
            masteredWords = masteredW,
            masteryPercent = masteryPct,
            dueToday = dueW,
            hardOrMistakesCount = hardW,
            incompleteWordsCount = incompleteW,
            totalReviewsAllTime = totalRev,
            overallAccuracyPercent = accuracy,
            reviewsToday = daily.reviewsToday,
            miniQuizzesToday = daily.miniQuizzesToday,
            streakDays = daily.currentStreakDays,
            dailyGoalTarget = daily.dailyGoalTarget,
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
                entry.prepositions.joinToString(" "),
                entry.collocations.joinToString(" "),
                entry.definition,
                entry.derivatives,
                entry.expressions,
                entry.linguisticNotes,
                entry.examples.joinToString(" "),
                entry.personalHint,
                entry.notes,
                entry.importantNotes,
                entry.studyTags.joinToString(" ")
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
            SearchField.PREPOSITIONS -> entry.prepositions
            SearchField.COLLOCATIONS -> entry.collocations
            SearchField.DEFINITION -> listOf(entry.definition)
            SearchField.DERIVATIVES -> listOf(entry.derivatives)
            SearchField.EXPRESSIONS -> listOf(entry.expressions)
            SearchField.LINGUISTIC_NOTES -> listOf(entry.linguisticNotes)
            SearchField.EXAMPLES -> entry.examples
            SearchField.NOTES -> listOf(entry.notes, entry.importantNotes, entry.personalHint)
            SearchField.STUDY_TAGS -> entry.studyTags
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

        if (f.prepositionsQuery.isNotBlank()) {
            val pq = normalize(f.prepositionsQuery)
            if (entry.prepositions.none { normalize(it).contains(pq) }) return false
        }

        if (f.collocationsQuery.isNotBlank()) {
            val cq = normalize(f.collocationsQuery)
            if (entry.collocations.none { normalize(it).contains(cq) }) return false
        }

        if (f.linguisticNotesQuery.isNotBlank()) {
            val lnq = normalize(f.linguisticNotesQuery)
            val hit = normalize(entry.linguisticNotes).contains(lnq) ||
                normalize(entry.notes).contains(lnq) ||
                normalize(entry.importantNotes).contains(lnq)
            if (!hit) return false
        }

        if (f.selectedStudyTags.isNotEmpty()) {
            val entryTags = entry.studyTags.map { normalize(it) }
            val matched = f.selectedStudyTags.any { sel -> entryTags.contains(normalize(sel)) }
            if (!matched) return false
        }

        if (f.selectedDifficulties.isNotEmpty()) {
            if (!f.selectedDifficulties.contains(entry.difficulty)) return false
        }

        if (f.favoritesOnly && !entry.isFavorite) return false
        if (f.hasNotesOnly && entry.notes.isBlank() && entry.importantNotes.isBlank() && entry.linguisticNotes.isBlank()) return false
        if (f.hasExpressionsOnly && entry.expressions.isBlank() && entry.examples.isEmpty()) return false
        if (f.hardOrMistakesOnly && !entry.isHardOrMistake()) return false
        if (f.incompleteOnly && !LearningAnalyticsEngine.computeWordIntelligence(entry).isIncomplete) return false

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
            SortOption.DIFFICULTY_DESC -> list.sortedWith(
                compareByDescending<WordEntry> { it.difficulty }
                    .thenByDescending { it.lastWrongAt }
                    .thenByDescending { it.createdAt }
            )
            SortOption.MOST_REVIEWED -> list.sortedWith(
                compareByDescending<WordEntry> { it.srsTotalReviews }
                    .thenByDescending { it.createdAt }
            )
        }
    }

    private fun normalize(text: String): String = WordRepository.normalizeKey(text)
}
