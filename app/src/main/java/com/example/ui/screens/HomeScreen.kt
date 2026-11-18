package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ThemeMode
import com.example.data.model.AdvancedSearchFilter
import com.example.data.model.BrowseGroupMode
import com.example.data.model.SearchField
import com.example.data.model.SortOption
import com.example.data.model.WordEntry
import com.example.export.ExportFormat
import com.example.ui.components.GroupSectionHeader
import com.example.ui.components.WordSummaryCard
import com.example.ui.components.buildCurriculumGroups
import com.example.ui.components.buildPosGroups
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.LocalQamusiColors
import com.example.ui.theme.SkyAccent
import com.example.ui.viewmodel.DictionaryUiState

@Composable
fun HomeScreen(
    uiState: DictionaryUiState,
    themeMode: ThemeMode,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChange: (String) -> Unit,
    onSearchFieldChange: (SearchField) -> Unit,
    onToggleBookFilter: (String?) -> Unit,
    onBrowseGroupModeChange: (BrowseGroupMode) -> Unit,
    onApplyAdvancedFilter: (AdvancedSearchFilter) -> Unit,
    onClearAllFilters: () -> Unit,
    onSortSelect: (SortOption) -> Unit,
    onWordClick: (Long) -> Unit,
    onToggleFavorite: (WordEntry) -> Unit,
    onSpeakWord: (String) -> Unit,
    onAddWordClick: (prefillQuery: String) -> Unit,
    onStartDueReviewClick: () -> Unit,
    onCycleThemeMode: () -> Unit,
    onMergeAllDuplicates: () -> Unit,
    onOpenStatsAndBackup: () -> Unit,
    onSaveExport: (ExportFormat, Boolean) -> Unit,
    onShareExport: (ExportFormat, Boolean) -> Unit
) {
    var showAdvancedSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    val palette = LocalQamusiColors.current

    val posGroups = remember(uiState.filteredWords, uiState.browseGroupMode) {
        if (uiState.browseGroupMode == BrowseGroupMode.BY_PART_OF_SPEECH) {
            buildPosGroups(uiState.filteredWords)
        } else {
            emptyList()
        }
    }
    val curriculumGroups = remember(uiState.filteredWords, uiState.browseGroupMode) {
        if (uiState.browseGroupMode == BrowseGroupMode.BY_CURRICULUM) {
            buildCurriculumGroups(uiState.filteredWords)
        } else {
            emptyList()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddWordClick("") },
                modifier = Modifier.testTag("add_word_fab"),
                shape = RoundedCornerShape(22.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null
                    )
                },
                text = {
                    Text(
                        text = "كلمة جديدة",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
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
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Rounded Blue Hero Header
                item {
                    BlueDictionaryHeader(
                        totalWords = uiState.allWordsCount,
                        favoriteCount = uiState.favoriteCount,
                        themeMode = themeMode,
                        onCycleThemeMode = onCycleThemeMode,
                        onExportClick = { showExportSheet = true }
                    )
                }

                // Spaced Repetition Optimal Interval Prompt Banner
                if (uiState.dueReviewCount > 0) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable { onStartDueReviewClick() }
                                .testTag("home_srs_prompt_banner"),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Psychology,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${uiState.dueReviewCount} كلمة مستحقة للمراجعة اليوم",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "اضغط لبدء جلسة التكرار المتباعد وتثبيت الحفظ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "راجع",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Duplicate Alert Banner (when duplicates exist)
                if (uiState.duplicateGroups.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .testTag("home_duplicates_banner"),
                            shape = RoundedCornerShape(22.dp),
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
                                Icon(
                                    imageVector = Icons.Rounded.WarningAmber,
                                    contentDescription = null,
                                    tint = palette.warningText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "توجد ${uiState.duplicateGroups.size} مجموعات كلمات مكررة",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.warningText,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onMergeAllDuplicates() },
                                    shape = RoundedCornerShape(50),
                                    color = palette.warningBorder
                                ) {
                                    Text(
                                        text = "دمج الكل",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onOpenStatsAndBackup() },
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = "عرض",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.warningText,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Search Bar + Advanced Filter trigger
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = onSearchQueryChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("search_input"),
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.search_placeholder),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "بحث",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchQueryChange("") }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "مسح البحث"
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(50),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )

                            Surface(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .clickable { showAdvancedSheet = true }
                                    .testTag("advanced_search_button"),
                                shape = CircleShape,
                                color = if (uiState.advancedFilter.isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer
                                },
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    BadgedBox(
                                        badge = {
                                            if (uiState.advancedFilter.activeCount > 0) {
                                                Badge(
                                                    containerColor = SkyAccent,
                                                    contentColor = Color.White
                                                ) {
                                                    Text("${uiState.advancedFilter.activeCount}")
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Tune,
                                            contentDescription = "بحث متقدم",
                                            tint = if (uiState.advancedFilter.isActive) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // "Search By" field selector pills
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(SearchField.entries) { field ->
                                val selected = uiState.searchField == field
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSearchFieldChange(field) },
                                    label = { Text(field.labelAr) },
                                    shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }
                        }

                        // Quick Books filter + Sort bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item {
                                    val allSelected = uiState.quickBookFilter == null
                                    FilterChip(
                                        selected = allSelected,
                                        onClick = { onToggleBookFilter(null) },
                                        label = { Text("كل الكتب") },
                                        shape = RoundedCornerShape(50),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                }
                                items(uiState.availableBooks) { book ->
                                    val selected = uiState.quickBookFilter == book
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onToggleBookFilter(book) },
                                        label = { Text(book) },
                                        shape = RoundedCornerShape(50),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(50))
                                    .clickable { showSortSheet = true }
                                    .testTag("sort_button"),
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Sort,
                                        contentDescription = "ترتيب وفرز",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "فرز",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // 3-Mode Grouping Switcher: قائمة | أقسام الكلام | الوحدة والدرس
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BrowseGroupMode.entries.forEach { mode ->
                                val selected = uiState.browseGroupMode == mode
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onBrowseGroupModeChange(mode) }
                                        .testTag("browse_mode_${mode.name.lowercase()}"),
                                    shape = RoundedCornerShape(50),
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.labelAr,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = uiState.isSearchOrFilterActive) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "النتائج: ${uiState.filteredWords.size} كلمة • فرز: ${uiState.sortOption.labelAr}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onClearAllFilters() },
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "إعادة ضبط",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Empty State OR Not-Found State OR Grouped/Flat Word Cards
                if (uiState.allWordsCount == 0) {
                    item {
                        EmptyDictionaryCard(
                            onAddFirstClick = { onAddWordClick("") }
                        )
                    }
                } else if (uiState.filteredWords.isEmpty()) {
                    item {
                        SearchNotFoundCard(
                            query = uiState.searchQuery,
                            onAddMissingWordClick = { onAddWordClick(uiState.searchQuery.trim()) },
                            onClearFiltersClick = onClearAllFilters
                        )
                    }
                } else {
                    when (uiState.browseGroupMode) {
                        BrowseGroupMode.FLAT_LIST -> {
                            items(
                                items = uiState.filteredWords,
                                key = { it.id }
                            ) { wordEntry ->
                                WordSummaryCard(
                                    entry = wordEntry,
                                    onClick = { onWordClick(wordEntry.id) },
                                    onToggleFavorite = { onToggleFavorite(wordEntry) },
                                    onSpeakWord = onSpeakWord,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }

                        BrowseGroupMode.BY_PART_OF_SPEECH -> {
                            posGroups.forEach { (posName, groupWords) ->
                                item(key = "pos_header_$posName") {
                                    GroupSectionHeader(
                                        title = posName,
                                        count = groupWords.size,
                                        icon = Icons.Rounded.Category,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                                itemsIndexed(
                                    items = groupWords,
                                    key = { idx, w -> "pos_${posName}_${w.id}_$idx" }
                                ) { _, wordEntry ->
                                    WordSummaryCard(
                                        entry = wordEntry,
                                        onClick = { onWordClick(wordEntry.id) },
                                        onToggleFavorite = { onToggleFavorite(wordEntry) },
                                        onSpeakWord = onSpeakWord,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }

                        BrowseGroupMode.BY_CURRICULUM -> {
                            curriculumGroups.forEach { unitGroup ->
                                item(key = "cur_header_${unitGroup.unitTitle}") {
                                    GroupSectionHeader(
                                        title = unitGroup.unitTitle,
                                        count = unitGroup.items.size,
                                        icon = Icons.Rounded.MenuBook,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                                itemsIndexed(
                                    items = unitGroup.items,
                                    key = { idx, item -> "cur_${unitGroup.unitTitle}_${item.entry.id}_$idx" }
                                ) { _, item ->
                                    val subTag = listOf(item.lessonLabel, item.pageLabel)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" • ")
                                    WordSummaryCard(
                                        entry = item.entry,
                                        subHeaderTag = subTag.ifBlank { null },
                                        onClick = { onWordClick(item.entry.id) },
                                        onToggleFavorite = { onToggleFavorite(item.entry) },
                                        onSpeakWord = onSpeakWord,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdvancedSheet) {
        AdvancedSearchSheet(
            currentFilter = uiState.advancedFilter,
            availableBooks = uiState.availableBooks,
            availablePartsOfSpeech = uiState.availablePartsOfSpeech,
            onApply = onApplyAdvancedFilter,
            onReset = { onApplyAdvancedFilter(AdvancedSearchFilter()) },
            onDismiss = { showAdvancedSheet = false }
        )
    }

    if (showSortSheet) {
        SortOptionsSheet(
            currentSort = uiState.sortOption,
            onSelect = onSortSelect,
            onDismiss = { showSortSheet = false }
        )
    }

    if (showExportSheet) {
        ExportSheet(
            totalCount = uiState.allWordsCount,
            filteredCount = uiState.filteredWords.size,
            isFiltered = uiState.isSearchOrFilterActive,
            isExporting = uiState.isExporting,
            onSaveToDevice = onSaveExport,
            onShareExport = onShareExport,
            onDismiss = { showExportSheet = false }
        )
    }
}

@Composable
private fun BlueDictionaryHeader(
    totalWords: Int,
    favoriteCount: Int,
    themeMode: ThemeMode,
    onCycleThemeMode: () -> Unit,
    onExportClick: () -> Unit
) {
    val themeIcon = when (themeMode) {
        ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
        ThemeMode.LIGHT -> Icons.Rounded.LightMode
        ThemeMode.DARK -> Icons.Rounded.DarkMode
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp),
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(CobaltDark, CobaltPrimary, SkyAccent)
                    )
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_dict_hero),
                contentDescription = stringResource(R.string.hero_banner_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.18f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "قاموسي",
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudOff,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "قاموسك الخاص بدون نت",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { onCycleThemeMode() }
                                .testTag("theme_toggle_button"),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = themeIcon,
                                    contentDescription = "تبديل المظهر (${themeMode.labelAr})",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(50))
                                .clickable { onExportClick() }
                                .testTag("export_button"),
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = "تصدير القاموس",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "تصدير",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeaderStatPill(
                        icon = Icons.Rounded.Bookmark,
                        label = "$totalWords كلمة"
                    )
                    HeaderStatPill(
                        icon = Icons.Rounded.Favorite,
                        label = "$favoriteCount مفضلة"
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderStatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.18f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}

@Composable
private fun EmptyDictionaryCard(onAddFirstClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_search),
                contentDescription = stringResource(R.string.empty_search_desc),
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(28.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = "ابدأ ببناء قاموسك الخاص",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "أضف كلمات المنهج مع معانيها والكتب (المعاصر، A1، كتاب الوزارة) والوحدات والدروس والمرادفات والملاحظات بطريقتك الخاصة وبدون نت.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onAddFirstClick,
                modifier = Modifier
                    .height(50.dp)
                    .testTag("add_first_word_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("إضافة أول كلمة", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SearchNotFoundCard(
    query: String,
    onAddMissingWordClick: () -> Unit,
    onClearFiltersClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_search),
                contentDescription = stringResource(R.string.empty_search_desc),
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(26.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = if (query.isNotBlank()) "لم يتم العثور على «$query»" else "لا توجد كلمات مطابقة للبحث",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "يمكنك إضافة هذه الكلمة مباشرة إلى قاموسك الخاص الآن.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onAddMissingWordClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("add_not_found_button"),
                shape = RoundedCornerShape(50)
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (query.isNotBlank()) "إضافة «$query» إلى قاموسي" else "إضافة كلمة جديدة",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(
                onClick = onClearFiltersClick,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("عرض كل الكلمات")
            }
        }
    }
}
