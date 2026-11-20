package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.unit.dp
import com.example.data.model.AdvancedSearchFilter
import com.example.data.model.SearchField
import com.example.ui.components.QamusiCurvedBlueHeader
import com.example.ui.components.ReferenceWhiteCard
import com.example.ui.components.RoundedBlueTextField
import com.example.ui.viewmodel.DictionaryUiState

private enum class AdvSearchFieldOption(val labelAr: String, val searchField: SearchField) {
    WORD("الكلمة", SearchField.WORD),
    MEANING("المعنى", SearchField.ARABIC_MEANING),
    SYNONYMS("المرادفات", SearchField.SYNONYMS),
    ANTONYMS("المضادات", SearchField.ANTONYMS),
    NOTES("الملاحظات", SearchField.NOTES),
    PREP_COL("حروف الجر والمتلازمات", SearchField.COLLOCATIONS)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedSearchScreen(
    uiState: DictionaryUiState,
    onApplyAndSearch: (
        query: String,
        searchField: SearchField,
        filter: AdvancedSearchFilter
    ) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var searchQuery by remember { mutableStateOf(uiState.searchQuery) }
    var selectedField by remember { mutableStateOf(uiState.searchField) }
    var draft by remember(uiState.advancedFilter) { mutableStateOf(uiState.advancedFilter) }
    var showBookDropdown by remember { mutableStateOf(false) }
    var showUnitDropdown by remember { mutableStateOf(false) }
    var showLessonDropdown by remember { mutableStateOf(false) }

    val selectedStarLevel = draft.selectedDifficulties.maxOrNull() ?: 0

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
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
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    QamusiCurvedBlueHeader(
                        title = "بحث المتقدم",
                        leadingIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                        onLeadingClick = onBack,
                        trailingIcon = Icons.Rounded.RestartAlt,
                        trailingContentDescription = "إعادة ضبط الفلاتر",
                        onTrailingClick = {
                            searchQuery = ""
                            selectedField = SearchField.ALL
                            draft = AdvancedSearchFilter()
                            onReset()
                        },
                        bottomCornerRadius = 28.dp
                    )
                }

                item {
                    ReferenceWhiteCard(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث في الكلمة أو المعنى أو الكتاب أو الملاحظات...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("adv_screen_query_input")
                        )

                        Text(
                            text = "ابحث في:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // 2x3 Checkable Pill Grid matching reference screen 7
                        val options = AdvSearchFieldOption.entries
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.chunked(3).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { opt ->
                                        val isChecked = selectedField == opt.searchField || selectedField == SearchField.ALL &&
                                            (opt == AdvSearchFieldOption.WORD || opt == AdvSearchFieldOption.ANTONYMS)
                                        val explicitlyPicked = selectedField == opt.searchField
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable {
                                                    selectedField = if (explicitlyPicked) SearchField.ALL else opt.searchField
                                                },
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (explicitlyPicked) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                if (explicitlyPicked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (explicitlyPicked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                                    contentDescription = null,
                                                    tint = if (explicitlyPicked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = opt.labelAr,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (explicitlyPicked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Book Selector Dropdown Row («في الكتاب:»)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AdvDropdownRow(
                                label = "في الكتاب:",
                                selectedValue = draft.selectedBooks.firstOrNull() ?: "كل الكتب",
                                onClick = { showBookDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showBookDropdown,
                                onDismissRequest = { showBookDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("كل الكتب") },
                                    onClick = {
                                        showBookDropdown = false
                                        draft = draft.copy(selectedBooks = emptySet())
                                    }
                                )
                                uiState.availableBooks.forEach { book ->
                                    DropdownMenuItem(
                                        text = { Text(book) },
                                        onClick = {
                                            showBookDropdown = false
                                            draft = draft.copy(selectedBooks = setOf(book))
                                        }
                                    )
                                }
                            }
                        }

                        // Unit Selector Dropdown Row («في الوحدة:»)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AdvDropdownRow(
                                label = "في الوحدة:",
                                selectedValue = if (draft.unitQuery.isBlank()) "كل الوحدات" else "الوحدة ${draft.unitQuery}",
                                onClick = { showUnitDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showUnitDropdown,
                                onDismissRequest = { showUnitDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("كل الوحدات") },
                                    onClick = {
                                        showUnitDropdown = false
                                        draft = draft.copy(unitQuery = "")
                                    }
                                )
                                uiState.availableUnits.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text("الوحدة $unit") },
                                        onClick = {
                                            showUnitDropdown = false
                                            draft = draft.copy(unitQuery = unit)
                                        }
                                    )
                                }
                            }
                        }

                        // Lesson Selector Dropdown Row («في الدرس:»)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            AdvDropdownRow(
                                label = "في الدرس:",
                                selectedValue = if (draft.lessonQuery.isBlank()) "كل الدروس" else "الدرس ${draft.lessonQuery}",
                                onClick = { showLessonDropdown = true }
                            )
                            DropdownMenu(
                                expanded = showLessonDropdown,
                                onDismissRequest = { showLessonDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("كل الدروس") },
                                    onClick = {
                                        showLessonDropdown = false
                                        draft = draft.copy(lessonQuery = "")
                                    }
                                )
                                uiState.availableLessons.forEach { lesson ->
                                    DropdownMenuItem(
                                        text = { Text("الدرس $lesson") },
                                        onClick = {
                                            showLessonDropdown = false
                                            draft = draft.copy(lessonQuery = lesson)
                                        }
                                    )
                                }
                            }
                        }

                        // Star Difficulty Row matching reference screen 7 («مستوى الصعوبة: ★★★☆☆»)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "مستوى الصعوبة:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                (1..3).forEach { level ->
                                    val filled = selectedStarLevel >= level
                                    IconButton(
                                        onClick = {
                                            val next = if (selectedStarLevel == level) emptySet() else setOf(level)
                                            draft = draft.copy(selectedDifficulties = next)
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = if (filled) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                            contentDescription = "صعوبة $level",
                                            tint = if (filled) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Optional direct field filters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RoundedBlueTextField(
                                value = draft.pageQuery,
                                onValueChange = { draft = draft.copy(pageQuery = it) },
                                label = "رقم الصفحة",
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            RoundedBlueTextField(
                                value = draft.prepositionsQuery,
                                onValueChange = { draft = draft.copy(prepositionsQuery = it) },
                                label = "حرف جر / متلازمة",
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Quick Boolean Filters
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                                AdvSwitchRow("المفضلة فقط", draft.favoritesOnly) {
                                    draft = draft.copy(favoritesOnly = it)
                                }
                                AdvSwitchRow("الصعبة والأخطاء فقط", draft.hardOrMistakesOnly) {
                                    draft = draft.copy(hardOrMistakesOnly = it)
                                }
                                AdvSwitchRow("كلمات بياناتها ناقصة فقط", draft.incompleteOnly) {
                                    draft = draft.copy(incompleteOnly = it)
                                }
                            }
                        }

                        // Full-Width Cobalt Search CTA Button matching reference screen 7
                        Button(
                            onClick = {
                                onApplyAndSearch(searchQuery, selectedField, draft)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("apply_advanced_search_button"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "بحث",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdvDropdownRow(
    label: String,
    selectedValue: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(82.dp)
        )
        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AdvSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
