package com.example.ui.screens

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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.WordEntry
import com.example.ui.components.DonutMasteryRing
import com.example.ui.components.QamusiCurvedBlueHeader
import com.example.ui.components.ReferenceWhiteCard
import com.example.ui.components.RoundedSectionCard
import com.example.ui.theme.LocalQamusiColors
import com.example.ui.viewmodel.AudioPlaylistState
import com.example.ui.viewmodel.DictionaryUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyCenterScreen(
    uiState: DictionaryUiState,
    audioPlaylist: AudioPlaylistState,
    snackbarHostState: SnackbarHostState,
    onOpenStats: () -> Unit,
    onSetDailyGoal: (Int) -> Unit,
    onStartFlashcardReview: () -> Unit,
    onStartQuickMcqQuiz: () -> Unit,
    onStartReverseMcqQuiz: () -> Unit,
    onChallengeAction: (missionId: String) -> Unit,
    onStartHardVaultQuiz: () -> Unit,
    onStartUnitQuiz: (book: String, unit: String) -> Unit,
    onBrowseUnit: (book: String, unit: String) -> Unit,
    onOpenStudyTagQuiz: (tagName: String) -> Unit,
    onBrowseStudyTag: (tagName: String) -> Unit,
    onSpeakWord: (String) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onStartAudioPlaylist: (List<WordEntry>, Boolean) -> Unit,
    onToggleAudioPause: () -> Unit,
    onSkipAudio: (Int) -> Unit,
    onStopAudio: () -> Unit,
    onRepeatTwiceChange: (Boolean) -> Unit
) {
    val palette = LocalQamusiColors.current
    var showAudioSheet by remember { mutableStateOf(false) }
    var showGoalEditor by remember { mutableStateOf(false) }
    val sentenceDrafts = remember { mutableStateMapOf<String, String>() }
    val sentenceFeedback = remember { mutableStateMapOf<String, String>() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Curved Blue Header matching Reference Screen 4
                item {
                    QamusiCurvedBlueHeader(
                        title = "المذاكرة",
                        leadingIcon = Icons.Rounded.BarChart,
                        leadingContentDescription = "إحصائياتي",
                        onLeadingClick = onOpenStats,
                        trailingIcon = Icons.Rounded.Headphones,
                        trailingContentDescription = "الاستماع والنطق",
                        onTrailingClick = { showAudioSheet = true },
                        bottomCornerRadius = 28.dp
                    )
                }

                // 2. Centered Mastery Donut Ring matching Reference Screen 4
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        DonutMasteryRing(
                            percent = uiState.masteryPercent,
                            label = "معدل الإتقان",
                            ringSize = 138.dp
                        )
                    }
                }

                // 3. Daily Goal Card («هدف اليوم») matching Reference Screen 4
                item {
                    val goalRatio = (uiState.stats.reviewsToday.toFloat() / uiState.dailyGoalTarget.coerceAtLeast(1))
                        .coerceIn(0f, 1f)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { showGoalEditor = !showGoalEditor },
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFFEE2E2),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.TrackChanges,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "هدف اليوم",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "مراجعة ${uiState.dailyGoalTarget} كلمة",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${uiState.stats.reviewsToday} / ${uiState.dailyGoalTarget}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            LinearProgressIndicator(
                                progress = { goalRatio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape),
                                color = Color(0xFF22C55E),
                                trackColor = MaterialTheme.colorScheme.surface
                            )

                            if (showGoalEditor) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("ضبط الهدف:", style = MaterialTheme.typography.labelLarge)
                                    listOf(10, 15, 25, 40).forEach { g ->
                                        FilterChip(
                                            selected = uiState.dailyGoalTarget == g,
                                            onClick = { onSetDailyGoal(g) },
                                            label = { Text("$g") },
                                            shape = RoundedCornerShape(50),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. «اختر وضع المذاكرة» 2x2 Rounded Ice-Blue Grid matching Reference Screen 4
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "اختر وضع المذاكرة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StudyModeTile(
                                icon = Icons.Rounded.DashboardCustomize,
                                iconTint = Color(0xFF0EA5E9),
                                title = "بطاقات المراجعة",
                                onClick = onStartFlashcardReview,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("study_mode_flashcards")
                            )
                            StudyModeTile(
                                icon = Icons.Rounded.Headphones,
                                iconTint = MaterialTheme.colorScheme.primary,
                                title = "الاستماع والنطق",
                                onClick = { showAudioSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("study_mode_audio")
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StudyModeTile(
                                icon = Icons.Rounded.EmojiEvents,
                                iconTint = Color(0xFFF59E0B),
                                title = "اختيار سريع",
                                onClick = onStartQuickMcqQuiz,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("study_mode_quick_mcq")
                            )
                            StudyModeTile(
                                icon = Icons.Rounded.SportsEsports,
                                iconTint = Color(0xFF2563EB),
                                title = "اختيار عكسي",
                                onClick = onStartReverseMcqQuiz,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("study_mode_reverse_mcq")
                            )
                        }
                    }
                }

                // 5. Daily Challenges Missions
                item {
                    RoundedSectionCard(
                        title = "التحديات اليومية",
                        icon = Icons.Rounded.Flag,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        uiState.dailyChallenges.forEach { mission ->
                            val ratio = (mission.current.toFloat() / mission.target.coerceAtLeast(1)).coerceIn(0f, 1f)
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = if (mission.isCompleted) palette.successBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, if (mission.isCompleted) palette.successBorder else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (mission.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.EmojiEvents,
                                                contentDescription = null,
                                                tint = if (mission.isCompleted) palette.successText else MaterialTheme.colorScheme.primary
                                            )
                                            Column {
                                                Text(mission.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text(mission.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .clickable { onChallengeAction(mission.id) },
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = mission.actionLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Offline Sentence Builder Practice
                if (uiState.sentencePrompts.isNotEmpty()) {
                    item {
                        RoundedSectionCard(
                            title = "تطبيق الجمل السياقية (أوفلاين)",
                            subtitle = "تدرّب على توظيف كلماتك ومتلازماتها في جمل من تأليفك",
                            icon = Icons.Rounded.EditNote,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            uiState.sentencePrompts.take(3).forEach { prompt ->
                                val draft = sentenceDrafts[prompt.titleWord].orEmpty()
                                val fb = sentenceFeedback[prompt.titleWord]
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${prompt.titleWord} • ${prompt.arabicMeaning}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = prompt.promptGuideAr,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(onClick = { onSpeakWord(prompt.targetPhrase) }) {
                                                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "نطق", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        OutlinedTextField(
                                            value = draft,
                                            onValueChange = { sentenceDrafts[prompt.titleWord] = it },
                                            placeholder = { Text("اكتب جملتك بالإنجليزية هنا...") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (!fb.isNullOrBlank()) {
                                                Text(
                                                    text = fb,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (fb.startsWith("ممتاز")) Color(0xFF15803D) else Color(0xFFB45309),
                                                    modifier = Modifier.weight(1f)
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                            Button(
                                                onClick = {
                                                    val normDraft = draft.lowercase()
                                                    val usedTarget = normDraft.contains(prompt.titleWord.lowercase()) ||
                                                        normDraft.contains(prompt.targetPhrase.lowercase())
                                                    sentenceFeedback[prompt.titleWord] = when {
                                                        draft.isBlank() -> "اكتب جملة أولاً"
                                                        usedTarget -> "ممتاز! استخدمت «${prompt.targetPhrase}» بنجاح"
                                                        else -> "حاول تضمين «${prompt.titleWord}» في الجملة"
                                                    }
                                                },
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text("تحقق")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. Hard & Mistakes Vault Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onStartHardVaultQuiz() }
                            .testTag("hard_vault_button"),
                        shape = RoundedCornerShape(22.dp),
                        color = palette.warningBg,
                        border = BorderStroke(1.dp, palette.warningBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = palette.warningText)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "بنك الكلمات الصعبة والأخطاء (${uiState.hardOrMistakesCount})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.warningText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "اضغط لبدء جلسة مراجعة مركزة على أصعب الكلمات",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.warningText
                                )
                            }
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = palette.warningText)
                        }
                    }
                }

                // 8. Study Groups Hub
                if (uiState.studyGroups.isNotEmpty()) {
                    item {
                        RoundedSectionCard(
                            title = "المجموعات الدراسية",
                            icon = Icons.Rounded.Label,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            uiState.studyGroups.forEach { group ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "#${group.tagName}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${group.wordCount} كلمة • متقن ${group.masteredCount} • مستحق ${group.dueCount}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = { onBrowseStudyTag(group.tagName) },
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text("عرض")
                                            }
                                            Button(
                                                onClick = { onOpenStudyTagQuiz(group.tagName) },
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text("اختبار")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 9. Curriculum Review Map
                item {
                    RoundedSectionCard(
                        title = "خريطة مراجعة المنهج",
                        subtitle = "تقدمك حسب الكتب والوحدات والدروس",
                        icon = Icons.Rounded.Map,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        if (uiState.curriculumMap.isEmpty()) {
                            Text(
                                text = "أضف كلمات وحدد الكتاب والوحدة لعرض خريطة المذاكرة التفاعلية.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.curriculumMap.forEach { bookNode ->
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "${bookNode.bookName} (${bookNode.masteredCount}/${bookNode.totalCount} متقن)",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    bookNode.units.forEach { unitNode ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(18.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        val unitLabel = if (unitNode.unitName == "بدون وحدة") "بدون وحدة" else "الوحدة ${unitNode.unitName}"
                                                        Text(
                                                            text = "$unitLabel — ${unitNode.masteryPercent}% إتقان",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "${unitNode.totalCount} كلمة • مستحق ${unitNode.dueCount} • دروس: ${unitNode.lessons.size}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        OutlinedButton(
                                                            onClick = { onBrowseUnit(bookNode.bookName, unitNode.unitName) },
                                                            shape = RoundedCornerShape(50)
                                                        ) {
                                                            Text("تصفح")
                                                        }
                                                        if (unitNode.unitName != "بدون وحدة") {
                                                            Button(
                                                                onClick = { onStartUnitQuiz(bookNode.bookName, unitNode.unitName) },
                                                                shape = RoundedCornerShape(50)
                                                            ) {
                                                                Text("اختبر")
                                                            }
                                                        }
                                                    }
                                                }
                                                LinearProgressIndicator(
                                                    progress = { unitNode.masteryPercent / 100f },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAudioSheet) {
        AudioStudySheet(
            playlistState = audioPlaylist,
            speechRate = uiState.ttsSpeechRate,
            targetWords = uiState.allWords,
            onSpeechRateChange = onSpeechRateChange,
            onStartPlaylist = onStartAudioPlaylist,
            onTogglePause = onToggleAudioPause,
            onSkip = onSkipAudio,
            onStop = onStopAudio,
            onRepeatTwiceChange = onRepeatTwiceChange,
            onDismiss = { showAudioSheet = false }
        )
    }
}

@Composable
private fun StudyModeTile(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}
