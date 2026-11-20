package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ThemeMode
import com.example.data.model.WordEntry
import com.example.export.ExportFormat
import com.example.ui.components.GoldTrophyArtwork
import com.example.ui.components.QamusiCurvedBlueHeader
import com.example.ui.components.ReferenceWhiteCard
import com.example.ui.theme.LocalQamusiColors
import com.example.ui.viewmodel.AudioPlaylistState
import com.example.ui.viewmodel.DictionaryUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    uiState: DictionaryUiState,
    audioPlaylist: AudioPlaylistState,
    themeMode: ThemeMode,
    snackbarHostState: SnackbarHostState,
    onSearchSubmitToList: (String) -> Unit,
    onOpenAdvancedSearch: () -> Unit,
    onOpenWordsList: () -> Unit,
    onOpenStudyCenter: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenQuizHub: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenWordDetail: (Long) -> Unit,
    onAddWordClick: (String) -> Unit,
    onSpeakWord: (String) -> Unit,
    onCycleThemeMode: () -> Unit,
    onMergeAllDuplicates: () -> Unit,
    onSubmitBulkQuickAdd: (String, String?, String?) -> Unit,
    onSaveExport: (ExportFormat, Boolean) -> Unit,
    onShareExport: (ExportFormat, Boolean) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onStartAudioPlaylist: (List<WordEntry>, Boolean) -> Unit,
    onToggleAudioPause: () -> Unit,
    onSkipAudio: (Int) -> Unit,
    onStopAudio: () -> Unit,
    onRepeatTwiceChange: (Boolean) -> Unit
) {
    val palette = LocalQamusiColors.current
    var homeSearchInput by remember { mutableStateOf("") }
    var showQuickToolsSheet by remember { mutableStateOf(false) }
    var showBulkAddSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    val themeIcon = when (themeMode) {
        ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
        ThemeMode.LIGHT -> Icons.Rounded.LightMode
        ThemeMode.DARK -> Icons.Rounded.DarkMode
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Curved Blue Branded Header + Search Pill (Screen 1)
                item {
                    QamusiCurvedBlueHeader(
                        bottomCornerRadius = 32.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showQuickToolsSheet = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_menu_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Menu,
                                    contentDescription = "الأدوات السريعة",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = onCycleThemeMode,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_theme_toggle")
                            ) {
                                Icon(
                                    imageVector = themeIcon,
                                    contentDescription = "تبديل المظهر: ${themeMode.labelAr}",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = onOpenStats,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("home_settings_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "الإعدادات والنسخ الاحتياطي",
                                    tint = Color.White
                                )
                            }
                        }

                        // Brand Emblem + Title + Tagline
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.MenuBook,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Text(
                                text = "قاموسي",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تعلّم .. راجع .. أتقن",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        // Embedded White Search Pill
                        OutlinedTextField(
                            value = homeSearchInput,
                            onValueChange = { homeSearchInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .testTag("home_dashboard_search"),
                            placeholder = {
                                Text(
                                    text = "ابحث في قاموسي...",
                                    color = Color(0xFF64748B)
                                )
                            },
                            leadingIcon = {
                                IconButton(
                                    onClick = { onSearchSubmitToList(homeSearchInput) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "ابحث في قائمة الكلمات",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = onOpenAdvancedSearch,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("home_open_advanced_search")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = "البحث المتقدم",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(50),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White,
                                focusedTextColor = Color(0xFF0B1F44),
                                unfocusedTextColor = Color(0xFF0B1F44)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { onSearchSubmitToList(homeSearchInput) }
                            )
                        )
                    }
                }

                // 2. Word of the Day Card («كلمة اليوم»)
                item {
                    val wotd = uiState.wordOfTheDay
                    ReferenceWhiteCard(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .testTag("dashboard_word_of_day_card"),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        if (wotd != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                GoldTrophyArtwork(size = 78.dp)

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "كلمة اليوم",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = wotd.displayTitle(),
                                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (wotd.word.isNotBlank()) {
                                            IconButton(
                                                onClick = { onSpeakWord(wotd.word) },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                                    contentDescription = "نطق كلمة اليوم",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    if (wotd.ipa.isNotBlank()) {
                                        Text(
                                            text = wotd.ipa,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (wotd.arabicMeaning.isNotBlank() && wotd.word.isNotBlank()) {
                                        Text(
                                            text = wotd.arabicMeaning,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onOpenWordDetail(wotd.id) }
                                    .testTag("wotd_details_btn"),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "عرض التفاصيل",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            // Genuine Empty State (No fake placeholder word!)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                GoldTrophyArtwork(size = 66.dp)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "مرحباً بك في قاموسي",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "أضف أول كلمة لتظهر هنا كـ «كلمة اليوم» وتبدأ خطتك أوفلاين.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = { onAddWordClick("") },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("أضف أول كلمة")
                            }
                        }
                    }
                }

                // 3. Three KPI Cards Row (Word Count | Reviews Today | Mastery %)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardKpiCard(
                            icon = Icons.Rounded.CheckCircle,
                            iconTint = Color(0xFF16A34A),
                            iconBg = Color(0xFFDCFCE7),
                            value = "${uiState.allWordsCount}",
                            label = "كلمة",
                            onClick = onOpenWordsList,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kpi_words_count")
                        )
                        DashboardKpiCard(
                            icon = Icons.Rounded.Today,
                            iconTint = Color(0xFFD97706),
                            iconBg = Color(0xFFFEF3C7),
                            value = "${uiState.stats.reviewsToday}",
                            label = "مراجعة اليوم",
                            onClick = onOpenStudyCenter,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kpi_reviews_today")
                        )
                        DashboardKpiCard(
                            icon = Icons.Rounded.Analytics,
                            iconTint = MaterialTheme.colorScheme.primary,
                            iconBg = MaterialTheme.colorScheme.primaryContainer,
                            value = "${uiState.masteryPercent}%",
                            label = "معدل الإتقان",
                            onClick = onOpenStats,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kpi_mastery_pct")
                        )
                    }
                }

                // 4. Quick Quizzes Banner («اختبارات سريعة»)
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onOpenQuizHub() }
                            .testTag("dashboard_quick_quiz_banner"),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "اختبارات سريعة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.dueReviewCount > 0) {
                                        "لديك ${uiState.dueReviewCount} كلمة مستحقة — اختبر نفسك الآن"
                                    } else {
                                        "اختبر نفسك الآن بمختلف الأنماط"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.SportsEsports,
                                        contentDescription = "الاختبارات",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Optional Action Row: Achievements + Add Word + Audio Study
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onAddWordClick("") },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("dashboard_add_word_btn")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة كلمة")
                        }
                        OutlinedButton(
                            onClick = onOpenAchievements,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("dashboard_achievements_btn")
                        ) {
                            Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("الإنجازات")
                        }
                    }
                }

                // Duplicate cleanup prompt if legacy duplicates exist
                if (uiState.duplicateGroups.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = palette.warningBg,
                            border = BorderStroke(1.dp, palette.warningBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = palette.warningText)
                                Text(
                                    text = "توجد ${uiState.duplicateGroups.size} مجموعات مكررة سابقة",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.warningText,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = onMergeAllDuplicates,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.warningBorder)
                                ) {
                                    Text("دمج")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQuickToolsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickToolsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "أدوات قاموسي السريعة",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                QuickToolRow(
                    icon = Icons.Rounded.Bolt,
                    title = "إضافة سريعة لعدة كلمات",
                    subtitle = "أدخل قائمة كلمات دفعة واحدة مع رفض التكرار تلقائياً",
                    onClick = {
                        showQuickToolsSheet = false
                        showBulkAddSheet = true
                    }
                )
                QuickToolRow(
                    icon = Icons.Rounded.Headphones,
                    title = "وضع الاستماع والنطق الصوتي (أوفلاين)",
                    subtitle = "تشغيل قائمة كلمات للمراجعة الصوتية دون لمس",
                    onClick = {
                        showQuickToolsSheet = false
                        showAudioSheet = true
                    }
                )
                QuickToolRow(
                    icon = Icons.Rounded.Download,
                    title = "تصدير القاموس (Excel 10 أوراق / CSV / PDF)",
                    subtitle = "حفظ أو مشاركة كلماتك محلياً بالكامل",
                    onClick = {
                        showQuickToolsSheet = false
                        showExportSheet = true
                    }
                )
                QuickToolRow(
                    icon = Icons.Rounded.Psychology,
                    title = "خريطة المذاكرة والتحديات اليومية",
                    subtitle = "خريطة الكتب والوحدات والدروس",
                    onClick = {
                        showQuickToolsSheet = false
                        onOpenStudyCenter()
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showBulkAddSheet) {
        BulkQuickAddSheet(
            availableBooks = uiState.availableBooks,
            availableTags = uiState.availableStudyTags,
            onSubmitBulk = onSubmitBulkQuickAdd,
            onDismiss = { showBulkAddSheet = false }
        )
    }

    if (showAudioSheet) {
        AudioStudySheet(
            playlistState = audioPlaylist,
            speechRate = uiState.ttsSpeechRate,
            targetWords = uiState.filteredWords.ifEmpty { uiState.allWords },
            onSpeechRateChange = onSpeechRateChange,
            onStartPlaylist = onStartAudioPlaylist,
            onTogglePause = onToggleAudioPause,
            onSkip = onSkipAudio,
            onStop = onStopAudio,
            onRepeatTwiceChange = onRepeatTwiceChange,
            onDismiss = { showAudioSheet = false }
        )
    }

    if (showExportSheet) {
        ExportSheet(
            totalCount = uiState.allWordsCount,
            filteredCount = uiState.filteredWords.size,
            isFiltered = uiState.isSearchOrFilterActive,
            isExporting = uiState.isExporting,
            onSaveToDevice = { format, onlyFiltered ->
                showExportSheet = false
                onSaveExport(format, onlyFiltered)
            },
            onShareExport = { format, onlyFiltered ->
                showExportSheet = false
                onShareExport(format, onlyFiltered)
            },
            onDismiss = { showExportSheet = false }
        )
    }
}

@Composable
private fun DashboardKpiCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconBg,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickToolRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
