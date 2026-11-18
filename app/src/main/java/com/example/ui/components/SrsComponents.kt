package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.WordEntry
import com.example.domain.SrsScheduler
import com.example.ui.theme.LocalQamusiColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SrsStatusBadge(
    entry: WordEntry,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val isDue = entry.isDueForReview(now)
    val isMastered = entry.srsIntervalDays >= 14 && !isDue
    val palette = LocalQamusiColors.current

    val (bg, fg, label) = when {
        entry.isNewForSrs() -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "جديدة"
        )
        isDue -> Triple(
            palette.srsGoodBg,
            palette.srsGoodText,
            "مستحقة الآن"
        )
        isMastered -> Triple(
            palette.successBg,
            palette.successText,
            "متقنة"
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            SrsScheduler.statusLabelAr(entry, now)
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SrsDetailCard(
    entry: WordEntry,
    onReviewNowClick: () -> Unit,
    onManualAdjustClick: () -> Unit
) {
    val now = System.currentTimeMillis()
    val statusText = SrsScheduler.statusLabelAr(entry, now)
    val accuracyText = if (entry.srsTotalReviews > 0) {
        val pct = (entry.srsCorrectCount * 100) / entry.srsTotalReviews
        "$pct% (${entry.srsCorrectCount}/${entry.srsTotalReviews})"
    } else {
        "لم تُراجع بعد"
    }
    val nextDateText = if (entry.srsNextReviewAt <= 0L) {
        "جاهزة للمراجعة الآن"
    } else {
        SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(entry.srsNextReviewAt))
    }

    RoundedSectionCard(
        title = "التكرار المتباعد والمراجعة الذكية (SRS)",
        subtitle = statusText,
        icon = Icons.Rounded.Psychology
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SrsMetricPill(
                label = "الفاصل الحالي",
                value = if (entry.srsIntervalDays == 0) "جديدة" else SrsScheduler.formatDaysAr(entry.srsIntervalDays),
                modifier = Modifier.weight(1f)
            )
            SrsMetricPill(
                label = "موعد المراجعة",
                value = nextDateText,
                modifier = Modifier.weight(1f)
            )
            SrsMetricPill(
                label = "نسبة التذكر",
                value = accuracyText,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onManualAdjustClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("manual_srs_button"),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Rounded.EditCalendar,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("تعديل الجدول يدوياً")
            }

            Button(
                onClick = onReviewNowClick,
                modifier = Modifier
                    .weight(1f)
                    .testTag("review_word_now_button"),
                shape = RoundedCornerShape(50)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("راجعها الآن")
            }
        }
    }
}

@Composable
private fun SrsMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualSrsScheduleDialog(
    entry: WordEntry,
    onConfirm: (intervalDays: Int, easeFactor: Float, resetProgress: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val quickOptions = listOf(
        0 to "الآن فوراً",
        1 to "غداً (1 يوم)",
        3 to "بعد 3 أيام",
        7 to "بعد أسبوع",
        14 to "بعد أسبوعين",
        30 to "بعد شهر"
    )

    var daysText by remember(entry.id) {
        mutableStateOf(entry.srsIntervalDays.coerceAtLeast(1).toString())
    }
    var selectedQuickDays by remember(entry.id) {
        mutableStateOf<Int?>(null)
    }
    var easeFactor by remember(entry.id) {
        mutableFloatStateOf(entry.srsEaseFactor.coerceIn(1.3f, 3.2f))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        icon = {
            Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "تعديل جدول المراجعة يدوياً",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "الكلمة: ${entry.displayTitle()}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "اختر موعد المراجعة القادم:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickOptions.forEach { (days, label) ->
                        val selected = selectedQuickDays == days
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedQuickDays = days
                                daysText = days.toString()
                            },
                            label = { Text(label) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = daysText,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }
                        daysText = digits
                        selectedQuickDays = digits.toIntOrNull()
                    },
                    label = { Text("أو أدخل عدد الأيام يدوياً (0 = الآن)") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_srs_days_input")
                )

                Column {
                    Text(
                        text = "معامل السهولة: ${"%.1f".format(Locale.US, easeFactor)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = easeFactor,
                        onValueChange = { easeFactor = it },
                        valueRange = 1.3f..3.2f
                    )
                }

                TextButton(
                    onClick = {
                        onConfirm(0, 2.5f, true)
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إعادة ضبط تقدم المراجعة (كلمة جديدة)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = (selectedQuickDays ?: daysText.toIntOrNull() ?: 1).coerceIn(0, 3650)
                    onConfirm(days, easeFactor, false)
                    onDismiss()
                },
                shape = RoundedCornerShape(50),
                modifier = Modifier.testTag("save_manual_srs_button")
            ) {
                Text("حفظ الموعد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
