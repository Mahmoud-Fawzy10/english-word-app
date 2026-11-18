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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MergeType
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restore
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ThemeMode
import com.example.data.model.DuplicatePolicy
import com.example.data.model.WordEntry
import com.example.ui.components.RoundedSectionCard
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.LocalQamusiColors
import com.example.ui.theme.SkyAccent
import com.example.ui.viewmodel.DictionaryUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(
    uiState: DictionaryUiState,
    themeMode: ThemeMode,
    snackbarHostState: SnackbarHostState,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onMergeDuplicateGroup: (List<WordEntry>) -> Unit,
    onMergeAllDuplicates: () -> Unit,
    onOpenWordDetail: (Long) -> Unit,
    onStartDueReview: () -> Unit,
    onOpenExportSheet: () -> Unit,
    onImportCsvClick: (DuplicatePolicy) -> Unit,
    onImportXlsxClick: (DuplicatePolicy) -> Unit,
    onExportJsonBackupClick: () -> Unit,
    onRestoreJsonBackupClick: (replaceExisting: Boolean) -> Unit
) {
    val stats = uiState.stats
    val palette = LocalQamusiColors.current
    var selectedImportPolicy by remember { mutableStateOf(DuplicatePolicy.MERGE) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
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
                contentPadding = PaddingValues(bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Blue Rounded Statistics & Data Center Header
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp),
                        shadowElevation = 6.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(listOf(CobaltDark, CobaltPrimary, SkyAccent))
                                )
                                .statusBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "الإحصائيات والبيانات",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "متابعة الحفظ والتكرار المتباعد والنسخ الاحتياطي أوفلاين بالكامل.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatsHeroPill(
                                        icon = Icons.Rounded.LocalFireDepartment,
                                        label = "سلسلة ${stats.streakDays} يوم"
                                    )
                                    StatsHeroPill(
                                        label = "مراجعات اليوم: ${stats.reviewsToday}"
                                    )
                                    StatsHeroPill(
                                        label = "الدقة: ${stats.overallAccuracyPercent}%"
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. Learning & SRS Maturity Progress Card
                item {
                    RoundedSectionCard(
                        title = "تقدم الحفظ والمراجعة (SRS)",
                        subtitle = "إجمالي الكلمات: ${stats.totalWords} • إجمالي المراجعات: ${stats.totalReviewsAllTime}",
                        icon = Icons.Rounded.Analytics,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCountTile(
                                label = "جديدة",
                                count = stats.newWords,
                                bg = MaterialTheme.colorScheme.secondaryContainer,
                                fg = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            StatCountTile(
                                label = "تعلّم",
                                count = stats.learningWords,
                                bg = palette.srsHardBg,
                                fg = palette.srsHardText,
                                modifier = Modifier.weight(1f)
                            )
                            StatCountTile(
                                label = "مراجعة",
                                count = stats.reviewingWords,
                                bg = palette.srsGoodBg,
                                fg = palette.srsGoodText,
                                modifier = Modifier.weight(1f)
                            )
                            StatCountTile(
                                label = "متقنة",
                                count = stats.masteredWords,
                                bg = palette.successBg,
                                fg = palette.successText,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (stats.totalWords > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                if (stats.masteredWords > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(stats.masteredWords.toFloat())
                                            .background(palette.successBorder)
                                    )
                                }
                                if (stats.reviewingWords > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(stats.reviewingWords.toFloat())
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                                if (stats.learningWords > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(stats.learningWords.toFloat())
                                            .background(palette.warningBorder)
                                    )
                                }
                                if (stats.newWords > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(stats.newWords.toFloat())
                                            .background(MaterialTheme.colorScheme.secondary)
                                    )
                                }
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

                // 2. Distribution by Book & Parts of Speech
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

                // 3. Duplicate Detection & Cleaner Card
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
                                shape = RoundedCornerShape(18.dp),
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
                                    shape = RoundedCornerShape(18.dp),
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
                                            Icon(
                                                imageVector = Icons.Rounded.MergeType,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
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

                // 4. Offline Import, Export & JSON Backup / Restore Center
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
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("import_policy_${policy.name.lowercase()}")
                                )
                            }
                        }

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
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير القاموس (PDF / Excel / Google Sheets CSV)")
                        }
                    }
                }

                // 5. Theme Appearance Selector Card
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
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
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
}

@Composable
private fun StatsHeroPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.20f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
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
        shape = RoundedCornerShape(20.dp),
        color = bg
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleLarge,
                color = fg,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg
            )
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
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$count كلمة",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
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
