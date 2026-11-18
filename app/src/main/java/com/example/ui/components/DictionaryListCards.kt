package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.WordEntry
import com.example.ui.theme.LocalQamusiColors

data class CurriculumWordItem(
    val entry: WordEntry,
    val lessonLabel: String,
    val pageLabel: String,
    val unitSortNum: Int,
    val lessonSortNum: Int,
    val pageSortNum: Int
)

data class CurriculumUnitGroup(
    val unitTitle: String,
    val unitSortNum: Int,
    val items: List<CurriculumWordItem>
)

fun buildPosGroups(words: List<WordEntry>): List<Pair<String, List<WordEntry>>> {
    val map = linkedMapOf<String, MutableList<WordEntry>>()
    for (word in words) {
        val posList = if (word.partsOfSpeech.isEmpty()) listOf("غير مصنف") else word.partsOfSpeech
        for (pos in posList) {
            map.getOrPut(pos) { mutableListOf() }.add(word)
        }
    }
    return map.entries
        .sortedWith(compareByDescending<Map.Entry<String, MutableList<WordEntry>>> { it.value.size }.thenBy { it.key })
        .map { it.key to it.value.toList() }
}

fun buildCurriculumGroups(words: List<WordEntry>): List<CurriculumUnitGroup> {
    val map = linkedMapOf<String, MutableList<CurriculumWordItem>>()
    fun firstInt(raw: String): Int = Regex("\\d+").find(raw)?.value?.toIntOrNull() ?: Int.MAX_VALUE

    for (word in words) {
        val locs = word.locations.filter { it.isNotBlank() }
        if (locs.isEmpty()) {
            map.getOrPut("بدون وحدة / موقع محدد") { mutableListOf() }.add(
                CurriculumWordItem(
                    entry = word,
                    lessonLabel = "",
                    pageLabel = "",
                    unitSortNum = Int.MAX_VALUE,
                    lessonSortNum = Int.MAX_VALUE,
                    pageSortNum = Int.MAX_VALUE
                )
            )
        } else {
            for (loc in locs) {
                val unitTitle = when {
                    loc.unit.isNotBlank() && loc.book.isNotBlank() -> "${loc.book} • الوحدة ${loc.unit}"
                    loc.unit.isNotBlank() -> "الوحدة ${loc.unit}"
                    loc.book.isNotBlank() -> loc.book
                    else -> "مواقع أخرى"
                }
                map.getOrPut(unitTitle) { mutableListOf() }.add(
                    CurriculumWordItem(
                        entry = word,
                        lessonLabel = if (loc.lesson.isNotBlank()) "الدرس ${loc.lesson}" else "",
                        pageLabel = if (loc.page.isNotBlank()) "ص ${loc.page}" else "",
                        unitSortNum = firstInt(loc.unit),
                        lessonSortNum = firstInt(loc.lesson),
                        pageSortNum = firstInt(loc.page)
                    )
                )
            }
        }
    }

    return map.entries
        .map { (title, list) ->
            val sortedItems = list
                .distinctBy { "${it.entry.id}_${it.lessonLabel}_${it.pageLabel}" }
                .sortedWith(
                    compareBy<CurriculumWordItem> { it.lessonSortNum }
                        .thenBy { it.pageSortNum }
                        .thenBy { it.entry.displayTitle().lowercase() }
                )
            CurriculumUnitGroup(
                unitTitle = title,
                unitSortNum = sortedItems.minOfOrNull { it.unitSortNum } ?: Int.MAX_VALUE,
                items = sortedItems
            )
        }
        .sortedWith(compareBy<CurriculumUnitGroup> { it.unitSortNum }.thenBy { it.unitTitle })
}

@Composable
fun GroupSectionHeader(
    title: String,
    count: Int,
    icon: ImageVector = Icons.Rounded.Category,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordSummaryCard(
    entry: WordEntry,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSpeakWord: (String) -> Unit,
    modifier: Modifier = Modifier,
    subHeaderTag: String? = null
) {
    val palette = LocalQamusiColors.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .testTag("word_card_${entry.id}"),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!subHeaderTag.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = subHeaderTag,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = entry.displayTitle(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (entry.ipa.isNotBlank()) {
                            Text(
                                text = entry.ipa,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (entry.arabicMeaning.isNotBlank() && entry.word.isNotBlank()) {
                        Text(
                            text = entry.arabicMeaning,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SrsStatusBadge(entry = entry)
                    if (entry.word.isNotBlank()) {
                        IconButton(
                            onClick = { onSpeakWord(entry.word) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("speak_card_${entry.id}")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "نطق الكلمة",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (entry.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (entry.isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة",
                            tint = if (entry.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (entry.books.isNotEmpty() || entry.partsOfSpeech.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    entry.books.forEach { book ->
                        BookPillBadge(bookName = book)
                    }
                    entry.partsOfSpeech.forEach { pos ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = pos,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            val validLocations = entry.locations.filter { it.isNotBlank() }
            if (validLocations.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    validLocations.take(3).forEach { loc ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = loc.formattedLabel(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    if (validLocations.size > 3) {
                        Text(
                            text = "+${validLocations.size - 3} مواقع",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                    }
                }
            }

            if (entry.synonyms.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "مرادفات:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    entry.synonyms.take(3).forEach { syn ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = palette.synonymBg
                        ) {
                            Text(
                                text = syn,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.synonymText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            val previewNote = entry.importantNotes.ifBlank { entry.notes.ifBlank { entry.definition } }
            if (previewNote.isNotBlank()) {
                Text(
                    text = previewNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
