package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AdvancedSearchFilter
import com.example.data.model.BrowseGroupMode
import com.example.data.model.SearchField
import com.example.data.model.SortOption
import com.example.data.model.WordEntry
import com.example.ui.components.CompactReferenceWordRow
import com.example.ui.components.GroupSectionHeader
import com.example.ui.components.QamusiCurvedBlueHeader
import com.example.ui.components.buildCurriculumGroups
import com.example.ui.components.buildDifficultyGroups
import com.example.ui.components.buildPosGroups
import com.example.ui.components.buildStudyTagGroups
import com.example.ui.theme.LocalQamusiColors
import com.example.ui.viewmodel.DictionaryUiState

@Composable
fun WordsListScreen(
    uiState: DictionaryUiState,
    snackbarHostState: SnackbarHostState,
    onBackToHome: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCommitSearchHistory: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onSearchFieldChange: (SearchField) -> Unit,
    onToggleBookFilter: (String?) -> Unit,
    onBrowseGroupModeChange: (BrowseGroupMode) -> Unit,
    onApplyAdvancedFilter: (AdvancedSearchFilter) -> Unit,
    onToggleFavoritesFilter: () -> Unit,
    onToggleHardFilter: () -> Unit,
    onToggleIncompleteFilter: () -> Unit,
    onClearAllFilters: () -> Unit,
    onSortSelect: (SortOption) -> Unit,
    onOpenAdvancedSearch: () -> Unit,
    onWordClick: (Long) -> Unit,
    onToggleWordSelection: (Long) -> Unit,
    onSelectAllFiltered: () -> Unit,
    onClearSelection: () -> Unit,
    onBatchSetFavorite: (Boolean) -> Unit,
    onBatchDelete: () -> Unit,
    onBatchQuizSelected: () -> Unit,
    onBatchExportSelected: (List<WordEntry>) -> Unit,
    onToggleFavorite: (WordEntry) -> Unit,
    onSpeakWord: (String) -> Unit,
    onAddWordClick: (prefillQuery: String) -> Unit
) {
    val palette = LocalQamusiColors.current
    var showSortSheet by remember { mutableStateOf(false) }
    var showBookMenu by remember { mutableStateOf(false) }
    var showUnitMenu by remember { mutableStateOf(false) }
    var showLessonMenu by remember { mutableStateOf(false) }
    var showFieldMenu by remember { mutableStateOf(false) }
    var showGroupMenu by remember { mutableStateOf(false) }

    val posGroups = remember(uiState.filteredWords, uiState.browseGroupMode) {
        if (uiState.browseGroupMode == BrowseGroupMode.BY_PART_OF_SPEECH) buildPosGroups(uiState.filteredWords) else emptyList()
    }
    val curriculumGroups = remember(uiState.filteredWords, uiState.browseGroupMode) {
        if (uiState.browseGroupMode == BrowseGroupMode.BY_CURRICULUM) buildCurriculumGroups(uiState.filteredWords) else emptyList()
    }
    val studyTagGroups = remember(uiState.filteredWords, uiState.browseGroupMode) {
        if (uiState.browseGroupMode == BrowseGroupMode.BY_STUDY_TAG) buildStudyTagGroups(uiState.filteredWords) else emptyList()
    }
    val difficultyGroups = remember(uiState.filteredWords, uiState.browseGroupMode) {
        if (uiState.browseGroupMode == BrowseGroupMode.BY_DIFFICULTY) buildDifficultyGroups(uiState.filteredWords) else emptyList()
    }

    val isAllActive = !uiState.advancedFilter.favoritesOnly &&
        !uiState.advancedFilter.hardOrMistakesOnly &&
        !uiState.advancedFilter.incompleteOnly &&
        uiState.quickBookFilter == null &&
        uiState.advancedFilter.unitQuery.isBlank() &&
        uiState.advancedFilter.lessonQuery.isBlank()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = { onAddWordClick("") },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(58.dp)
                        .testTag("add_word_fab")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "إضافة كلمة جديدة",
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Curved Blue Header matching Reference Screen 2
                item {
                    QamusiCurvedBlueHeader(
                        title = "قاموسي",
                        leadingIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                        leadingContentDescription = "العودة للرئيسية",
                        onLeadingClick = onBackToHome,
                        trailingIcon = Icons.AutoMirrored.Rounded.Sort,
                        trailingContentDescription = "ترتيب وفرز",
                        onTrailingClick = { showSortSheet = true },
                        bottomCornerRadius = 28.dp
                    ) {
                        // Embedded white search bar
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("search_input"),
                            placeholder = {
                                Text(
                                    text = "ابحث عن كلمة...",
                                    color = Color(0xFF64748B),
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onSearchQueryChange("") }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "مسح البحث",
                                                tint = Color(0xFF475569)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = onOpenAdvancedSearch,
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("advanced_search_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Tune,
                                            contentDescription = "بحث متقدم",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (uiState.searchQuery.isNotBlank()) {
                                        onCommitSearchHistory(uiState.searchQuery)
                                    }
                                }
                            ),
                            shape = RoundedCornerShape(50),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White,
                                focusedTextColor = Color(0xFF0B1F44),
                                unfocusedTextColor = Color(0xFF0B1F44)
                            )
                        )
                    }
                }

                // Multi-Select Batch Bar
                if (uiState.isSelectionMode) {
                    item {
                        WordsBatchActionBar(
                            selectedCount = uiState.selectedWordIds.size,
                            onSelectAll = onSelectAllFiltered,
                            onClearSelection = onClearSelection,
                            onFavoriteSelected = { onBatchSetFavorite(true) },
                            onQuizSelected = onBatchQuizSelected,
                            onExportSelected = {
                                val selWords = uiState.allWords.filter { uiState.selectedWordIds.contains(it.id) }
                                onBatchExportSelected(selWords)
                            },
                            onDeleteSelected = onBatchDelete
                        )
                    }
                }

                // Exact Match Anti-Duplication Status Banner
                val exact = uiState.exactSearchMatch
                if (uiState.searchQuery.isNotBlank() && exact != null) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    onCommitSearchHistory(uiState.searchQuery)
                                    onWordClick(exact.id)
                                }
                                .testTag("search_exact_match_banner"),
                            shape = RoundedCornerShape(18.dp),
                            color = palette.successBg,
                            border = BorderStroke(1.dp, palette.successBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = palette.successText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "الكلمة موجودة في قاموسك: «${exact.displayTitle()}» (اضغط للفتح)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.successText,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Recent Searches Chips
                if (uiState.searchQuery.isEmpty() && uiState.recentSearches.isNotEmpty()) {
                    item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = "عمليات البحث الأخيرة",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            items(uiState.recentSearches) { recent ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onSearchQueryChange(recent) },
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = recent,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            item {
                                Text(
                                    text = "مسح",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onClearSearchHistory() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Row 1 Filter Chips matching Reference Screen 2 («الكل | المفضلة | صعبة | بيانات ناقصة»)
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            ListFilterChipPill(
                                label = "الكل",
                                selected = isAllActive,
                                onClick = onClearAllFilters,
                                testTagName = "filter_all_chip"
                            )
                        }
                        item {
                            ListFilterChipPill(
                                label = "المفضلة",
                                selected = uiState.advancedFilter.favoritesOnly,
                                onClick = onToggleFavoritesFilter,
                                testTagName = "filter_favorites_chip"
                            )
                        }
                        item {
                            ListFilterChipPill(
                                label = "صعبة",
                                selected = uiState.advancedFilter.hardOrMistakesOnly,
                                onClick = onToggleHardFilter,
                                testTagName = "filter_hard_chip"
                            )
                        }
                        item {
                            ListFilterChipPill(
                                label = "بيانات ناقصة",
                                selected = uiState.advancedFilter.incompleteOnly,
                                onClick = onToggleIncompleteFilter,
                                testTagName = "filter_incomplete_chip"
                            )
                        }
                    }
                }

                // 3. Row 2 Dropdown Pills matching Reference Screen 2 («الكتاب v | الوحدة v | الدرس v | تجميع v | حقل البحث v»)
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Book Dropdown
                        item {
                            Box {
                                DropdownPillButton(
                                    label = uiState.quickBookFilter ?: "الكتاب",
                                    active = uiState.quickBookFilter != null,
                                    onClick = { showBookMenu = true },
                                    testTagName = "dropdown_book_pill"
                                )
                                DropdownMenu(
                                    expanded = showBookMenu,
                                    onDismissRequest = { showBookMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("كل الكتب") },
                                        onClick = {
                                            showBookMenu = false
                                            if (uiState.quickBookFilter != null) onToggleBookFilter(null)
                                        }
                                    )
                                    uiState.availableBooks.forEach { b ->
                                        DropdownMenuItem(
                                            text = { Text(b) },
                                            onClick = {
                                                showBookMenu = false
                                                onToggleBookFilter(b)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Unit Dropdown
                        item {
                            Box {
                                val activeUnit = uiState.advancedFilter.unitQuery
                                DropdownPillButton(
                                    label = if (activeUnit.isNotBlank()) "وحدة $activeUnit" else "الوحدة",
                                    active = activeUnit.isNotBlank(),
                                    onClick = { showUnitMenu = true },
                                    testTagName = "dropdown_unit_pill"
                                )
                                DropdownMenu(
                                    expanded = showUnitMenu,
                                    onDismissRequest = { showUnitMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("كل الوحدات") },
                                        onClick = {
                                            showUnitMenu = false
                                            onApplyAdvancedFilter(uiState.advancedFilter.copy(unitQuery = ""))
                                        }
                                    )
                                    uiState.availableUnits.forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text("وحدة $u") },
                                            onClick = {
                                                showUnitMenu = false
                                                onApplyAdvancedFilter(uiState.advancedFilter.copy(unitQuery = u))
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Lesson Dropdown
                        item {
                            Box {
                                val activeLesson = uiState.advancedFilter.lessonQuery
                                DropdownPillButton(
                                    label = if (activeLesson.isNotBlank()) "درس $activeLesson" else "الدرس",
                                    active = activeLesson.isNotBlank(),
                                    onClick = { showLessonMenu = true },
                                    testTagName = "dropdown_lesson_pill"
                                )
                                DropdownMenu(
                                    expanded = showLessonMenu,
                                    onDismissRequest = { showLessonMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("كل الدروس") },
                                        onClick = {
                                            showLessonMenu = false
                                            onApplyAdvancedFilter(uiState.advancedFilter.copy(lessonQuery = ""))
                                        }
                                    )
                                    uiState.availableLessons.forEach { l ->
                                        DropdownMenuItem(
                                            text = { Text("درس $l") },
                                            onClick = {
                                                showLessonMenu = false
                                                onApplyAdvancedFilter(uiState.advancedFilter.copy(lessonQuery = l))
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Browse Grouping Dropdown
                        item {
                            Box {
                                DropdownPillButton(
                                    label = uiState.browseGroupMode.labelAr,
                                    active = uiState.browseGroupMode != BrowseGroupMode.FLAT_LIST,
                                    onClick = { showGroupMenu = true },
                                    testTagName = "dropdown_group_pill"
                                )
                                DropdownMenu(
                                    expanded = showGroupMenu,
                                    onDismissRequest = { showGroupMenu = false }
                                ) {
                                    BrowseGroupMode.entries.forEach { mode ->
                                        DropdownMenuItem(
                                            text = { Text(mode.labelAr) },
                                            onClick = {
                                                showGroupMenu = false
                                                onBrowseGroupModeChange(mode)
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Search Field Dropdown
                        item {
                            Box {
                                DropdownPillButton(
                                    label = if (uiState.searchField == SearchField.ALL) "الحقل" else uiState.searchField.labelAr,
                                    active = uiState.searchField != SearchField.ALL,
                                    onClick = { showFieldMenu = true },
                                    testTagName = "dropdown_field_pill"
                                )
                                DropdownMenu(
                                    expanded = showFieldMenu,
                                    onDismissRequest = { showFieldMenu = false }
                                ) {
                                    SearchField.entries.forEach { f ->
                                        DropdownMenuItem(
                                            text = { Text(f.labelAr) },
                                            onClick = {
                                                showFieldMenu = false
                                                onSearchFieldChange(f)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Active filter count bar
                item {
                    AnimatedVisibility(visible = uiState.isSearchOrFilterActive) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp),
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

                // 4. Dictionary Rows OR Empty / Zero-Result Add Card
                if (uiState.allWordsCount == 0) {
                    item {
                        ListEmptyDictionaryCard(onAddFirstClick = { onAddWordClick("") })
                    }
                } else if (uiState.filteredWords.isEmpty()) {
                    item {
                        ListSearchNotFoundCard(
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
                                CompactReferenceWordRow(
                                    entry = wordEntry,
                                    selectionMode = uiState.isSelectionMode,
                                    isSelected = uiState.selectedWordIds.contains(wordEntry.id),
                                    onClick = {
                                        if (uiState.isSelectionMode) {
                                            onToggleWordSelection(wordEntry.id)
                                        } else {
                                            if (uiState.searchQuery.isNotBlank()) {
                                                onCommitSearchHistory(uiState.searchQuery)
                                            }
                                            onWordClick(wordEntry.id)
                                        }
                                    },
                                    onLongClick = { onToggleWordSelection(wordEntry.id) },
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
                                    CompactReferenceWordRow(
                                        entry = wordEntry,
                                        selectionMode = uiState.isSelectionMode,
                                        isSelected = uiState.selectedWordIds.contains(wordEntry.id),
                                        onClick = {
                                            if (uiState.isSelectionMode) onToggleWordSelection(wordEntry.id)
                                            else onWordClick(wordEntry.id)
                                        },
                                        onLongClick = { onToggleWordSelection(wordEntry.id) },
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
                                    CompactReferenceWordRow(
                                        entry = item.entry,
                                        badgeNote = subTag.ifBlank { null },
                                        selectionMode = uiState.isSelectionMode,
                                        isSelected = uiState.selectedWordIds.contains(item.entry.id),
                                        onClick = {
                                            if (uiState.isSelectionMode) onToggleWordSelection(item.entry.id)
                                            else onWordClick(item.entry.id)
                                        },
                                        onLongClick = { onToggleWordSelection(item.entry.id) },
                                        onToggleFavorite = { onToggleFavorite(item.entry) },
                                        onSpeakWord = onSpeakWord,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }

                        BrowseGroupMode.BY_STUDY_TAG -> {
                            studyTagGroups.forEach { (tagName, groupWords) ->
                                item(key = "tag_header_$tagName") {
                                    GroupSectionHeader(
                                        title = tagName,
                                        count = groupWords.size,
                                        icon = Icons.Rounded.Label,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                                itemsIndexed(
                                    items = groupWords,
                                    key = { idx, w -> "tag_${tagName}_${w.id}_$idx" }
                                ) { _, wordEntry ->
                                    CompactReferenceWordRow(
                                        entry = wordEntry,
                                        selectionMode = uiState.isSelectionMode,
                                        isSelected = uiState.selectedWordIds.contains(wordEntry.id),
                                        onClick = {
                                            if (uiState.isSelectionMode) onToggleWordSelection(wordEntry.id)
                                            else onWordClick(wordEntry.id)
                                        },
                                        onLongClick = { onToggleWordSelection(wordEntry.id) },
                                        onToggleFavorite = { onToggleFavorite(wordEntry) },
                                        onSpeakWord = onSpeakWord,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }

                        BrowseGroupMode.BY_DIFFICULTY -> {
                            difficultyGroups.forEach { (diffLabel, groupWords) ->
                                item(key = "diff_header_$diffLabel") {
                                    GroupSectionHeader(
                                        title = "مستوى الصعوبة: $diffLabel",
                                        count = groupWords.size,
                                        icon = Icons.Rounded.WarningAmber,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                                itemsIndexed(
                                    items = groupWords,
                                    key = { idx, w -> "diff_${diffLabel}_${w.id}_$idx" }
                                ) { _, wordEntry ->
                                    CompactReferenceWordRow(
                                        entry = wordEntry,
                                        selectionMode = uiState.isSelectionMode,
                                        isSelected = uiState.selectedWordIds.contains(wordEntry.id),
                                        onClick = {
                                            if (uiState.isSelectionMode) onToggleWordSelection(wordEntry.id)
                                            else onWordClick(wordEntry.id)
                                        },
                                        onLongClick = { onToggleWordSelection(wordEntry.id) },
                                        onToggleFavorite = { onToggleFavorite(wordEntry) },
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

    if (showSortSheet) {
        SortOptionsSheet(
            currentSort = uiState.sortOption,
            onSelect = onSortSelect,
            onDismiss = { showSortSheet = false }
        )
    }
}

@Composable
private fun ListFilterChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTagName: String
) {
    Surface(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .testTag(testTagName),
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun DropdownPillButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTagName: String
) {
    Surface(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .testTag(testTagName),
        shape = RoundedCornerShape(50),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun WordsBatchActionBar(
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onFavoriteSelected: () -> Unit,
    onQuizSelected: () -> Unit,
    onExportSelected: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("batch_action_bar"),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onClearSelection) {
                    Icon(Icons.Rounded.Close, contentDescription = "إلغاء التحديد", tint = Color.White)
                }
                Text(
                    text = "محدد: $selectedCount",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSelectAll) {
                    Icon(Icons.Rounded.SelectAll, contentDescription = "تحديد الكل", tint = Color.White)
                }
                IconButton(onClick = onFavoriteSelected) {
                    Icon(Icons.Rounded.Favorite, contentDescription = "تفضيل المحدد", tint = Color.White)
                }
                IconButton(onClick = onQuizSelected) {
                    Icon(Icons.Rounded.Quiz, contentDescription = "اختبار المحدد", tint = Color.White)
                }
                IconButton(onClick = onExportSelected) {
                    Icon(Icons.Rounded.Download, contentDescription = "تصدير المحدد", tint = Color.White)
                }
                IconButton(onClick = onDeleteSelected) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "حذف المحدد", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ListEmptyDictionaryCard(onAddFirstClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
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
                    .size(130.dp)
                    .clip(RoundedCornerShape(26.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = "ابدأ ببناء قاموسك الخاص",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "أضف كلمات المنهج مع معانيها والكتب والوحدات والدروس والمرادفات والمضادات وحروف الجر بدون نت.",
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
private fun ListSearchNotFoundCard(
    query: String,
    onAddMissingWordClick: () -> Unit,
    onClearFiltersClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
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
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = if (query.isNotBlank()) "لم يتم العثور على «$query»" else "لا توجد كلمات مطابقة للبحث",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "الكلمة غير مسجلة في قاموسك. يمكنك إضافتها الآن بضغطة زر.",
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
