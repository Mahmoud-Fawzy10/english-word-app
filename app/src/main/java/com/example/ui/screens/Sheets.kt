package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AdvancedSearchFilter
import com.example.data.model.SortOption
import com.example.data.model.WordDifficulty
import com.example.export.ExportFormat
import com.example.ui.components.MultiSelectPillGroup
import com.example.ui.components.RoundedBlueTextField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedSearchSheet(
    currentFilter: AdvancedSearchFilter,
    availableBooks: List<String>,
    availablePartsOfSpeech: List<String>,
    availableStudyTags: List<String> = emptyList(),
    onApply: (AdvancedSearchFilter) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember(currentFilter) { mutableStateOf(currentFilter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "بحث متقدم متعدد المعايير",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(
                    onClick = {
                        draft = AdvancedSearchFilter()
                        onReset()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("مسح الكل")
                }
            }

            Text(
                text = "الكتب",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            MultiSelectPillGroup(
                options = availableBooks,
                selectedOptions = draft.selectedBooks,
                onToggle = { book ->
                    val next = draft.selectedBooks.toMutableSet()
                    if (!next.add(book)) next.remove(book)
                    draft = draft.copy(selectedBooks = next)
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RoundedBlueTextField(
                    value = draft.unitQuery,
                    onValueChange = { draft = draft.copy(unitQuery = it) },
                    label = "الوحدة",
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                RoundedBlueTextField(
                    value = draft.lessonQuery,
                    onValueChange = { draft = draft.copy(lessonQuery = it) },
                    label = "الدرس",
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                RoundedBlueTextField(
                    value = draft.pageQuery,
                    onValueChange = { draft = draft.copy(pageQuery = it) },
                    label = "الصفحة",
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            RoundedBlueTextField(
                value = draft.wordQuery,
                onValueChange = { draft = draft.copy(wordQuery = it) },
                label = "نص في الكلمة",
                singleLine = true
            )

            RoundedBlueTextField(
                value = draft.meaningQuery,
                onValueChange = { draft = draft.copy(meaningQuery = it) },
                label = "نص في المعنى بالعربي",
                singleLine = true
            )

            RoundedBlueTextField(
                value = draft.synonymAntonymQuery,
                onValueChange = { draft = draft.copy(synonymAntonymQuery = it) },
                label = "في المرادفات أو المضادات",
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RoundedBlueTextField(
                    value = draft.prepositionsQuery,
                    onValueChange = { draft = draft.copy(prepositionsQuery = it) },
                    label = "حروف الجر",
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                RoundedBlueTextField(
                    value = draft.collocationsQuery,
                    onValueChange = { draft = draft.copy(collocationsQuery = it) },
                    label = "المتلازمات",
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            RoundedBlueTextField(
                value = draft.linguisticNotesQuery,
                onValueChange = { draft = draft.copy(linguisticNotesQuery = it) },
                label = "في الملاحظات والملاحظات اللغوية",
                singleLine = true
            )

            Text(
                text = "موقعها من الكلام",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            MultiSelectPillGroup(
                options = availablePartsOfSpeech,
                selectedOptions = draft.selectedPartsOfSpeech,
                onToggle = { pos ->
                    val next = draft.selectedPartsOfSpeech.toMutableSet()
                    if (!next.add(pos)) next.remove(pos)
                    draft = draft.copy(selectedPartsOfSpeech = next)
                }
            )

            if (availableStudyTags.isNotEmpty()) {
                Text(
                    text = "المجموعات الدراسية",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                MultiSelectPillGroup(
                    options = availableStudyTags,
                    selectedOptions = draft.selectedStudyTags,
                    onToggle = { tag ->
                        val next = draft.selectedStudyTags.toMutableSet()
                        if (!next.add(tag)) next.remove(tag)
                        draft = draft.copy(selectedStudyTags = next)
                    }
                )
            }

            Text(
                text = "مستوى الصعوبة",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                WordDifficulty.entries.forEach { diff ->
                    val selected = draft.selectedDifficulties.contains(diff.level)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            val next = draft.selectedDifficulties.toMutableSet()
                            if (!next.add(diff.level)) next.remove(diff.level)
                            draft = draft.copy(selectedDifficulties = next)
                        },
                        label = { Text(diff.labelAr) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    FilterSwitchRow(
                        label = "المفضلة فقط",
                        checked = draft.favoritesOnly,
                        onCheckedChange = { draft = draft.copy(favoritesOnly = it) }
                    )
                    FilterSwitchRow(
                        label = "تحتوي على ملاحظات أو فروق لغوية",
                        checked = draft.hasNotesOnly,
                        onCheckedChange = { draft = draft.copy(hasNotesOnly = it) }
                    )
                    FilterSwitchRow(
                        label = "تحتوي على تعبيرات أو أمثلة",
                        checked = draft.hasExpressionsOnly,
                        onCheckedChange = { draft = draft.copy(hasExpressionsOnly = it) }
                    )
                    FilterSwitchRow(
                        label = "الكلمات الصعبة والأخطاء فقط",
                        checked = draft.hardOrMistakesOnly,
                        onCheckedChange = { draft = draft.copy(hardOrMistakesOnly = it) }
                    )
                }
            }

            Button(
                onClick = {
                    onApply(draft)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("apply_advanced_search_button"),
                shape = RoundedCornerShape(50)
            ) {
                Text("تطبيق البحث المتقدم", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilterSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortOptionsSheet(
    currentSort: SortOption,
    onSelect: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "فرز الكلمات حسب",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            SortOption.entries.forEach { option ->
                val selected = option == currentSort
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            onSelect(option)
                            onDismiss()
                        }
                        .testTag("sort_option_${option.name.lowercase()}"),
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    },
                    border = if (selected) {
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    } else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.labelAr,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExportSheet(
    totalCount: Int,
    filteredCount: Int,
    isFiltered: Boolean,
    isExporting: Boolean,
    onSaveToDevice: (ExportFormat, Boolean) -> Unit,
    onShareExport: (ExportFormat, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var exportFilteredOnly by remember(isFiltered) { mutableStateOf(isFiltered) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "تصدير القاموس (100% بدون نت)",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "تصدير Excel و Google Sheets يتضمن الورقة الأساسية «قاموسي» + 9 أوراق متخصصة تلقائياً (المرادفات، المضادات، التعريفات، الاشتقاقات، الملاحظات اللغوية، حروف الجر، المتلازمات اللفظية، التعبيرات والأمثلة، فهرس المنهج).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isExporting) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                )
            }

            if (isFiltered) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !exportFilteredOnly,
                        onClick = { exportFilteredOnly = false },
                        label = { Text("كل الكلمات ($totalCount)") },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    FilterChip(
                        selected = exportFilteredOnly,
                        onClick = { exportFilteredOnly = true },
                        label = { Text("النتائج المعروضة فقط ($filteredCount)") },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            ExportOptionCard(
                icon = Icons.Rounded.TableChart,
                format = ExportFormat.EXCEL,
                enabled = !isExporting,
                onSave = { onSaveToDevice(ExportFormat.EXCEL, exportFilteredOnly) },
                onShare = { onShareExport(ExportFormat.EXCEL, exportFilteredOnly) }
            )
            ExportOptionCard(
                icon = Icons.Rounded.FolderZip,
                format = ExportFormat.GOOGLE_SHEETS_BUNDLE,
                enabled = !isExporting,
                onSave = { onSaveToDevice(ExportFormat.GOOGLE_SHEETS_BUNDLE, exportFilteredOnly) },
                onShare = { onShareExport(ExportFormat.GOOGLE_SHEETS_BUNDLE, exportFilteredOnly) }
            )
            ExportOptionCard(
                icon = Icons.Rounded.TableView,
                format = ExportFormat.GOOGLE_SHEETS,
                enabled = !isExporting,
                onSave = { onSaveToDevice(ExportFormat.GOOGLE_SHEETS, exportFilteredOnly) },
                onShare = { onShareExport(ExportFormat.GOOGLE_SHEETS, exportFilteredOnly) }
            )
            ExportOptionCard(
                icon = Icons.Rounded.PictureAsPdf,
                format = ExportFormat.PDF,
                enabled = !isExporting,
                onSave = { onSaveToDevice(ExportFormat.PDF, exportFilteredOnly) },
                onShare = { onShareExport(ExportFormat.PDF, exportFilteredOnly) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ExportOptionCard(
    icon: ImageVector,
    format: ExportFormat,
    enabled: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = format.titleAr,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = format.subtitleAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSave,
                    enabled = enabled,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("حفظ بالجهاز")
                }
                Button(
                    onClick = onShare,
                    enabled = enabled,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("فتح / مشاركة")
                }
            }
        }
    }
}

@Composable
fun JsonRestoreModeDialog(
    onRestore: (replaceExisting: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Icon(
                imageVector = Icons.Rounded.Restore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("استعادة النسخة الاحتياطية (JSON)")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "اختر طريقة الاستعادة بعد اختيار ملف JSON من جهازك:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            onDismiss()
                            onRestore(false)
                        }
                        .testTag("restore_merge_option"),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "دمج مع القاموس الحالي (آمن وموصى به)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "يضيف الكلمات الجديدة ويدمج المواقع والمرادفات دون تكرار أو حذف.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            onDismiss()
                            onRestore(true)
                        }
                        .testTag("restore_replace_option"),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "استبدال القاموس بالكامل",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "يمسح الكلمات الحالية ويستعيد النسخة الاحتياطية وتقدم SRS كما هو.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
