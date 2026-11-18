package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.WordEntry
import com.example.data.repository.WordRepository
import com.example.domain.QuestionInteractionKind
import com.example.domain.SrsGrade
import com.example.domain.SrsScheduler
import com.example.ui.components.BookPillBadge
import com.example.ui.components.ManualSrsScheduleDialog
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.LocalQamusiColors
import com.example.ui.theme.SkyAccent
import com.example.ui.viewmodel.QuizSessionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuizSessionScreen(
    session: QuizSessionState,
    onRevealFlashcard: () -> Unit,
    onSubmitFlashcardGrade: (SrsGrade) -> Unit,
    onSelectMcqOption: (String) -> Unit,
    onTypedInputChange: (String) -> Unit,
    onSubmitTyping: () -> Unit,
    onNextQuestion: () -> Unit,
    onRestart: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onManualSchedule: (WordEntry, Int, Float, Boolean) -> Unit,
    onOpenWordDetail: (Long) -> Unit,
    onExit: () -> Unit
) {
    BackHandler { onExit() }

    val palette = LocalQamusiColors.current
    var editingSrsWord by remember { mutableStateOf<WordEntry?>(null) }
    var showHintLetter by remember(session.currentIndex) { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (session.isFinished) {
                            "نتيجة الاختبار"
                        } else {
                            "سؤال ${session.currentIndex + 1} من ${session.questions.size}"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onExit,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "إنهاء والعودة"
                        )
                    }
                },
                actions = {
                    val curWord = session.currentQuestion?.entry
                    if (!session.isFinished && curWord != null) {
                        IconButton(
                            onClick = { editingSrsWord = curWord },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EditCalendar,
                                contentDescription = "تعديل جدول المراجعة يدوياً",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (session.isFinished) {
                QuizSummaryContent(
                    session = session,
                    onManualAdjustWord = { editingSrsWord = it },
                    onOpenWordDetail = onOpenWordDetail,
                    onRestart = onRestart,
                    onExit = onExit
                )
            } else {
                val question = session.currentQuestion ?: return@Box
                val entry = question.entry
                val progress = (session.currentIndex + 1f) / session.questions.size.coerceAtLeast(1)
                val cardScale by animateFloatAsState(
                    targetValue = if (session.isAnswerRevealed) 1.01f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "card_scale"
                )

                Column(
                    modifier = Modifier
                        .widthIn(max = 700.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )

                    // Prompt Card with Question Type Badge & Offline TTS
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer(scaleX = cardScale, scaleY = cardScale),
                        shape = RoundedCornerShape(30.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = question.badgeLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }

                                if (entry.word.isNotBlank()) {
                                    IconButton(
                                        onClick = { onSpeakWord(entry.word) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("quiz_speak_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                            contentDescription = "نطق الكلمة",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (entry.books.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    entry.books.forEach { BookPillBadge(it) }
                                }
                            }

                            Text(
                                text = question.promptText,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            if (question.hintText.isNotBlank()) {
                                Text(
                                    text = question.hintText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    when (question.interactionKind) {
                        QuestionInteractionKind.FLASHCARD -> {
                            if (!session.isAnswerRevealed) {
                                Button(
                                    onClick = onRevealFlashcard,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("reveal_answer_button"),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Icon(imageVector = Icons.Rounded.Visibility, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("إظهار الإجابة والتفاصيل", style = MaterialTheme.typography.titleMedium)
                                }
                            }

                            AnimatedVisibility(
                                visible = session.isAnswerRevealed,
                                enter = fadeIn() + expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(26.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(18.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = question.answerText,
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (entry.ipa.isNotBlank()) {
                                                Text(
                                                    text = "النطق: ${entry.ipa}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (entry.synonyms.isNotEmpty()) {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    entry.synonyms.forEach { syn ->
                                                        Surface(shape = RoundedCornerShape(50), color = palette.synonymBg) {
                                                            Text(
                                                                text = syn,
                                                                color = palette.synonymText,
                                                                style = MaterialTheme.typography.labelMedium,
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            if (entry.antonyms.isNotEmpty()) {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    entry.antonyms.forEach { ant ->
                                                        Surface(shape = RoundedCornerShape(50), color = palette.antonymBg) {
                                                            Text(
                                                                text = "ضد: $ant",
                                                                color = palette.antonymText,
                                                                style = MaterialTheme.typography.labelMedium,
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            if (entry.definition.isNotBlank()) {
                                                Text("التعريف: ${entry.definition}", style = MaterialTheme.typography.bodyMedium)
                                            }
                                            if (entry.derivatives.isNotBlank()) {
                                                Text("الاشتقاقات: ${entry.derivatives}", style = MaterialTheme.typography.bodyMedium)
                                            }
                                            if (entry.expressions.isNotBlank()) {
                                                Text("التعبيرات: ${entry.expressions}", style = MaterialTheme.typography.bodyMedium)
                                            }
                                            val note = entry.importantNotes.ifBlank { entry.notes }
                                            if (note.isNotBlank()) {
                                                Text("ملاحظات: $note", style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }

                                    Text(
                                        text = "كيف كان تذكرك للكلمة؟ (يُحدّث جدول المراجعة تلقائياً)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            SrsGradePillButton(
                                                grade = SrsGrade.AGAIN,
                                                entry = entry,
                                                bgColor = palette.srsAgainBg,
                                                fgColor = palette.srsAgainText,
                                                onClick = { onSubmitFlashcardGrade(SrsGrade.AGAIN) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            SrsGradePillButton(
                                                grade = SrsGrade.HARD,
                                                entry = entry,
                                                bgColor = palette.srsHardBg,
                                                fgColor = palette.srsHardText,
                                                onClick = { onSubmitFlashcardGrade(SrsGrade.HARD) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            SrsGradePillButton(
                                                grade = SrsGrade.GOOD,
                                                entry = entry,
                                                bgColor = palette.srsGoodBg,
                                                fgColor = palette.srsGoodText,
                                                onClick = { onSubmitFlashcardGrade(SrsGrade.GOOD) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            SrsGradePillButton(
                                                grade = SrsGrade.EASY,
                                                entry = entry,
                                                bgColor = palette.srsEasyBg,
                                                fgColor = palette.srsEasyText,
                                                onClick = { onSubmitFlashcardGrade(SrsGrade.EASY) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        QuestionInteractionKind.MCQ -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                question.options.forEachIndexed { idx, option ->
                                    val answered = session.selectedMcqOption != null
                                    val isSelected = session.selectedMcqOption == option
                                    val normOpt = WordRepository.normalizeKey(option)
                                    val isRightOption = question.acceptedAnswers.any {
                                        WordRepository.normalizeKey(it) == normOpt
                                    } || normOpt == WordRepository.normalizeKey(question.answerText)

                                    val (bg, border, textColor) = when {
                                        !answered -> Triple(
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.outline,
                                            MaterialTheme.colorScheme.onSurface
                                        )
                                        isRightOption -> Triple(
                                            palette.successBg,
                                            palette.successBorder,
                                            palette.successText
                                        )
                                        isSelected && !isRightOption -> Triple(
                                            palette.dangerBg,
                                            palette.dangerBorder,
                                            palette.dangerText
                                        )
                                        else -> Triple(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            MaterialTheme.colorScheme.outlineVariant,
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(22.dp))
                                            .clickable(enabled = !answered) { onSelectMcqOption(option) }
                                            .testTag("mcq_option_$idx"),
                                        shape = RoundedCornerShape(22.dp),
                                        color = bg,
                                        border = BorderStroke(1.5.dp, border)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = option,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = textColor,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (answered && isRightOption) {
                                                Icon(
                                                    Icons.Rounded.CheckCircle,
                                                    contentDescription = null,
                                                    tint = palette.successBorder
                                                )
                                            } else if (answered && isSelected && !isRightOption) {
                                                Icon(
                                                    Icons.Rounded.Close,
                                                    contentDescription = null,
                                                    tint = palette.dangerBorder
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(visible = session.selectedMcqOption != null) {
                                Button(
                                    onClick = onNextQuestion,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("next_question_button"),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = if (session.currentIndex + 1 >= session.questions.size) "عرض النتيجة" else "السؤال التالي",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }

                        QuestionInteractionKind.TYPING -> {
                            val checked = session.lastTypingCorrect != null
                            val hintSample = question.acceptedAnswers.firstOrNull().orEmpty().ifBlank { question.answerText }

                            OutlinedTextField(
                                value = session.typedInput,
                                onValueChange = onTypedInputChange,
                                enabled = !checked,
                                label = { Text("اكتب الإجابة هنا") },
                                singleLine = true,
                                shape = RoundedCornerShape(22.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { if (!checked) onSubmitTyping() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("typing_answer_input")
                            )

                            if (!checked) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showHintLetter = true },
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تلميح")
                                    }
                                    Button(
                                        onClick = onSubmitTyping,
                                        enabled = session.typedInput.isNotBlank(),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("check_typing_button")
                                    ) {
                                        Text("تحقق")
                                    }
                                }
                                if (showHintLetter && hintSample.isNotBlank()) {
                                    Text(
                                        text = "تبدأ بـ: ${hintSample.take(2)}... (${hintSample.length} حرف)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                val ok = session.lastTypingCorrect == true
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    color = if (ok) palette.successBg else palette.dangerBg,
                                    border = BorderStroke(1.dp, if (ok) palette.successBorder else palette.dangerBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (ok) {
                                                "إجابة صحيحة! تم جدولة المراجعة القادمة"
                                            } else {
                                                "الإجابة الصحيحة: ${question.answerText}"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (ok) palette.successText else palette.dangerText
                                        )
                                    }
                                }
                                Button(
                                    onClick = onNextQuestion,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("next_question_button"),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = if (session.currentIndex + 1 >= session.questions.size) "عرض النتيجة" else "السؤال التالي",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingSrsWord?.let { target ->
        ManualSrsScheduleDialog(
            entry = target,
            onConfirm = { days, ease, reset ->
                onManualSchedule(target, days, ease, reset)
            },
            onDismiss = { editingSrsWord = null }
        )
    }
}

@Composable
private fun SrsGradePillButton(
    grade: SrsGrade,
    entry: WordEntry,
    bgColor: Color,
    fgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preview = remember(entry.id, grade) {
        SrsScheduler.scheduleNext(entry, grade)
    }
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .testTag("srs_grade_${grade.name.lowercase()}"),
        shape = RoundedCornerShape(22.dp),
        color = bgColor,
        border = BorderStroke(1.dp, fgColor.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = grade.labelAr,
                style = MaterialTheme.typography.titleSmall,
                color = fgColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = SrsScheduler.formatDaysAr(preview.intervalDays),
                style = MaterialTheme.typography.labelSmall,
                color = fgColor.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun QuizSummaryContent(
    session: QuizSessionState,
    onManualAdjustWord: (WordEntry) -> Unit,
    onOpenWordDetail: (Long) -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    val palette = LocalQamusiColors.current
    val total = session.records.size.coerceAtLeast(1)
    val correct = session.records.count { it.isCorrect }
    val percent = (correct * 100) / total

    Column(
        modifier = Modifier
            .widthIn(max = 700.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(listOf(CobaltDark, CobaltPrimary, SkyAccent))
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "$correct / ${session.records.size} ($percent%)",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White
                    )
                    Text(
                        text = "تم تحديث جدول المراجعة الذكي (SRS) لكل كلمة تلقائياً",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        session.records.forEach { rec ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onOpenWordDetail(rec.entry.id) },
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (rec.isCorrect) palette.successBorder else palette.dangerBorder
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rec.entry.displayTitle(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (rec.entry.arabicMeaning.isNotBlank()) {
                            Text(
                                text = rec.entry.arabicMeaning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onManualAdjustWord(rec.entry) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EditCalendar,
                                contentDescription = "تعديل الجدول",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = if (rec.isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
                            contentDescription = null,
                            tint = if (rec.isCorrect) palette.successBorder else palette.dangerBorder
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("إنهاء")
            }
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("إعادة الاختبار")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
