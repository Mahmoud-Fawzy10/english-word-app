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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WordEntry
import com.example.data.model.WordRevision
import com.example.domain.LearningAnalyticsEngine
import com.example.ui.components.BookPillBadge
import com.example.ui.components.DifficultyPillBadge
import com.example.ui.components.ManualSrsScheduleDialog
import com.example.ui.components.QamusiCurvedBlueHeader
import com.example.ui.components.SrsDetailCard
import com.example.ui.theme.LocalQamusiColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordDetailScreen(
    word: WordEntry?,
    relatedWords: List<WordEntry> = emptyList(),
    revisions: List<WordRevision> = emptyList(),
    onBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onOpenRelatedWord: (Long) -> Unit = {},
    onOpenRelationshipMap: (Long) -> Unit = {},
    onRestoreRevision: (Long) -> Unit = {},
    onDeleteConfirm: (Long) -> Unit,
    onToggleFavorite: (WordEntry) -> Unit,
    onSpeakWord: (String) -> Unit,
    onReviewNowClick: (WordEntry) -> Unit,
    onManualSrsUpdate: (WordEntry, Int, Float, Boolean) -> Unit,
    onShareCardText: (String) -> Unit = {},
    onCopiedMessage: (String) -> Unit
) {
    BackHandler { onBack() }

    val palette = LocalQamusiColors.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showManualSrsDialog by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    var expandMeaning by remember { mutableStateOf(true) }
    var expandSynonyms by remember { mutableStateOf(true) }
    var expandAntonyms by remember { mutableStateOf(true) }
    var expandDerivatives by remember { mutableStateOf(true) }
    var expandPrepositions by remember { mutableStateOf(true) }
    var expandCollocationsAndExamples by remember { mutableStateOf(true) }
    var expandHistory by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (word != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 10.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Favorite Pill («مفضلة ♥») matching Reference Screen 3
                        OutlinedButton(
                            onClick = { onToggleFavorite(word) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("detail_favorite_button"),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = if (word.isFavorite) "في المفضلة" else "مفضلة",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (word.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Review Now Pill («مراجعة ↻») matching Reference Screen 3
                        Button(
                            onClick = { onReviewNowClick(word) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("detail_review_button"),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مراجعة", fontWeight = FontWeight.Bold)
                        }

                        // Compact Delete Icon Button
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("delete_word_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "حذف الكلمة",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
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
            if (word == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    QamusiCurvedBlueHeader(
                        title = "تفاصيل الكلمة",
                        leadingIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                        onLeadingClick = onBack
                    )
                    Spacer(modifier = Modifier.height(36.dp))
                    Text("لم يتم العثور على هذه الكلمة", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onBack, shape = RoundedCornerShape(50)) {
                        Text("العودة")
                    }
                }
            } else {
                val intelligence = remember(word) { LearningAnalyticsEngine.computeWordIntelligence(word) }
                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 700.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Curved Blue Header with Share, Copy & Edit
                    item {
                        QamusiCurvedBlueHeader(
                            title = "تفاصيل الكلمة",
                            leadingIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                            onLeadingClick = onBack,
                            secondaryTrailingIcon = Icons.Rounded.Edit,
                            secondaryTrailingDescription = "تعديل الكلمة",
                            onSecondaryTrailingClick = { onEditClick(word.id) },
                            trailingIcon = Icons.Rounded.Share,
                            trailingContentDescription = "مشاركة بطاقة الكلمة",
                            onTrailingClick = { onShareCardText(formatWordForCopy(word)) },
                            bottomCornerRadius = 26.dp
                        )
                    }

                    // 2. Centered Word Hero Header matching Reference Screen 3
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = word.displayTitle(),
                                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                if (word.word.isNotBlank()) {
                                    IconButton(
                                        onClick = { onSpeakWord(word.word) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("detail_speak_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                            contentDescription = "نطق الكلمة محلياً",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (word.ipa.isNotBlank()) {
                                Text(
                                    text = word.ipa,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("detail_ipa_text")
                                )
                            }

                            if (word.arabicMeaning.isNotBlank() && word.word.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = word.arabicMeaning,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.Star,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Quick actions bar: Edit pill + Copy pill + Difficulty & POS chips
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                word.partsOfSpeech.forEach { pos ->
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = pos,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                if (word.difficulty > 0) {
                                    DifficultyPillBadge(difficulty = word.difficulty)
                                }
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onEditClick(word.id) }
                                        .testTag("edit_word_button"),
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        Text("تعديل", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable {
                                            clipboard.setText(AnnotatedString(formatWordForCopy(word)))
                                            onCopiedMessage("تم نسخ بطاقة الكلمة")
                                        },
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Text("نسخ", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Word Intelligence Strip (Mastery % + Missing Fields Indicator)
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
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
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "ذكاء الكلمة: ${intelligence.readinessLabelAr}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "${intelligence.masteryScorePercent}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { intelligence.masteryScorePercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                                if (intelligence.missingFieldsAr.isNotEmpty()) {
                                    Text(
                                        text = "حقول غير مضافة: ${intelligence.missingFieldsAr.take(3).joinToString(" • ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 4. Accordion Section: المعنى والمواقع والكتاب
                    val validLocations = word.locations.filter { it.isNotBlank() }
                    if (word.definition.isNotBlank() || word.arabicMeaning.isNotBlank() || word.books.isNotEmpty() || validLocations.isNotEmpty()) {
                        item {
                            DetailAccordionCard(
                                title = "المعنى",
                                expanded = expandMeaning,
                                onToggle = { expandMeaning = !expandMeaning }
                            ) {
                                if (word.definition.isNotBlank()) {
                                    Text(
                                        text = word.definition,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                } else if (word.arabicMeaning.isNotBlank()) {
                                    Text(
                                        text = word.arabicMeaning,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (word.books.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        word.books.forEach { BookPillBadge(it) }
                                    }
                                }
                                if (validLocations.isNotEmpty()) {
                                    validLocations.forEach { loc ->
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Text(loc.formattedLabel(), style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Accordion: المرادفات
                    if (word.synonyms.isNotEmpty()) {
                        item {
                            DetailAccordionCard(
                                title = "المرادفات",
                                preview = word.synonyms.joinToString(" • "),
                                expanded = expandSynonyms,
                                onToggle = { expandSynonyms = !expandSynonyms }
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    word.synonyms.forEach { syn ->
                                        Surface(shape = RoundedCornerShape(50), color = palette.synonymBg) {
                                            Text(
                                                text = syn,
                                                color = palette.synonymText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. Accordion: المضادات
                    if (word.antonyms.isNotEmpty()) {
                        item {
                            DetailAccordionCard(
                                title = "المضادات",
                                preview = word.antonyms.joinToString(" • "),
                                expanded = expandAntonyms,
                                onToggle = { expandAntonyms = !expandAntonyms }
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    word.antonyms.forEach { ant ->
                                        Surface(shape = RoundedCornerShape(50), color = palette.antonymBg) {
                                            Text(
                                                text = ant,
                                                color = palette.antonymText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 7. Accordion: الاشتقاقات
                    if (word.derivatives.isNotBlank()) {
                        item {
                            DetailAccordionCard(
                                title = "الاشتقاقات",
                                preview = word.derivatives,
                                expanded = expandDerivatives,
                                onToggle = { expandDerivatives = !expandDerivatives }
                            ) {
                                Text(
                                    text = word.derivatives,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // 8. Accordion: حروف الجر
                    if (word.prepositions.isNotEmpty()) {
                        item {
                            DetailAccordionCard(
                                title = "حروف الجر",
                                preview = word.prepositions.joinToString(" • "),
                                expanded = expandPrepositions,
                                onToggle = { expandPrepositions = !expandPrepositions }
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    word.prepositions.forEach { prep ->
                                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
                                            Text(
                                                text = prep,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 9. Accordion: المتلازمات اللفظية والأمثلة والتعبيرات والملاحظات
                    if (word.collocations.isNotEmpty() || word.expressions.isNotBlank() ||
                        word.examples.isNotEmpty() || word.linguisticNotes.isNotBlank() ||
                        word.personalHint.isNotBlank() || word.notes.isNotBlank() || word.importantNotes.isNotBlank()
                    ) {
                        item {
                            DetailAccordionCard(
                                title = "المتلازمات اللفظية والأمثلة والتعبيرات",
                                expanded = expandCollocationsAndExamples,
                                onToggle = { expandCollocationsAndExamples = !expandCollocationsAndExamples }
                            ) {
                                if (word.collocations.isNotEmpty()) {
                                    Text("المتلازمات اللفظية:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        word.collocations.forEach { col ->
                                            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                                                Text(
                                                    text = col,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (word.expressions.isNotBlank()) {
                                    DetailFieldBlock(label = "التعبيرات اللغوية", content = word.expressions)
                                }
                                word.examples.forEach { example ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(text = example, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                            IconButton(onClick = { onSpeakWord(example) }) {
                                                Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = "نطق المثال", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                                if (word.linguisticNotes.isNotBlank()) {
                                    DetailFieldBlock(label = "الملاحظات اللغوية", content = word.linguisticNotes)
                                }
                                if (word.personalHint.isNotBlank()) {
                                    DetailFieldBlock(label = "التلميح الذهني السريع", content = word.personalHint)
                                }
                                if (word.notes.isNotBlank()) {
                                    DetailFieldBlock(label = "ملاحظات عامة", content = word.notes)
                                }
                                if (word.importantNotes.isNotBlank()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = palette.importantNoteBg,
                                        border = BorderStroke(1.dp, palette.importantNoteBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Text(word.importantNotes, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 10. Link to Relationship Map Screen («خريطة العلاقات بين الكلمات»)
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onOpenRelationshipMap(word.id) }
                                .testTag("open_relationship_map_btn"),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        Icon(Icons.Rounded.Hub, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = "خريطة العلاقات بين الكلمات",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = "عرض الشبكة >",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (relatedWords.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        relatedWords.take(6).forEach { rel ->
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable { onOpenRelatedWord(rel.id) },
                                                shape = RoundedCornerShape(50),
                                                color = MaterialTheme.colorScheme.surface
                                            ) {
                                                Text(
                                                    text = rel.displayTitle(),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 11. Word Revision History & Restore («سجل تطور الكلمة»)
                    if (revisions.isNotEmpty()) {
                        item {
                            DetailAccordionCard(
                                title = "سجل تطور الكلمة (${revisions.size})",
                                expanded = expandHistory,
                                onToggle = { expandHistory = !expandHistory }
                            ) {
                                val fmt = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
                                revisions.forEach { rev ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                                Column {
                                                    Text(rev.changeSummaryAr, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                                    Text(fmt.format(Date(rev.editedAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            OutlinedButton(
                                                onClick = { onRestoreRevision(rev.id) },
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("استعادة")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 12. Spaced Repetition (SRS) Detail Card
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SrsDetailCard(
                                entry = word,
                                onReviewNowClick = { onReviewNowClick(word) },
                                onManualAdjustClick = { showManualSrsDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showManualSrsDialog && word != null) {
        ManualSrsScheduleDialog(
            entry = word,
            onConfirm = { days, ease, reset ->
                onManualSrsUpdate(word, days, ease, reset)
            },
            onDismiss = { showManualSrsDialog = false }
        )
    }

    if (showDeleteDialog && word != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("حذف الكلمة؟") },
            text = { Text("هل أنت متأكد من حذف «${word.displayTitle()}» من قاموسك؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteConfirm(word.id)
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun DetailAccordionCard(
    title: String,
    preview: String? = null,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "طي" else "توسيع",
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    if (!expanded && !preview.isNullOrBlank()) {
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.size(24.dp))
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun DetailFieldBlock(label: String, content: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatWordForCopy(word: WordEntry): String = buildString {
    if (word.word.isNotBlank()) appendLine(word.word)
    if (word.ipa.isNotBlank()) appendLine("النطق (IPA): ${word.ipa}")
    if (word.arabicMeaning.isNotBlank()) appendLine("المعنى: ${word.arabicMeaning}")
    if (word.books.isNotEmpty()) appendLine("الكتاب: ${word.books.joinToString("، ")}")
    val locs = word.locations.filter { it.isNotBlank() }.joinToString(" | ") { it.formattedLabel() }
    if (locs.isNotBlank()) appendLine("المواقع: $locs")
    if (word.partsOfSpeech.isNotEmpty()) appendLine("موقعها من الكلام: ${word.partsOfSpeech.joinToString("، ")}")
    if (word.synonyms.isNotEmpty()) appendLine("المرادفات: ${word.synonyms.joinToString("، ")}")
    if (word.antonyms.isNotEmpty()) appendLine("المضادات: ${word.antonyms.joinToString("، ")}")
    if (word.prepositions.isNotEmpty()) appendLine("حروف الجر: ${word.prepositions.joinToString("، ")}")
    if (word.collocations.isNotEmpty()) appendLine("المتلازمات: ${word.collocations.joinToString("، ")}")
    if (word.definition.isNotBlank()) appendLine("التعريف: ${word.definition}")
    if (word.derivatives.isNotBlank()) appendLine("الاشتقاقات: ${word.derivatives}")
    if (word.expressions.isNotBlank()) appendLine("التعبيرات اللغوية: ${word.expressions}")
    if (word.examples.isNotEmpty()) appendLine("الأمثلة: ${word.examples.joinToString(" | ")}")
    if (word.linguisticNotes.isNotBlank()) appendLine("الملاحظات اللغوية: ${word.linguisticNotes}")
    if (word.personalHint.isNotBlank()) appendLine("التلميح الذهني: ${word.personalHint}")
    if (word.notes.isNotBlank()) appendLine("ملاحظات: ${word.notes}")
    if (word.importantNotes.isNotBlank()) appendLine("م&ملاحظات: ${word.importantNotes}")
}.trim()
