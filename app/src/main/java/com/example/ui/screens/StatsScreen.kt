package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MergeType
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ThemeMode
import com.example.data.model.DuplicatePolicy
import com.example.data.model.WordEntry
import com.example.data.model.WordFormConfig
import com.example.ui.components.QamusiCurvedBlueHeader
import com.example.ui.components.ReferenceWhiteCard
import com.example.ui.components.RoundedSectionCard
import com.example.ui.components.WeeklyMasteryLineChart
import com.example.ui.theme.LocalQamusiColors
import com.example.ui.viewmodel.DictionaryUiState

private enum class StatsSegmentTab(val labelAr: String) {
    GENERAL("عام"),
    STUDY("المذاكرة"),
    QUIZZES("الاختبارات")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(
    uiState: DictionaryUiState,
    themeMode: ThemeMode,
    formConfig: WordFormConfig,
    snackbarHostState: SnackbarHostState,
    onBackToHome: () -> Unit = {},
    onOpenAchievements: () -> Unit = {},
    onBrowseIncompleteWords: () -> Unit = {},
    onSelectThemeMode: (ThemeMode) -> Unit,
    onUpdateFormConfig: (WordFormConfig) -> Unit,
    onSubmitBulkQuickAdd: (String, String?, String?) -> Unit,
    onMergeDuplicateGroup: (List<WordEntry>) -> Unit,
    onMergeAllDuplicates: () -> Unit,
    onOpenWordDetail: (Long) -> Unit,
    onStartDueReview: () -> Unit,
    onStartHardQuiz: () -> Unit = {},
    onOpenExportSheet: () -> Unit,
    onImportCsvClick: (DuplicatePolicy) -> Unit,
    onImportXlsxClick: (DuplicatePolicy) -> Unit,
    onExportJsonBackupClick: () -> Unit,
    onRestoreJsonBackupClick: (replaceExisting: Boolean) -> Unit
) {
    val stats = uiState.stats
    val palette = LocalQamusiColors.current
    var activeTab by remember { mutableStateOf(StatsSegmentTab.GENERAL) }
    var selectedImportPolicy by remember { mutableStateOf(DuplicatePolicy.SKIP) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showCustomizeSheet by remember { mutableStateOf(false) }
    var showBulkAddSheet by remember { mutableStateOf(false) }

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
                contentPadding = PaddingValues(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Curved Blue Header matching Reference Screen 6 («إحصائياتي»)
                item {
                    QamusiCurvedBlueHeader(
                        title = "إحصائياتي",
                        leadingIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                        leadingContentDescription = "العودة للرئيسية",
                        onLeadingClick = onBackToHome,
                        trailingIcon = Icons.Rounded.EmojiEvents,
                        trailingContentDescription = "الإنجازات",
                        trailingTint = Color(0xFFFDE047),
                        onTrailingClick = onOpenAchievements,
                        bottomCornerRadius = 28.dp
                    )
                }

                // 2. Segmented 3-Tab Pills matching Reference Screen 6 («عام | المذاكرة | الاختبارات»)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatsSegmentTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { activeTab = tab }
                                    .testTag("stats_tab_${tab.name.lowercase()}"),
                                shape = RoundedCornerShape(50),
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Text(
                                    text = tab.labelAr,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Two KPI Cards matching Reference Screen 6 («إجمالي الكلمات» & «معدل النجاح»)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "${stats.totalWords}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "إجمالي الكلمات",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "${stats.overallAccuracyPercent}%",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "معدل النجاح",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 4. Mastery Progression Line Chart («تطور الإتقان») matching Reference Screen 6
                item {
                    ReferenceWhiteCard(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تطور الإتقان",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "السلسلة: ${stats.streakDays} يوم",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        WeeklyMasteryLineChart(points = uiState.dailyHistory)
                    }
                }

                // 5. «أصعب الكلمات» Horizontal Clickable Pills matching Reference Screen 6
                item {
                    ReferenceWhiteCard(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "أصعب الكلمات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.hardestWords.isEmpty()) {
                            Text(
                                text = "لا توجد كلمات صعبة أو أخطاء مسجلة حالياً.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.hardestWords, key = { it.id }) { hardWord ->
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .clickable { onOpenWordDetail(hardWord.id) },
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Text(
                                            text = hardWord.displayTitle(),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab-specific Detail Sections
                when (activeTab) {
                    StatsSegmentTab.GENERAL -> {
                        // Missing Data Detector Card
                        if (uiState.incompleteWordsCount > 0) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { onBrowseIncompleteWords() }
                                        .testTag("incomplete_detector_card"),
                                    shape = RoundedCornerShape(20.dp),
                                    color = palette.warningBg,
                                    border = BorderStroke(1.dp, palette.warningBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = palette.warningText)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "كلمات تحتاج استكمال بيانات (${uiState.incompleteWordsCount})",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = palette.warningText,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "اضغط لعرض الكلمات التي ينقصها معنى أو مثال أو مرادف.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.warningText
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (stats.bookDistribution.isNotEmpty()) {
                            item {
                                RoundedSectionCard(
                                    title = "توزيع الكلمات حسب الكتاب",
                                    icon = Icons.Rounded.MenuBook,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    val maxCount = stats.bookDistribution.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                                    stats.bookDistribution.take(6).forEach { (book, count) ->
                                        DistributionBarRow(label = book, count = count, maxCount = maxCount)
                                    }
                                }
                            }
                        }

                        if (stats.posDistribution.isNotEmpty()) {
                            item {
                                RoundedSectionCard(
                                    title = "توزيع الكلمات حسب موقعها من الكلام",
                                    icon = Icons.Rounded.Category,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    val maxCount = stats.posDistribution.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                                    stats.posDistribution.take(8).forEach { (pos, count) ->
                                        DistributionBarRow(label = pos, count = count, maxCount = maxCount)
                                    }
                                }
                            }
                        }
                    }

                    StatsSegmentTab.STUDY -> {
                        item {
                            RoundedSectionCard(
                                title = "تقدم الحفظ والمراجعة (SRS)",
                                subtitle = "معدل الإتقان الشامل: ${stats.masteryPercent}% • مراجعات اليوم: ${stats.reviewsToday}",
                                icon = Icons.Rounded.Analytics,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatCountTile("جديدة", stats.newWords, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f))
                                    StatCountTile("تعلّم", stats.learningWords, palette.srsHardBg, palette.srsHardText, Modifier.weight(1f))
                                    StatCountTile("مراجعة", stats.reviewingWords, palette.srsGoodBg, palette.srsGoodText, Modifier.weight(1f))
                                    StatCountTile("متقنة", stats.masteredWords, palette.successBg, palette.successText, Modifier.weight(1f))
                                }

                                if (stats.totalWords > 0) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        if (stats.masteredWords > 0) Box(Modifier.fillMaxHeight().weight(stats.masteredWords.toFloat()).background(palette.successBorder))
                                        if (stats.reviewingWords > 0) Box(Modifier.fillMaxHeight().weight(stats.reviewingWords.toFloat()).background(MaterialTheme.colorScheme.primary))
                                        if (stats.learningWords > 0) Box(Modifier.fillMaxHeight().weight(stats.learningWords.toFloat()).background(palette.warningBorder))
                                        if (stats.newWords > 0) Box(Modifier.fillMaxHeight().weight(stats.newWords.toFloat()).background(MaterialTheme.colorScheme.secondary))
                                    }
                                }

                                if (stats.dueToday > 0) {
                                    Button(
                                        onClick = onStartDueReview,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Rounded.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("ابدأ مراجعة ${stats.dueToday} كلمة مستحقة الآن")
                                    }
                                }
                            }
                        }
                    }

                    StatsSegmentTab.QUIZZES -> {
                        item {
                            RoundedSectionCard(
                                title = "ملخص الاختبارات والميني-كويز",
                                subtitle = "إجمالي المراجعات: ${stats.totalReviewsAllTime} • ميني-كويز اليوم: ${stats.miniQuizzesToday}",
                                icon = Icons.Rounded.EmojiEvents,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "نسبة النجاح التراكمية: ${stats.overallAccuracyPercent}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = onStartHardQuiz,
                                    enabled = uiState.hardOrMistakesCount > 0,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("اختبار الكلمات الصعبة (${uiState.hardOrMistakesCount})")
                                }
                            }
                        }
                    }
                }

                // Duplicate Cleaner Card
                item {
                    RoundedSectionCard(
                        title = "اكتشاف ودمج الكلمات المكررة",
                        subtitle = if (uiState.duplicateGroups.isEmpty()) {
                            "قاموسك خالٍ من التكرار حالياً"
                        } else {
                            "تم العثور على ${uiState.duplicateGroups.size} مجموعات مكررة"
                        },
                        icon = Icons.Rounded.CleaningServices,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .testTag("duplicates_cleaner_card")
                    ) {
                        if (uiState.duplicateGroups.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = palette.successBg,
                                border = BorderStroke(1.dp, palette.successBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = palette.successText)
                                    Text(
                                        text = "ممتاز! لا توجد أي كلمات مكررة.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.successText
                                    )
                                }
                            }
                        } else {
                            uiState.duplicateGroups.forEach { group ->
                                val lead = group.first()
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = palette.warningBg,
                                    border = BorderStroke(1.dp, palette.warningBorder)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenWordDetail(lead.id) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = lead.displayTitle(),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = palette.warningText,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${group.size} إدخالات متطابقة",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.warningText
                                            )
                                        }
                                        Button(
                                            onClick = { onMergeDuplicateGroup(group) },
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Icon(Icons.Rounded.MergeType, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("دمج")
                                        }
                                    }
                                }
                            }
                            if (uiState.duplicateGroups.size > 1) {
                                OutlinedButton(
                                    onClick = onMergeAllDuplicates,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("merge_all_duplicates_button")
                                ) {
                                    Text("دمج جميع المجموعات (${uiState.duplicateGroups.size})")
                                }
                            }
                        }
                    }
                }

                // Offline Export / Import / JSON Backup Card
                item {
                    RoundedSectionCard(
                        title = "الاستيراد والتصدير والنسخ الاحتياطي",
                        subtitle = "جميع العمليات محلية أوفلاين 100%",
                        icon = Icons.Rounded.Backup,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        if (uiState.isExporting) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(CircleShape))
                        }

                        Text("سياسة التعامل مع الكلمات المكررة عند الاستيراد:", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DuplicatePolicy.entries.forEach { policy ->
                                val selected = selectedImportPolicy == policy
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedImportPolicy = policy },
                                    label = { Text(policy.labelAr) },
                                    shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("import_policy_${policy.name.lowercase()}")
                                )
                            }
                        }
                        Text(
                            text = selectedImportPolicy.descriptionAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onImportCsvClick(selectedImportPolicy) },
                                enabled = !uiState.isExporting,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_csv_button")
                            ) {
                                Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استيراد CSV")
                            }
                            OutlinedButton(
                                onClick = { onImportXlsxClick(selectedImportPolicy) },
                                enabled = !uiState.isExporting,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_xlsx_button")
                            ) {
                                Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استيراد Excel")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onExportJsonBackupClick,
                                enabled = !uiState.isExporting,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("export_json_backup_button")
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخة JSON")
                            }
                            OutlinedButton(
                                onClick = { showRestoreDialog = true },
                                enabled = !uiState.isExporting,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("restore_json_backup_button")
                            ) {
                                Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استعادة JSON")
                            }
                        }

                        OutlinedButton(
                            onClick = onOpenExportSheet,
                            enabled = !uiState.isExporting,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_export_sheet_button")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير القاموس (10 أوراق Excel / CSV / PDF)")
                        }
                    }
                }

                // Form Customization & Bulk Quick Add
                item {
                    RoundedSectionCard(
                        title = "تخصيص الحقول والإضافة السريعة",
                        icon = Icons.Rounded.Tune,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showCustomizeSheet = true },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("stats_customize_fields_button")
                            ) {
                                Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تخصيص الحقول")
                            }
                            Button(
                                onClick = { showBulkAddSheet = true },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("stats_bulk_quick_add_button")
                            ) {
                                Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة سريعة")
                            }
                        }
                    }
                }

                // Light / Dark / System Theme Selector
                item {
                    RoundedSectionCard(
                        title = "مظهر التطبيق",
                        subtitle = "اختر الوضع الفاتح أو الداكن أو التلقائي",
                        icon = Icons.Rounded.DarkMode,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                val selected = themeMode == mode
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectThemeMode(mode) },
                                    label = { Text(mode.labelAr) },
                                    shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRestoreDialog) {
        JsonRestoreModeDialog(
            onRestore = onRestoreJsonBackupClick,
            onDismiss = { showRestoreDialog = false }
        )
    }

    if (showCustomizeSheet) {
        FormCustomizationSheet(
            config = formConfig,
            onUpdateConfig = onUpdateFormConfig,
            onDismiss = { showCustomizeSheet = false }
        )
    }

    if (showBulkAddSheet) {
        BulkQuickAddSheet(
            availableBooks = uiState.availableBooks,
            availableTags = uiState.availableStudyTags,
            onSubmitBulk = onSubmitBulkQuickAdd,
            onDismiss = { showBulkAddSheet = false }
        )
    }
}

@Composable
private fun StatCountTile(
    label: String,
    count: Int,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = bg
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = "$count", style = MaterialTheme.typography.titleLarge, color = fg, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = fg)
        }
    }
}

@Composable
private fun DistributionBarRow(
    label: String,
    count: Int,
    maxCount: Int
) {
    val ratio = (count.toFloat() / maxCount.coerceAtLeast(1)).coerceIn(0.05f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(text = "$count كلمة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
        )
    }
}
