package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.ThemeMode
import com.example.data.model.DuplicatePolicy
import com.example.data.model.WordEntry
import com.example.domain.QuizConfig
import com.example.domain.QuizDirection
import com.example.domain.QuizQuestionType
import com.example.domain.QuizScopeMode
import com.example.export.DictionaryExporter
import com.example.export.ExportFormat
import com.example.export.JsonBackupManager
import com.example.ui.screens.AchievementsScreen
import com.example.ui.screens.AddEditWordScreen
import com.example.ui.screens.AdvancedSearchScreen
import com.example.ui.screens.ExportSheet
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.QuizHubScreen
import com.example.ui.screens.QuizSessionScreen
import com.example.ui.screens.RelationshipMapScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.StudyCenterScreen
import com.example.ui.screens.WordDetailScreen
import com.example.ui.screens.WordsListScreen
import com.example.ui.theme.QamusiTheme
import com.example.ui.viewmodel.DictionaryViewModel
import com.example.ui.viewmodel.UiEvent
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: DictionaryViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LaunchedEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
                    navigationBarStyle = if (darkTheme) {
                        SystemBarStyle.dark(Color.Transparent.toArgb())
                    } else {
                        SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb())
                    }
                )
            }
            QamusiTheme(themeMode = themeMode) {
                QamusiApp(viewModel = vm, themeMode = themeMode)
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val WORDS_LIST = "words_list"
    const val STUDY_CENTER = "study_center"
    const val STATS = "stats"
    const val QUIZ_HUB = "quiz_hub"
    const val QUIZ_SESSION = "quiz_session"
    const val ADVANCED_SEARCH = "advanced_search"
    const val ACHIEVEMENTS = "achievements"
    const val RELATIONSHIP_MAP = "relationship_map?wordId={wordId}"
    const val ADD_EDIT = "add_edit?wordId={wordId}&prefill={prefill}"
    const val DETAIL = "detail/{wordId}"

    fun addEdit(wordId: Long = 0L, prefill: String = ""): String {
        return "add_edit?wordId=$wordId&prefill=${Uri.encode(prefill)}"
    }

    fun detail(wordId: Long): String = "detail/$wordId"

    fun relationshipMap(wordId: Long = 0L): String = "relationship_map?wordId=$wordId"
}

@Composable
fun QamusiApp(
    viewModel: DictionaryViewModel,
    themeMode: ThemeMode
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val quizConfig by viewModel.quizConfig.collectAsStateWithLifecycle()
    val quizSession by viewModel.quizSession.collectAsStateWithLifecycle()
    val audioPlaylist by viewModel.audioPlaylist.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDueClock()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute == Routes.HOME ||
        currentRoute == Routes.WORDS_LIST ||
        currentRoute == Routes.STUDY_CENTER ||
        currentRoute == Routes.STATS

    var pendingSaveFilteredOnly by remember { mutableStateOf(false) }
    var pendingCustomExportWords by remember { mutableStateOf<List<WordEntry>?>(null) }
    var pendingImportPolicy by remember { mutableStateOf(DuplicatePolicy.SKIP) }
    var pendingImportIsXlsx by remember { mutableStateOf(false) }
    var pendingRestoreReplace by remember { mutableStateOf(false) }
    var showGlobalExportSheet by remember { mutableStateOf(false) }

    val pdfSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.PDF.mimeType)
    ) { uri ->
        val custom = pendingCustomExportWords
        pendingCustomExportWords = null
        if (uri != null) viewModel.exportToPickedUri(uri, ExportFormat.PDF, pendingSaveFilteredOnly, custom)
    }
    val excelSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.EXCEL.mimeType)
    ) { uri ->
        val custom = pendingCustomExportWords
        pendingCustomExportWords = null
        if (uri != null) viewModel.exportToPickedUri(uri, ExportFormat.EXCEL, pendingSaveFilteredOnly, custom)
    }
    val csvSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.GOOGLE_SHEETS.mimeType)
    ) { uri ->
        val custom = pendingCustomExportWords
        pendingCustomExportWords = null
        if (uri != null) viewModel.exportToPickedUri(uri, ExportFormat.GOOGLE_SHEETS, pendingSaveFilteredOnly, custom)
    }
    val zipBundleSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.GOOGLE_SHEETS_BUNDLE.mimeType)
    ) { uri ->
        val custom = pendingCustomExportWords
        pendingCustomExportWords = null
        if (uri != null) viewModel.exportToPickedUri(uri, ExportFormat.GOOGLE_SHEETS_BUNDLE, pendingSaveFilteredOnly, custom)
    }
    val jsonBackupSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportJsonBackupToUri(uri)
    }
    val spreadsheetImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importSpreadsheetFromUri(uri, pendingImportIsXlsx, pendingImportPolicy)
        }
    }
    val jsonRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreJsonBackupFromUri(uri, pendingRestoreReplace)
        }
    }

    fun launchSaveExport(format: ExportFormat, onlyFiltered: Boolean, customWords: List<WordEntry>? = null) {
        pendingSaveFilteredOnly = onlyFiltered
        pendingCustomExportWords = customWords
        val suggested = DictionaryExporter.suggestedFileName(format)
        when (format) {
            ExportFormat.PDF -> pdfSaveLauncher.launch(suggested)
            ExportFormat.EXCEL -> excelSaveLauncher.launch(suggested)
            ExportFormat.GOOGLE_SHEETS -> csvSaveLauncher.launch(suggested)
            ExportFormat.GOOGLE_SHEETS_BUNDLE -> zipBundleSaveLauncher.launch(suggested)
        }
    }

    fun navigateTopLevel(route: String) {
        if (currentRoute != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.LaunchShareIntent -> {
                    val chooser = DictionaryExporter.buildShareIntent(event.uri, event.format)
                    context.startActivity(chooser)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    val homeSelected = currentRoute == Routes.HOME
                    NavigationBarItem(
                        selected = homeSelected,
                        onClick = { navigateTopLevel(Routes.HOME) },
                        icon = {
                            Icon(
                                imageVector = if (homeSelected) Icons.Rounded.Home else Icons.Outlined.Home,
                                contentDescription = "الرئيسية"
                            )
                        },
                        label = { Text("الرئيسية", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_home_tab")
                    )

                    val wordsSelected = currentRoute == Routes.WORDS_LIST
                    NavigationBarItem(
                        selected = wordsSelected,
                        onClick = { navigateTopLevel(Routes.WORDS_LIST) },
                        icon = {
                            Icon(
                                imageVector = if (wordsSelected) Icons.Rounded.MenuBook else Icons.Outlined.MenuBook,
                                contentDescription = "قاموسي"
                            )
                        },
                        label = { Text("قاموسي", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_words_list_tab")
                    )

                    val studySelected = currentRoute == Routes.STUDY_CENTER
                    NavigationBarItem(
                        selected = studySelected,
                        onClick = { navigateTopLevel(Routes.STUDY_CENTER) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (uiState.dueReviewCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ) {
                                            Text("${uiState.dueReviewCount}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (studySelected) Icons.Rounded.Psychology else Icons.Outlined.Psychology,
                                    contentDescription = "المذاكرة"
                                )
                            }
                        },
                        label = { Text("المذاكرة", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_study_center_tab")
                    )

                    val statsSelected = currentRoute == Routes.STATS
                    NavigationBarItem(
                        selected = statsSelected,
                        onClick = { navigateTopLevel(Routes.STATS) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (uiState.duplicateGroups.isNotEmpty()) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ) {
                                            Text("${uiState.duplicateGroups.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (statsSelected) Icons.Rounded.Analytics else Icons.Outlined.Analytics,
                                    contentDescription = "إنجازي"
                                )
                            }
                        },
                        label = { Text("إنجازي", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_stats_tab")
                    )
                }
            }
        }
    ) { outerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomBar) outerPadding.calculateBottomPadding() else 0.dp)
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME
            ) {
                // Screen 1: الرئيسية
                composable(Routes.HOME) {
                    HomeDashboardScreen(
                        uiState = uiState,
                        audioPlaylist = audioPlaylist,
                        themeMode = themeMode,
                        snackbarHostState = snackbarHostState,
                        onSearchSubmitToList = { q ->
                            viewModel.updateSearchQuery(q)
                            if (q.isNotBlank()) viewModel.commitSearchHistory(q)
                            navigateTopLevel(Routes.WORDS_LIST)
                        },
                        onOpenAdvancedSearch = { navController.navigate(Routes.ADVANCED_SEARCH) },
                        onOpenWordsList = { navigateTopLevel(Routes.WORDS_LIST) },
                        onOpenStudyCenter = { navigateTopLevel(Routes.STUDY_CENTER) },
                        onOpenStats = { navigateTopLevel(Routes.STATS) },
                        onOpenQuizHub = { navController.navigate(Routes.QUIZ_HUB) },
                        onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                        onOpenWordDetail = { wordId -> navController.navigate(Routes.detail(wordId)) },
                        onAddWordClick = { prefill -> navController.navigate(Routes.addEdit(wordId = 0L, prefill = prefill)) },
                        onSpeakWord = viewModel::speakWordOffline,
                        onCycleThemeMode = viewModel::cycleThemeMode,
                        onMergeAllDuplicates = viewModel::mergeAllDuplicates,
                        onSubmitBulkQuickAdd = { raw, book, tag -> viewModel.bulkQuickAdd(raw, book, tag) },
                        onSaveExport = { format, onlyFiltered ->
                            launchSaveExport(format, onlyFiltered, customWords = null)
                        },
                        onShareExport = { format, onlyFiltered ->
                            viewModel.exportAndShare(format, onlyFiltered)
                        },
                        onSpeechRateChange = viewModel::setTtsSpeechRate,
                        onStartAudioPlaylist = viewModel::startAudioPlaylist,
                        onToggleAudioPause = viewModel::togglePauseAudioPlaylist,
                        onSkipAudio = viewModel::skipAudioPlaylist,
                        onStopAudio = viewModel::stopAudioPlaylist,
                        onRepeatTwiceChange = viewModel::setAudioRepeatTwice
                    )
                }

                // Screen 2: قائمة الكلمات
                composable(Routes.WORDS_LIST) {
                    WordsListScreen(
                        uiState = uiState,
                        snackbarHostState = snackbarHostState,
                        onBackToHome = { navigateTopLevel(Routes.HOME) },
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onCommitSearchHistory = viewModel::commitSearchHistory,
                        onClearSearchHistory = viewModel::clearSearchHistory,
                        onSearchFieldChange = viewModel::updateSearchField,
                        onToggleBookFilter = viewModel::toggleQuickBookFilter,
                        onBrowseGroupModeChange = viewModel::updateBrowseGroupMode,
                        onApplyAdvancedFilter = viewModel::updateAdvancedFilter,
                        onToggleFavoritesFilter = viewModel::toggleFavoritesFilter,
                        onToggleHardFilter = viewModel::toggleHardMistakesFilter,
                        onToggleIncompleteFilter = viewModel::toggleIncompleteFilter,
                        onClearAllFilters = viewModel::clearAllFilters,
                        onSortSelect = viewModel::updateSortOption,
                        onOpenAdvancedSearch = { navController.navigate(Routes.ADVANCED_SEARCH) },
                        onWordClick = { wordId -> navController.navigate(Routes.detail(wordId)) },
                        onToggleWordSelection = viewModel::toggleWordSelection,
                        onSelectAllFiltered = viewModel::selectAllFilteredWords,
                        onClearSelection = viewModel::clearWordSelection,
                        onBatchSetFavorite = viewModel::batchSetFavorite,
                        onBatchDelete = viewModel::batchDeleteSelected,
                        onBatchQuizSelected = {
                            if (viewModel.startQuizWithSelectedWords()) {
                                navController.navigate(Routes.QUIZ_SESSION)
                            }
                        },
                        onBatchExportSelected = { selectedWords ->
                            pendingCustomExportWords = selectedWords
                            showGlobalExportSheet = true
                        },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onSpeakWord = viewModel::speakWordOffline,
                        onAddWordClick = { prefill -> navController.navigate(Routes.addEdit(wordId = 0L, prefill = prefill)) }
                    )
                }

                // Screen 4: مركز المذاكرة
                composable(Routes.STUDY_CENTER) {
                    StudyCenterScreen(
                        uiState = uiState,
                        audioPlaylist = audioPlaylist,
                        snackbarHostState = snackbarHostState,
                        onOpenStats = { navigateTopLevel(Routes.STATS) },
                        onSetDailyGoal = viewModel::setDailyGoalTarget,
                        onStartFlashcardReview = {
                            val scope = if (uiState.dueReviewCount > 0) QuizScopeMode.SRS_DUE else QuizScopeMode.RANDOM_RANGE
                            val cfg = QuizConfig(
                                scopeMode = scope,
                                questionType = QuizQuestionType.FLASHCARD_SRS,
                                questionLimit = uiState.dailyGoalTarget.coerceIn(5, 25)
                            )
                            if (viewModel.startQuiz(cfg)) {
                                navController.navigate(Routes.QUIZ_SESSION)
                            }
                        },
                        onStartQuickMcqQuiz = {
                            val cfg = QuizConfig(
                                scopeMode = QuizScopeMode.RANDOM_RANGE,
                                questionType = QuizQuestionType.MULTIPLE_CHOICE,
                                direction = QuizDirection.WORD_TO_MEANING,
                                questionLimit = 10
                            )
                            if (viewModel.startQuiz(cfg)) {
                                navController.navigate(Routes.QUIZ_SESSION)
                            }
                        },
                        onStartReverseMcqQuiz = {
                            val cfg = QuizConfig(
                                scopeMode = QuizScopeMode.RANDOM_RANGE,
                                questionType = QuizQuestionType.MULTIPLE_CHOICE,
                                direction = QuizDirection.MEANING_TO_WORD,
                                questionLimit = 10
                            )
                            if (viewModel.startQuiz(cfg)) {
                                navController.navigate(Routes.QUIZ_SESSION)
                            }
                        },
                        onChallengeAction = { missionId ->
                            when (missionId) {
                                "daily_goal" -> {
                                    val scope = if (uiState.dueReviewCount > 0) QuizScopeMode.SRS_DUE else QuizScopeMode.RANDOM_RANGE
                                    val cfg = QuizConfig(
                                        scopeMode = scope,
                                        questionType = QuizQuestionType.FLASHCARD_SRS,
                                        questionLimit = uiState.dailyGoalTarget.coerceIn(5, 25)
                                    )
                                    if (viewModel.startQuiz(cfg)) {
                                        navController.navigate(Routes.QUIZ_SESSION)
                                    }
                                }
                                "mini_quiz" -> {
                                    val cfg = QuizConfig(
                                        scopeMode = QuizScopeMode.RANDOM_RANGE,
                                        questionType = QuizQuestionType.MATCH_PAIRS,
                                        questionLimit = 8
                                    )
                                    if (viewModel.startQuiz(cfg)) {
                                        navController.navigate(Routes.QUIZ_SESSION)
                                    }
                                }
                                "hard_vault" -> {
                                    if (uiState.hardOrMistakesCount > 0) {
                                        val cfg = QuizConfig(
                                            scopeMode = QuizScopeMode.HARD_AND_MISTAKES,
                                            questionType = QuizQuestionType.FLASHCARD_SRS,
                                            questionLimit = 15
                                        )
                                        if (viewModel.startQuiz(cfg)) {
                                            navController.navigate(Routes.QUIZ_SESSION)
                                        }
                                    } else {
                                        navController.navigate(Routes.addEdit())
                                    }
                                }
                            }
                        },
                        onStartHardVaultQuiz = {
                            val cfg = QuizConfig(
                                scopeMode = QuizScopeMode.HARD_AND_MISTAKES,
                                questionType = QuizQuestionType.FLASHCARD_SRS,
                                questionLimit = 20
                            )
                            if (viewModel.startQuiz(cfg)) {
                                navController.navigate(Routes.QUIZ_SESSION)
                            }
                        },
                        onStartUnitQuiz = { book, unit ->
                            val cfg = QuizConfig(
                                scopeMode = QuizScopeMode.BY_UNIT,
                                selectedBook = if (book == "كلمات عامة") null else book,
                                unitFrom = if (unit == "بدون وحدة") "" else unit,
                                unitTo = if (unit == "بدون وحدة") "" else unit,
                                questionType = QuizQuestionType.MIXED,
                                questionLimit = 15
                            )
                            if (viewModel.startQuiz(cfg)) {
                                navController.navigate(Routes.QUIZ_SESSION)
                            }
                        },
                        onBrowseUnit = { book, unit ->
                            viewModel.applyUnitFilter(book, unit)
                            navigateTopLevel(Routes.WORDS_LIST)
                        },
                        onOpenStudyTagQuiz = { tagName ->
                            val cfg = QuizConfig(
                                scopeMode = QuizScopeMode.BY_STUDY_TAG,
                                selectedStudyTag = tagName,
                                questionType = QuizQuestionType.FLASHCARD_SRS,
                                questionLimit = 15
                            )
                            if (viewModel.startQuiz(cfg)) {
                                navController.navigate(Routes.QUIZ_SESSION)
                            }
                        },
                        onBrowseStudyTag = { tagName ->
                            viewModel.clearAllFilters()
                            viewModel.applyQuickTagFilter(tagName)
                            navigateTopLevel(Routes.WORDS_LIST)
                        },
                        onSpeakWord = viewModel::speakWordOffline,
                        onSpeechRateChange = viewModel::setTtsSpeechRate,
                        onStartAudioPlaylist = viewModel::startAudioPlaylist,
                        onToggleAudioPause = viewModel::togglePauseAudioPlaylist,
                        onSkipAudio = viewModel::skipAudioPlaylist,
                        onStopAudio = viewModel::stopAudioPlaylist,
                        onRepeatTwiceChange = viewModel::setAudioRepeatTwice
                    )
                }

                // Screen 6: إحصائياتي
                composable(Routes.STATS) {
                    StatsScreen(
                        uiState = uiState,
                        themeMode = themeMode,
                        formConfig = uiState.formConfig,
                        snackbarHostState = snackbarHostState,
                        onBackToHome = { navigateTopLevel(Routes.HOME) },
                        onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                        onBrowseIncompleteWords = {
                            viewModel.applyIncompleteOnlyFilter()
                            navigateTopLevel(Routes.WORDS_LIST)
                        },
                        onSelectThemeMode = viewModel::setThemeMode,
                        onUpdateFormConfig = viewModel::updateFormConfig,
                        onSubmitBulkQuickAdd = { raw, book, tag -> viewModel.bulkQuickAdd(raw, book, tag) },
                        onMergeDuplicateGroup = viewModel::mergeDuplicateGroup,
                        onMergeAllDuplicates = viewModel::mergeAllDuplicates,
                        onOpenWordDetail = { wordId -> navController.navigate(Routes.detail(wordId)) },
                        onStartDueReview = {
                            val dueConfig = QuizConfig(
                                scopeMode = QuizScopeMode.SRS_DUE,
                                questionType = QuizQuestionType.FLASHCARD_SRS,
                                questionLimit = 20
                            )
                            if (viewModel.startQuiz(dueConfig)) {
                                navController.navigate(Routes.QUIZ_SESSION)
                            }
                        },
                        onStartHardQuiz = {
                            val cfg = QuizConfig(
                                scopeMode = QuizScopeMode.HARD_AND_MISTAKES,
                                questionType = QuizQuestionType.MULTIPLE_CHOICE,
                                questionLimit = 15
                            )
                            if (viewModel.startQuiz(cfg)) {
                                navController.navigate(Routes.QUIZ_SESSION)
                            }
                        },
                        onOpenExportSheet = {
                            pendingCustomExportWords = null
                            showGlobalExportSheet = true
                        },
                        onImportCsvClick = { policy ->
                            pendingImportPolicy = policy
                            pendingImportIsXlsx = false
                            spreadsheetImportLauncher.launch(
                                arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*")
                            )
                        },
                        onImportXlsxClick = { policy ->
                            pendingImportPolicy = policy
                            pendingImportIsXlsx = true
                            spreadsheetImportLauncher.launch(
                                arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "*/*")
                            )
                        },
                        onExportJsonBackupClick = {
                            jsonBackupSaveLauncher.launch(JsonBackupManager.suggestedBackupFileName())
                        },
                        onRestoreJsonBackupClick = { replace ->
                            pendingRestoreReplace = replace
                            jsonRestoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        }
                    )
                }

                // Screen 5: الاختبارات
                composable(Routes.QUIZ_HUB) {
                    QuizHubScreen(
                        uiState = uiState,
                        quizConfig = quizConfig,
                        snackbarHostState = snackbarHostState,
                        onUpdateConfig = viewModel::updateQuizConfig,
                        onStartDueSrsReview = {
                            val dueConfig = QuizConfig(
                                scopeMode = QuizScopeMode.SRS_DUE,
                                questionType = QuizQuestionType.FLASHCARD_SRS,
                                questionLimit = 20
                            )
                            if (viewModel.startQuiz(dueConfig)) {
                                navController.navigate(Routes.QUIZ_SESSION)
                            }
                        },
                        onStartConfiguredQuiz = {
                            if (viewModel.startQuiz()) {
                                navController.navigate(Routes.QUIZ_SESSION)
                            }
                        },
                        onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Screen 10: مراجعة سريعة
                composable(Routes.QUIZ_SESSION) {
                    val activeSession = quizSession
                    if (activeSession == null) {
                        LaunchedEffect(Unit) {
                            navController.popBackStack()
                        }
                    } else {
                        QuizSessionScreen(
                            session = activeSession,
                            onRevealFlashcard = viewModel::revealFlashcard,
                            onSubmitFlashcardGrade = viewModel::submitFlashcardGrade,
                            onSelectMcqOption = viewModel::submitMcqOption,
                            onTypedInputChange = viewModel::updateTypedInput,
                            onSubmitTyping = viewModel::submitTypingAnswer,
                            onSubmitScramble = viewModel::submitScrambleAnswer,
                            onCompleteMatchRound = viewModel::completeMatchPairsRound,
                            onNextQuestion = viewModel::nextQuizQuestion,
                            onRestart = viewModel::restartCurrentQuiz,
                            onSpeakWord = viewModel::speakWordOffline,
                            onManualSchedule = viewModel::updateManualSrsSchedule,
                            onOpenWordDetail = { wordId -> navController.navigate(Routes.detail(wordId)) },
                            onExit = {
                                viewModel.endQuizSession()
                                navController.popBackStack()
                            }
                        )
                    }
                }

                // Screen 7: بحث المتقدم
                composable(Routes.ADVANCED_SEARCH) {
                    AdvancedSearchScreen(
                        uiState = uiState,
                        onApplyAndSearch = { query, searchField, filter ->
                            viewModel.updateSearchQuery(query)
                            viewModel.updateSearchField(searchField)
                            viewModel.updateAdvancedFilter(filter)
                            if (query.isNotBlank()) viewModel.commitSearchHistory(query)
                            navController.popBackStack()
                            navigateTopLevel(Routes.WORDS_LIST)
                        },
                        onReset = viewModel::clearAllFilters,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Screen 8: خريطة العلاقات بين الكلمات
                composable(
                    route = Routes.RELATIONSHIP_MAP,
                    arguments = listOf(
                        navArgument("wordId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        }
                    )
                ) { backStackEntry ->
                    val wordId = backStackEntry.arguments?.getLong("wordId") ?: 0L
                    RelationshipMapScreen(
                        allWords = uiState.allWords,
                        initialWordId = wordId,
                        onBack = { navController.popBackStack() },
                        onOpenWordDetail = { targetId -> navController.navigate(Routes.detail(targetId)) },
                        onAddMissingWord = { prefill -> navController.navigate(Routes.addEdit(wordId = 0L, prefill = prefill)) },
                        onSpeakWord = viewModel::speakWordOffline
                    )
                }

                // Screen 9: الإنجازاتي
                composable(Routes.ACHIEVEMENTS) {
                    AchievementsScreen(
                        uiState = uiState,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Add / Edit Word
                composable(
                    route = Routes.ADD_EDIT,
                    arguments = listOf(
                        navArgument("wordId") {
                            type = NavType.LongType
                            defaultValue = 0L
                        },
                        navArgument("prefill") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { backStackEntry ->
                    val wordId = backStackEntry.arguments?.getLong("wordId") ?: 0L
                    val prefill = backStackEntry.arguments?.getString("prefill").orEmpty()
                    var existingWord by remember(wordId) { mutableStateOf<WordEntry?>(null) }

                    LaunchedEffect(wordId) {
                        if (wordId > 0L) {
                            existingWord = viewModel.getWordOnce(wordId)
                        }
                    }

                    AddEditWordScreen(
                        existingWord = existingWord,
                        prefillQuery = prefill,
                        availableBooks = uiState.availableBooks,
                        availablePartsOfSpeech = uiState.availablePartsOfSpeech,
                        availableStudyTags = uiState.availableStudyTags,
                        formConfig = uiState.formConfig,
                        onUpdateFormConfig = viewModel::updateFormConfig,
                        onCheckDuplicates = viewModel::findDuplicatesFor,
                        onSpeakWord = viewModel::speakWordOffline,
                        onOpenExistingWord = { existingId ->
                            navController.navigate(Routes.detail(existingId))
                        },
                        onSave = { entry, mergeTarget ->
                            viewModel.saveWord(
                                entry = entry,
                                mergeTarget = mergeTarget,
                                onSaved = { navController.popBackStack() }
                            )
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Screen 3: تفاصيل الكلمة
                composable(
                    route = Routes.DETAIL,
                    arguments = listOf(
                        navArgument("wordId") { type = NavType.LongType }
                    )
                ) { backStackEntry ->
                    val wordId = backStackEntry.arguments?.getLong("wordId") ?: 0L
                    val word by viewModel.observeWord(wordId).collectAsStateWithLifecycle(initialValue = null)
                    val revisions by viewModel.observeRevisions(wordId).collectAsStateWithLifecycle(initialValue = emptyList())
                    val relatedWords = remember(word, uiState.allWords) {
                        word?.let { viewModel.findRelatedWords(it) } ?: emptyList()
                    }

                    WordDetailScreen(
                        word = word,
                        relatedWords = relatedWords,
                        revisions = revisions,
                        onBack = { navController.popBackStack() },
                        onEditClick = { id ->
                            navController.navigate(Routes.addEdit(wordId = id))
                        },
                        onOpenRelatedWord = { relId ->
                            navController.navigate(Routes.detail(relId))
                        },
                        onOpenRelationshipMap = { id ->
                            navController.navigate(Routes.relationshipMap(id))
                        },
                        onRestoreRevision = viewModel::restoreWordRevision,
                        onDeleteConfirm = { id ->
                            viewModel.deleteWord(id) {
                                navController.popBackStack()
                            }
                        },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onSpeakWord = viewModel::speakWordOffline,
                        onReviewNowClick = { targetWord ->
                            if (viewModel.startSingleWordReview(targetWord)) {
                                navController.navigate(Routes.QUIZ_SESSION)
                            }
                        },
                        onManualSrsUpdate = viewModel::updateManualSrsSchedule,
                        onShareCardText = { cardText ->
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, cardText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة بطاقة الكلمة"))
                        },
                        onCopiedMessage = { msg ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showGlobalExportSheet) {
        val customExport = pendingCustomExportWords
        ExportSheet(
            totalCount = customExport?.size ?: uiState.allWordsCount,
            filteredCount = customExport?.size ?: uiState.filteredWords.size,
            isFiltered = customExport == null && uiState.isSearchOrFilterActive,
            isExporting = uiState.isExporting,
            onSaveToDevice = { format, onlyFiltered ->
                showGlobalExportSheet = false
                launchSaveExport(format, onlyFiltered, customExport)
            },
            onShareExport = { format, onlyFiltered ->
                showGlobalExportSheet = false
                pendingCustomExportWords = null
                viewModel.exportAndShare(format, onlyFiltered, customExport)
            },
            onDismiss = {
                pendingCustomExportWords = null
                showGlobalExportSheet = false
            }
        )
    }
}
