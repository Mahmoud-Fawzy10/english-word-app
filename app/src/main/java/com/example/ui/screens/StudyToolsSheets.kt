package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.WordEntry
import com.example.data.model.WordFormConfig
import com.example.ui.components.RoundedBlueTextField
import com.example.ui.viewmodel.AudioPlaylistState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BulkQuickAddSheet(
    availableBooks: List<String>,
    availableTags: List<String>,
    onSubmitBulk: (rawText: String, defaultBook: String?, defaultTag: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rawText by remember { mutableStateOf("") }
    var selectedBook by remember { mutableStateOf<String?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "إضافة سريعة لعدة كلمات دفعة واحدة",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "اكتب كل كلمة في سطر بالصيغة التالية (ويُرفض أي تكرار تلقائياً):\nsustainable : مستدام\nresilient - مرن\nperseverance = مثابرة",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            RoundedBlueTextField(
                value = rawText,
                onValueChange = { rawText = it },
                label = "قائمة الكلمات",
                placeholder = "innovate : يبتكر\ninspire : يلهم",
                minLines = 5,
                maxLines = 10,
                modifier = Modifier.testTag("bulk_quick_add_input")
            )

            if (availableBooks.isNotEmpty()) {
                Text("إسناد لكتاب (اختياري):", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableBooks.take(6).forEach { b ->
                        val active = selectedBook == b
                        FilterChip(
                            selected = active,
                            onClick = { selectedBook = if (active) null else b },
                            label = { Text(b) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            if (availableTags.isNotEmpty()) {
                Text("إسناد لمجموعة دراسية (اختياري):", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableTags.take(6).forEach { tag ->
                        val active = selectedTag == tag
                        FilterChip(
                            selected = active,
                            onClick = { selectedTag = if (active) null else tag },
                            label = { Text(tag) },
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }
                }
            }

            Button(
                onClick = {
                    onSubmitBulk(rawText, selectedBook, selectedTag)
                    onDismiss()
                },
                enabled = rawText.isNotBlank(),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_bulk_quick_add")
            ) {
                Text("حفظ الكلمات (مع منع التكرار)", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormCustomizationSheet(
    config: WordFormConfig,
    onUpdateConfig: (WordFormConfig) -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "تخصيص حقول الإدخال",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(onClick = { onUpdateConfig(WordFormConfig()) }) {
                    Text("إظهار الكل")
                }
            }
            Text(
                text = "جميع الحقول اختيارية تماماً. أخفِ الأقسام التي لا تحتاجها لتسريع إضافة الكلمات.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FormToggleRow("الرمز الصوتي (IPA)", config.showIpa) {
                onUpdateConfig(config.copy(showIpa = it))
            }
            FormToggleRow("موقعها من الكلام", config.showPartOfSpeech) {
                onUpdateConfig(config.copy(showPartOfSpeech = it))
            }
            FormToggleRow("الكتب ومواقع المنهج", config.showBooksAndLocations) {
                onUpdateConfig(config.copy(showBooksAndLocations = it))
            }
            FormToggleRow("المرادفات والمضادات المتعددة", config.showSynonymsAntonyms) {
                onUpdateConfig(config.copy(showSynonymsAntonyms = it))
            }
            FormToggleRow("حروف الجر والمتلازمات اللفظية", config.showPrepositionsCollocations) {
                onUpdateConfig(config.copy(showPrepositionsCollocations = it))
            }
            FormToggleRow("التعريف والاشتقاقات والتعبيرات", config.showDefinitionDerivativesExpressions) {
                onUpdateConfig(config.copy(showDefinitionDerivativesExpressions = it))
            }
            FormToggleRow("الأمثلة السياقية", config.showExamples) {
                onUpdateConfig(config.copy(showExamples = it))
            }
            FormToggleRow("الملاحظات والملاحظات اللغوية والهامة", config.showNotesAndLinguistic) {
                onUpdateConfig(config.copy(showNotesAndLinguistic = it))
            }
            FormToggleRow("التلميح الذهني السريع", config.showPersonalHint) {
                onUpdateConfig(config.copy(showPersonalHint = it))
            }
            FormToggleRow("مستوى الصعوبة والمجموعات الدراسية", config.showDifficultyAndTags) {
                onUpdateConfig(config.copy(showDifficultyAndTags = it))
            }

            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("تم")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FormToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onCheckedChange(!checked) },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioStudySheet(
    playlistState: AudioPlaylistState,
    speechRate: Float,
    targetWords: List<WordEntry>,
    onSpeechRateChange: (Float) -> Unit,
    onStartPlaylist: (List<WordEntry>, Boolean) -> Unit,
    onTogglePause: () -> Unit,
    onSkip: (Int) -> Unit,
    onStop: () -> Unit,
    onRepeatTwiceChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var repeatTwice by remember(playlistState.repeatWordTwice) {
        mutableStateOf(playlistState.repeatWordTwice)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Headphones,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "مشغل الاستماع الصوتي (بدون نت)",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (playlistState.isActive && playlistState.currentWord != null) {
                val word = playlistState.currentWord!!
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${playlistState.currentIndex + 1} / ${playlistState.queue.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = word.displayTitle(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (word.ipa.isNotBlank()) {
                            Text(word.ipa, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (word.arabicMeaning.isNotBlank()) {
                            Text(
                                text = word.arabicMeaning,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onSkip(-1) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "السابق")
                    }
                    Surface(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .clickable { onTogglePause() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (playlistState.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = if (playlistState.isPaused) "استئناف" else "إيقاف مؤقت",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { onSkip(1) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = "التالي")
                    }
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            Icons.Rounded.Stop,
                            contentDescription = "إيقاف القائمة",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else {
                Text(
                    text = "استمع لنطق ${targetWords.count { it.word.isNotBlank() }} كلمة ومرادفاتها تلقائياً باستخدام محرك النطق المحلي.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { onStartPlaylist(targetWords, repeatTwice) },
                    enabled = targetWords.any { it.word.isNotBlank() },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("start_audio_study_button")
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("بدء قائمة الاستماع", style = MaterialTheme.typography.titleMedium)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تكرار نطق الكلمة مرتين", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = repeatTwice,
                    onCheckedChange = {
                        repeatTwice = it
                        onRepeatTwiceChange(it)
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("سرعة النطق", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = "${String.format("%.2f", speechRate)}x",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = speechRate,
                    onValueChange = onSpeechRateChange,
                    valueRange = 0.6f..1.4f
                )
            }

            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("إغلاق")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
