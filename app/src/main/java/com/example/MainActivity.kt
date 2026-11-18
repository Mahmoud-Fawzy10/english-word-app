package com.example

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
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Quiz
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
import com.example.domain.QuizQuestionType
import com.example.domain.QuizScopeMode
import com.example.export.DictionaryExporter
import com.example.export.ExportFormat
import com.example.export.JsonBackupManager
import com.example.ui.screens.AddEditWordScreen
import com.example.ui.screens.ExportSheet
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.QuizHubScreen
import com.example.ui.screens.QuizSessionScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.WordDetailScreen
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
    const val QUIZ_HUB = "quiz_hub"
    const val STATS = "stats"
    const val QUIZ_SESSION = "quiz_session"
    const val ADD_EDIT = "add_edit?wordId={wordId}&prefill={prefill}"
    const val DETAIL = "detail/{wordId}"

    fun addEdit(wordId: Long = 0L, prefill: String = ""): String {
        return "add_edit?wordId=$wordId&prefill=${Uri.encode(prefill)}"
    }

    fun detail(wordId: Long): String = "detail/$wordId"
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
        currentRoute == Routes.QUIZ_HUB ||
        currentRoute == Routes.STATS

    var pendingSaveFilteredOnly by remember { mutableStateOf(false) }
    var pendingImportPolicy by remember { mutableStateOf(DuplicatePolicy.MERGE) }
    var pendingImportIsXlsx by remember { mutableStateOf(false) }
    var pendingRestoreReplace by remember { mutableStateOf(false) }
    var showGlobalExportSheet by remember { mutableStateOf(false) }

    val pdfSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.PDF.mimeType)
    ) { uri ->
        if (uri != null) viewModel.exportToPickedUri(uri, ExportFormat.PDF, pendingSaveFilteredOnly)
    }
    val excelSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.EXCEL.mimeType)
    ) { uri ->
        if (uri != null) viewModel.exportToPickedUri(uri, ExportFormat.EXCEL, pendingSaveFilteredOnly)
    }
    val csvSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(ExportFormat.GOOGLE_SHEETS.mimeType)
    ) { uri ->
        if (uri != null) viewModel.exportToPickedUri(uri, ExportFormat.GOOGLE_SHEETS, pendingSaveFilteredOnly)
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

    fun launchSaveExport(format: ExportFormat, onlyFiltered: Boolean) {
        pendingSaveFilteredOnly = onlyFiltered
        val suggested = DictionaryExporter.suggestedFileName(format)
        when (format) {
            ExportFormat.PDF -> pdfSaveLauncher.launch(suggested)
            ExportFormat.EXCEL -> excelSaveLauncher.launch(suggested)
            ExportFormat.GOOGLE_SHEETS -> csvSaveLauncher.launch(suggested)
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
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    val homeSelected = currentRoute == Routes.HOME
                    NavigationBarItem(
                        selected = homeSelected,
                        onClick = { navigateTopLevel(Routes.HOME) },
                        icon = {
                            Icon(
                                imageVector = if (homeSelected) Icons.Rounded.MenuBook else Icons.Outlined.MenuBook,
                                contentDescription = "قاموسي"
                            )
                        },
                        label = { Text("قاموسي") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_home_tab")
                    )

                    val quizSelected = currentRoute == Routes.QUIZ_HUB
                    NavigationBarItem(
                        selected = quizSelected,
                        onClick = { navigateTopLevel(Routes.QUIZ_HUB) },
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
                                    imageVector = if (quizSelected) Icons.Rounded.Quiz else Icons.Outlined.Quiz,
                                    contentDescription = "الاختبارات"
                                )
                            }
                        },
                        label = { Text("الاختبارات") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_quiz_tab")
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
                                    contentDescription = "الإحصائيات"
                                )
                            }
                        },
                        label = { Text("الإحصائيات") },
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
                composable(Routes.HOME) {
                    HomeScreen(
                        uiState = uiState,
                        themeMode = themeMode,
                        snackbarHostState = snackbarHostState,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onSearchFieldChange = viewModel::updateSearchField,
                        onToggleBookFilter = viewModel::toggleQuickBookFilter,
                        onBrowseGroupModeChange = viewModel::updateBrowseGroupMode,
                        onApplyAdvancedFilter = viewModel::updateAdvancedFilter,
                        onClearAllFilters = viewModel::clearAllFilters,
                        onSortSelect = viewModel::updateSortOption,
                        onWordClick = { wordId ->
                            navController.navigate(Routes.detail(wordId))
                        },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onSpeakWord = viewModel::speakWordOffline,
                        onAddWordClick = { prefill ->
                            navController.navigate(Routes.addEdit(wordId = 0L, prefill = prefill))
                        },
                        onStartDueReviewClick = {
                            val dueConfig = QuizConfig(
                                scopeMode = QuizScopeMode.SRS_DUE,
                                questionType = QuizQuestionType.FLASHCARD_SRS,
                                questionLimit = 15
                            )
                            if (viewModel.startQuiz(dueConfig)) {
                                navController.navigate(Routes.QUIZ_SESSION)
                            }
                        },
                        onCycleThemeMode = viewModel::cycleThemeMode,
                        onMergeAllDuplicates = viewModel::mergeAllDuplicates,
                        onOpenStatsAndBackup = { navigateTopLevel(Routes.STATS) },
                        onSaveExport = ::launchSaveExport,
                        onShareExport = { format, onlyFiltered ->
                            viewModel.exportAndShare(format, onlyFiltered)
                        }
                    )
                }

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
                        }
                    )
                }

                composable(Routes.STATS) {
                    StatsScreen(
                        uiState = uiState,
                        themeMode = themeMode,
                        snackbarHostState = snackbarHostState,
                        onSelectThemeMode = viewModel::setThemeMode,
                        onMergeDuplicateGroup = viewModel::mergeDuplicateGroup,
                        onMergeAllDuplicates = viewModel::mergeAllDuplicates,
                        onOpenWordDetail = { wordId ->
                            navController.navigate(Routes.detail(wordId))
                        },
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
                        onOpenExportSheet = { showGlobalExportSheet = true },
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
                            onNextQuestion = viewModel::nextQuizQuestion,
                            onRestart = viewModel::restartCurrentQuiz,
                            onSpeakWord = viewModel::speakWordOffline,
                            onManualSchedule = viewModel::updateManualSrsSchedule,
                            onOpenWordDetail = { wordId ->
                                navController.navigate(Routes.detail(wordId))
                            },
                            onExit = {
                                viewModel.endQuizSession()
                                navController.popBackStack()
                            }
                        )
                    }
                }

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
                        onCheckDuplicates = viewModel::findDuplicatesFor,
                        onSpeakWord = viewModel::speakWordOffline,
                        onOpenExistingWord = { existingId ->
                            navController.navigate(Routes.detail(existingId))
                        },
                        onSave = { entry, mergeTarget ->
                            viewModel.saveWord(entry, mergeTarget) {
                                navController.popBackStack()
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Routes.DETAIL,
                    arguments = listOf(
                        navArgument("wordId") { type = NavType.LongType }
                    )
                ) { backStackEntry ->
                    val wordId = backStackEntry.arguments?.getLong("wordId") ?: 0L
                    val word by viewModel.observeWord(wordId).collectAsStateWithLifecycle(initialValue = null)

                    WordDetailScreen(
                        word = word,
                        onBack = { navController.popBackStack() },
                        onEditClick = { id ->
                            navController.navigate(Routes.addEdit(wordId = id))
                        },
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
        ExportSheet(
            totalCount = uiState.allWordsCount,
            filteredCount = uiState.filteredWords.size,
            isFiltered = uiState.isSearchOrFilterActive,
            isExporting = uiState.isExporting,
            onSaveToDevice = ::launchSaveExport,
            onShareExport = { format, onlyFiltered ->
                viewModel.exportAndShare(format, onlyFiltered)
            },
            onDismiss = { showGlobalExportSheet = false }
        )
    }
}
