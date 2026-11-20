package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.TrackChanges
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.QuizConfig
import com.example.domain.QuizDirection
import com.example.domain.QuizEngine
import com.example.domain.QuizQuestionType
import com.example.domain.QuizScopeMode
import com.example.ui.components.QamusiCurvedBlueHeader
import com.example.ui.components.QuizClipboardArtwork
import com.example.ui.components.ReferenceWhiteCard
import com.example.ui.viewmodel.DictionaryUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuizHubScreen(
    uiState: DictionaryUiState,
    quizConfig: QuizConfig,
    snackbarHostState: SnackbarHostState,
    onUpdateConfig: ((QuizConfig) -> QuizConfig) -> Unit,
    onStartDueSrsReview: () -> Unit,
    onStartConfiguredQuiz: () -> Unit,
    onOpenAchievements: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    BackHandler { onBack() }

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
                contentPadding = PaddingValues(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Curved Blue Header matching Reference Screen 5 («الاختبارات» + Trophy + Close X)
                item {
                    QamusiCurvedBlueHeader(
                        title = "الاختبارات",
                        leadingIcon = Icons.Rounded.EmojiEvents,
                        leadingContentDescription = "الإنجازات",
                        onLeadingClick = onOpenAchievements,
                        trailingIcon = Icons.Rounded.Close,
                        trailingContentDescription = "إغلاق",
                        onTrailingClick = onBack,
                        bottomCornerRadius = 28.dp
                    )
                }

                // 2. Clipboard + Stopwatch Illustration matching Reference Screen 5
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        QuizClipboardArtwork()
                    }
                }

                // 3. Due SRS Prompt (if words are due right now)
                if (uiState.dueReviewCount > 0) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = "لديك ${uiState.dueReviewCount} كلمة مستحقة للمراجعة",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = onStartDueSrsReview,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("start_due_srs_button")
                                ) {
                                    Text("راجع الآن")
                                }
                            }
                        }
                    }
                }

                // 4. «اختر نوع الاختبار» 2x2 Primary Mode Grid matching Reference Screen 5
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "اختر نوع الاختبار",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuizTypeTile(
                                icon = Icons.Rounded.TrackChanges,
                                iconTint = Color(0xFFDC2626),
                                title = "حسب الوحدة أو الكتاب",
                                selected = quizConfig.scopeMode == QuizScopeMode.BY_UNIT &&
                                    quizConfig.questionType != QuizQuestionType.TYPING &&
                                    quizConfig.questionType != QuizQuestionType.MATCH_PAIRS,
                                onClick = {
                                    onUpdateConfig {
                                        it.copy(
                                            scopeMode = QuizScopeMode.BY_UNIT,
                                            questionType = if (it.questionType == QuizQuestionType.TYPING || it.questionType == QuizQuestionType.MATCH_PAIRS) {
                                                QuizQuestionType.MULTIPLE_CHOICE
                                            } else it.questionType
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quiz_scope_by_unit")
                            )
                            QuizTypeTile(
                                icon = Icons.Rounded.Shuffle,
                                iconTint = MaterialTheme.colorScheme.primary,
                                title = "اختبار عشوائي\n(من كل الكلمات)",
                                selected = quizConfig.scopeMode == QuizScopeMode.RANDOM_RANGE &&
                                    quizConfig.questionType != QuizQuestionType.TYPING &&
                                    quizConfig.questionType != QuizQuestionType.MATCH_PAIRS,
                                onClick = {
                                    onUpdateConfig {
                                        it.copy(
                                            scopeMode = QuizScopeMode.RANDOM_RANGE,
                                            questionType = if (it.questionType == QuizQuestionType.TYPING || it.questionType == QuizQuestionType.MATCH_PAIRS) {
                                                QuizQuestionType.MULTIPLE_CHOICE
                                            } else it.questionType
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quiz_scope_random_range")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            QuizTypeTile(
                                icon = Icons.Rounded.Keyboard,
                                iconTint = MaterialTheme.colorScheme.primary,
                                title = "الإملاء\n(اكتب الكلمة)",
                                selected = quizConfig.questionType == QuizQuestionType.TYPING,
                                onClick = { onUpdateConfig { it.copy(questionType = QuizQuestionType.TYPING) } },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quiz_type_typing")
                            )
                            QuizTypeTile(
                                icon = Icons.Rounded.Extension,
                                iconTint = Color(0xFF0EA5E9),
                                title = "المطابقة\n(كلمة ↔ معنى)",
                                selected = quizConfig.questionType == QuizQuestionType.MATCH_PAIRS,
                                onClick = { onUpdateConfig { it.copy(questionType = QuizQuestionType.MATCH_PAIRS) } },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quiz_type_match_pairs")
                            )
                        }
                    }
                }

                // 5. Full-Width Cobalt CTA «ابدأ الاختبار >» matching Reference Screen 5
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onStartConfiguredQuiz,
                            enabled = matchingCount > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("start_quiz_button"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = if (matchingCount > 0) "ابدأ الاختبار  >  ($matchingCount كلمة)" else "لا توجد كلمات مطابقة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 6. Extended Quiz Settings & Customization Card (preserving all scopes, question types, ranges)
                item {
                    ReferenceWhiteCard(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text("نطاق الأسئلة الإضافي:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QuizScopeMode.entries.forEach { mode ->
                                val selected = quizConfig.scopeMode == mode
                                FilterChip(
                                    selected = selected,
                                    onClick = { onUpdateConfig { it.copy(scopeMode = mode) } },
                                    label = { Text(mode.labelAr) },
                                    shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Text("نمط الأسئلة والميني-كويز:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        if (quizConfig.scopeMode == QuizScopeMode.BY_STUDY_TAG && uiState.availableStudyTags.isNotEmpty()) {
                            Text("المجموعة الدراسية:", style = MaterialTheme.typography.labelLarge)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = quizConfig.selectedStudyTag == null,
                                    onClick = { onUpdateConfig { it.copy(selectedStudyTag = null) } },
                                    label = { Text("الكل") },
                                    shape = RoundedCornerShape(50)
                                )
                                uiState.availableStudyTags.forEach { tag ->
                                    val active = quizConfig.selectedStudyTag == tag
                                    FilterChip(
                                        selected = active,
                                        onClick = { onUpdateConfig { it.copy(selectedStudyTag = if (active) null else tag) } },
                                        label = { Text("#$tag") },
                                        shape = RoundedCornerShape(50)
                                    )
                                }
                            }
                        }

                        Text("تصفية حسب الكتاب:", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = quizConfig.selectedBook == null,
                                onClick = { onUpdateConfig { it.copy(selectedBook = null) } },
                                label = { Text("كل الكتب") },
                                shape = RoundedCornerShape(50)
                            )
                            uiState.availableBooks.forEach { book ->
                                val selected = quizConfig.selectedBook == book
                                FilterChip(
                                    selected = selected,
                                    onClick = { onUpdateConfig { it.copy(selectedBook = if (selected) null else book) } },
                                    label = { Text(book) },
                                    shape = RoundedCornerShape(50)
                                )
                            }
                        }

                        if (quizConfig.scopeMode == QuizScopeMode.BY_UNIT || quizConfig.scopeMode == QuizScopeMode.RANDOM_RANGE) {
                            QuizRangePair(
                                title = "نطاق الوحدة:",
                                fromLabel = "من وحدة",
                                toLabel = "إلى وحدة",
                                fromValue = quizConfig.unitFrom,
                                toValue = quizConfig.unitTo,
                                onFromChange = { v -> onUpdateConfig { it.copy(unitFrom = v) } },
                                onToChange = { v -> onUpdateConfig { it.copy(unitTo = v) } },
                                quickChips = uiState.availableUnits
                            )
                        }

                        if (quizConfig.scopeMode == QuizScopeMode.BY_LESSON) {
                            QuizRangePair(
                                title = "نطاق الدرس:",
                                fromLabel = "من درس",
                                toLabel = "إلى درس",
                                fromValue = quizConfig.lessonFrom,
                                toValue = quizConfig.lessonTo,
                                onFromChange = { v -> onUpdateConfig { it.copy(lessonFrom = v) } },
                                onToChange = { v -> onUpdateConfig { it.copy(lessonTo = v) } },
                                quickChips = uiState.availableLessons
                            )
                        }

                        if (quizConfig.scopeMode == QuizScopeMode.BY_PAGE) {
                            QuizRangePair(
                                title = "نطاق الصفحة:",
                                fromLabel = "من صفحة",
                                toLabel = "إلى صفحة",
                                fromValue = quizConfig.pageFrom,
                                toValue = quizConfig.pageTo,
                                onFromChange = { v -> onUpdateConfig { it.copy(pageFrom = v) } },
                                onToChange = { v -> onUpdateConfig { it.copy(pageTo = v) } },
                                quickChips = emptyList()
                            )
                        }

                        AnimatedVisibility(
                            visible = quizConfig.questionType == QuizQuestionType.FLASHCARD_SRS ||
                                quizConfig.questionType == QuizQuestionType.MULTIPLE_CHOICE ||
                                quizConfig.questionType == QuizQuestionType.TYPING ||
                                quizConfig.questionType == QuizQuestionType.MIXED
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("اتجاه السؤال:", style = MaterialTheme.typography.labelLarge)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    QuizDirection.entries.forEach { dir ->
                                        val selected = quizConfig.direction == dir
                                        FilterChip(
                                            selected = selected,
                                            onClick = { onUpdateConfig { it.copy(direction = dir) } },
                                            label = { Text(dir.labelAr) },
                                            shape = RoundedCornerShape(50)
                                        )
                                    }
                                }
                            }
                        }

                        Text("عدد الأسئلة:", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(5 to "5", 10 to "10", 15 to "15", 20 to "20", 0 to "الكل").forEach { (limit, label) ->
                                FilterChip(
                                    selected = quizConfig.questionLimit == limit,
                                    onClick = { onUpdateConfig { it.copy(questionLimit = limit) } },
                                    label = { Text(label) },
                                    shape = RoundedCornerShape(50)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("المفضلة فقط", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = quizConfig.favoritesOnly,
                                onCheckedChange = { fav -> onUpdateConfig { it.copy(favoritesOnly = fav) } }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ترتيب عشوائي للأسئلة", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = quizConfig.shuffle,
                                onCheckedChange = { sh -> onUpdateConfig { it.copy(shuffle = sh) } }
                            )
                        }

                        if (quizConfig.questionType == QuizQuestionType.FLASHCARD_SRS) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text("إعادة البطاقات المنسية بالنهاية", style = MaterialTheme.typography.bodyMedium)
                                }
                                Switch(
                                    checked = quizConfig.requeueForgottenCards,
                                    onCheckedChange = { req -> onUpdateConfig { it.copy(requeueForgottenCards = req) } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizTypeTile(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                minLines = 2
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuizRangePair(
    title: String,
    fromLabel: String,
    toLabel: String,
    fromValue: String,
    toValue: String,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    quickChips: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = fromValue,
                onValueChange = onFromChange,
                label = { Text(fromLabel) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
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
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
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
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
