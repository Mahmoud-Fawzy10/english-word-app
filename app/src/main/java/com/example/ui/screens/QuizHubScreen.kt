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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.QuizConfig
import com.example.domain.QuizDirection
import com.example.domain.QuizEngine
import com.example.domain.QuizQuestionType
import com.example.domain.QuizScopeMode
import com.example.ui.components.RoundedSectionCard
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.SkyAccent
import com.example.ui.viewmodel.DictionaryUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuizHubScreen(
    uiState: DictionaryUiState,
    quizConfig: QuizConfig,
    snackbarHostState: SnackbarHostState,
    onUpdateConfig: ((QuizConfig) -> QuizConfig) -> Unit,
    onStartDueSrsReview: () -> Unit,
    onStartConfiguredQuiz: () -> Unit
) {
    val matchingCount = remember(uiState.allWords, quizConfig) {
        QuizEngine.filterWordsForConfig(uiState.allWords, quizConfig).size
    }

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
                // Blue Rounded Quiz & SRS Header
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
                                    Brush.linearGradient(
                                        listOf(CobaltDark, CobaltPrimary, SkyAccent)
                                    )
                                )
                                .statusBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "الاختبارات والمراجعة",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = "اختبر نفسك حسب الوحدة أو الدرس أو الصفحة أو عشوائياً، مع نظام التكرار المتباعد لتثبيت الحفظ.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    QuizStatChip(label = "مستحقة الآن: ${uiState.dueReviewCount}")
                                    QuizStatChip(label = "متقنة: ${uiState.masteredCount}")
                                    QuizStatChip(label = "الكل: ${uiState.allWordsCount}")
                                }
                            }
                        }
                    }
                }

                // Due SRS Review Prompt Card
                if (uiState.dueReviewCount > 0) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(26.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Psychology,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "لديك ${uiState.dueReviewCount} كلمة مستحقة للمراجعة",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "المراجعة الآن تحافظ على أعلى معدل تذكر.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(
                                    onClick = onStartDueSrsReview,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("start_due_srs_button")
                                ) {
                                    Text("مراجعة")
                                }
                            }
                        }
                    }
                }

                // Scope Selection Card
                item {
                    RoundedSectionCard(
                        title = "اختر نطاق الاختبار",
                        subtitle = quizConfig.scopeMode.descriptionAr,
                        icon = Icons.Rounded.Tune,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuizScopeMode.entries.forEach { mode ->
                                val selected = quizConfig.scopeMode == mode
                                FilterChip(
                                    selected = selected,
                                    onClick = { onUpdateConfig { it.copy(scopeMode = mode) } },
                                    label = { Text(mode.labelAr) },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        // Optional Book Filter
                        Text(
                            text = "تصفية حسب الكتاب (اختياري):",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = quizConfig.selectedBook == null,
                                onClick = { onUpdateConfig { it.copy(selectedBook = null) } },
                                label = { Text("كل الكتب") },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = Color.White
                                )
                            )
                            uiState.availableBooks.forEach { book ->
                                val selected = quizConfig.selectedBook == book
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        onUpdateConfig {
                                            it.copy(selectedBook = if (selected) null else book)
                                        }
                                    },
                                    label = { Text(book) },
                                    shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Unit Range Inputs
                        if (quizConfig.scopeMode == QuizScopeMode.BY_UNIT ||
                            quizConfig.scopeMode == QuizScopeMode.RANDOM_RANGE
                        ) {
                            RangeInputPair(
                                title = "نطاق الوحدة (رقم وحدة أو من - إلى):",
                                fromLabel = "من وحدة",
                                toLabel = "إلى وحدة",
                                fromValue = quizConfig.unitFrom,
                                toValue = quizConfig.unitTo,
                                onFromChange = { v -> onUpdateConfig { it.copy(unitFrom = v) } },
                                onToChange = { v -> onUpdateConfig { it.copy(unitTo = v) } },
                                quickChips = uiState.availableUnits
                            )
                        }

                        // Lesson Range Inputs
                        if (quizConfig.scopeMode == QuizScopeMode.BY_LESSON ||
                            quizConfig.scopeMode == QuizScopeMode.RANDOM_RANGE
                        ) {
                            RangeInputPair(
                                title = "نطاق الدرس (رقم درس أو من - إلى):",
                                fromLabel = "من درس",
                                toLabel = "إلى درس",
                                fromValue = quizConfig.lessonFrom,
                                toValue = quizConfig.lessonTo,
                                onFromChange = { v -> onUpdateConfig { it.copy(lessonFrom = v) } },
                                onToChange = { v -> onUpdateConfig { it.copy(lessonTo = v) } },
                                quickChips = uiState.availableLessons
                            )
                        }

                        // Page Range Inputs
                        if (quizConfig.scopeMode == QuizScopeMode.BY_PAGE ||
                            quizConfig.scopeMode == QuizScopeMode.RANDOM_RANGE
                        ) {
                            RangeInputPair(
                                title = "نطاق الصفحة (صفحة واحدة أو من - إلى):",
                                fromLabel = "من صفحة",
                                toLabel = "إلى صفحة",
                                fromValue = quizConfig.pageFrom,
                                toValue = quizConfig.pageTo,
                                onFromChange = { v -> onUpdateConfig { it.copy(pageFrom = v) } },
                                onToChange = { v -> onUpdateConfig { it.copy(pageTo = v) } },
                                quickChips = emptyList()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("كلمات المفضلة فقط", style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = quizConfig.favoritesOnly,
                                onCheckedChange = { fav -> onUpdateConfig { it.copy(favoritesOnly = fav) } }
                            )
                        }
                    }
                }

                // Question Style, Direction & Count Card
                item {
                    RoundedSectionCard(
                        title = "إعدادات الأسئلة",
                        icon = Icons.Rounded.Quiz,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text("طريقة الاختبار:", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuizQuestionType.entries.forEach { qType ->
                                val selected = quizConfig.questionType == qType
                                FilterChip(
                                    selected = selected,
                                    onClick = { onUpdateConfig { it.copy(questionType = qType) } },
                                    label = { Text(qType.labelAr) },
                                    shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        Text("اتجاه السؤال:", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuizDirection.entries.forEach { dir ->
                                val selected = quizConfig.direction == dir
                                FilterChip(
                                    selected = selected,
                                    onClick = { onUpdateConfig { it.copy(direction = dir) } },
                                    label = { Text(dir.labelAr) },
                                    shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        Text("عدد الأسئلة:", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5 to "5", 10 to "10", 15 to "15", 20 to "20", 0 to "الكل").forEach { (limit, label) ->
                                val selected = quizConfig.questionLimit == limit
                                FilterChip(
                                    selected = selected,
                                    onClick = { onUpdateConfig { it.copy(questionLimit = limit) } },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Shuffle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("ترتيب عشوائي للأسئلة", style = MaterialTheme.typography.bodyLarge)
                            }
                            Switch(
                                checked = quizConfig.shuffle,
                                onCheckedChange = { sh -> onUpdateConfig { it.copy(shuffle = sh) } }
                            )
                        }
                    }
                }

                // Start Quiz Action Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "الكلمات المتاحة لهذا الاختبار: $matchingCount كلمة",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Button(
                            onClick = onStartConfiguredQuiz,
                            enabled = matchingCount > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("start_quiz_button"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (matchingCount > 0) "ابدأ الاختبار الآن ($matchingCount)" else "لا توجد كلمات مطابقة للنطاق",
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RangeInputPair(
    title: String,
    fromLabel: String,
    toLabel: String,
    fromValue: String,
    toValue: String,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    quickChips: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = fromValue,
                onValueChange = onFromChange,
                label = { Text(fromLabel) },
                placeholder = { Text("مثال: 1") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = toValue,
                onValueChange = onToChange,
                label = { Text(toLabel) },
                placeholder = { Text("اختياري") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.weight(1f)
            )
        }
        if (quickChips.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickChips.take(10).forEach { chip ->
                    val active = fromValue == chip && (toValue.isBlank() || toValue == chip)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                if (active) {
                                    onFromChange("")
                                    onToChange("")
                                } else {
                                    onFromChange(chip)
                                    onToChange(chip)
                                }
                            },
                        shape = RoundedCornerShape(50),
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizStatChip(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.20f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
