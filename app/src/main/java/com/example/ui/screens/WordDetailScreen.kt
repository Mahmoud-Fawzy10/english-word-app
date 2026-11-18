package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.WordEntry
import com.example.ui.components.BookPillBadge
import com.example.ui.components.ManualSrsScheduleDialog
import com.example.ui.components.RoundedSectionCard
import com.example.ui.components.SrsDetailCard
import com.example.ui.theme.AntonymChipBg
import com.example.ui.theme.AntonymChipText
import com.example.ui.theme.CobaltDark
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.ImportantNoteBg
import com.example.ui.theme.ImportantNoteBorder
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.SynonymChipBg
import com.example.ui.theme.SynonymChipText

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WordDetailScreen(
    word: WordEntry?,
    onBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteConfirm: (Long) -> Unit,
    onToggleFavorite: (WordEntry) -> Unit,
    onReviewNowClick: (WordEntry) -> Unit,
    onManualSrsUpdate: (WordEntry, Int, Float, Boolean) -> Unit,
    onCopiedMessage: (String) -> Unit
) {
    BackHandler { onBack() }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showManualSrsDialog by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل الكلمة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    if (word != null) {
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(formatWordForCopy(word)))
                                onCopiedMessage("تم نسخ بيانات الكلمة")
                            },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "نسخ الكلمة"
                            )
                        }
                        IconButton(
                            onClick = { onToggleFavorite(word) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (word.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "مفضلة",
                                tint = if (word.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (word != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 10.dp,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("delete_word_button"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حذف")
                        }
                        Button(
                            onClick = { onEditClick(word.id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("edit_word_button"),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تعديل الكلمة", style = MaterialTheme.typography.titleMedium)
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
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("لم يتم العثور على هذه الكلمة", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = onBack, shape = RoundedCornerShape(50)) {
                        Text("العودة")
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .widthIn(max = 700.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Main Hero Word Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(30.dp),
                        shadowElevation = 4.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(CobaltDark, CobaltPrimary, SkyAccent)
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = word.displayTitle(),
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White
                                )
                                if (word.arabicMeaning.isNotBlank() && word.word.isNotBlank()) {
                                    Text(
                                        text = word.arabicMeaning,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color.White.copy(alpha = 0.94f)
                                    )
                                }
                                if (word.partsOfSpeech.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        word.partsOfSpeech.forEach { pos ->
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color.White.copy(alpha = 0.22f)
                                            ) {
                                                Text(
                                                    text = pos,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Books & Locations
                    val validLocations = word.locations.filter { it.isNotBlank() }
                    if (word.books.isNotEmpty() || validLocations.isNotEmpty()) {
                        RoundedSectionCard(
                            title = "الكتب والمواقع بالمنهج",
                            icon = Icons.Rounded.MenuBook
                        ) {
                            if (word.books.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    word.books.forEach { BookPillBadge(it) }
                                }
                            }
                            if (validLocations.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    validLocations.forEach { loc ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(18.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.LocationOn,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = loc.formattedLabel(),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Synonyms & Antonyms
                    if (word.synonyms.isNotEmpty() || word.antonyms.isNotEmpty()) {
                        RoundedSectionCard(
                            title = "المرادفات والمضادات",
                            icon = Icons.Rounded.CompareArrows
                        ) {
                            if (word.synonyms.isNotEmpty()) {
                                Text("المرادفات (Synonyms)", style = MaterialTheme.typography.labelLarge)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    word.synonyms.forEach { syn ->
                                        Surface(shape = RoundedCornerShape(50), color = SynonymChipBg) {
                                            Text(
                                                text = syn,
                                                color = SynonymChipText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (word.antonyms.isNotEmpty()) {
                                Text("المضادات (Antonyms)", style = MaterialTheme.typography.labelLarge)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    word.antonyms.forEach { ant ->
                                        Surface(shape = RoundedCornerShape(50), color = AntonymChipBg) {
                                            Text(
                                                text = ant,
                                                color = AntonymChipText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Definition, Derivatives & Expressions
                    if (word.definition.isNotBlank() || word.derivatives.isNotBlank() || word.expressions.isNotBlank()) {
                        RoundedSectionCard(
                            title = "الشرح واللغويات",
                            icon = Icons.Rounded.AutoStories
                        ) {
                            if (word.definition.isNotBlank()) {
                                DetailFieldBlock(label = "التعريف", content = word.definition)
                            }
                            if (word.derivatives.isNotBlank()) {
                                DetailFieldBlock(label = "الاشتقاقات", content = word.derivatives)
                            }
                            if (word.expressions.isNotBlank()) {
                                DetailFieldBlock(label = "التعبيرات اللغوية", content = word.expressions)
                            }
                        }
                    }

                    // Notes & Important M&Notes
                    if (word.notes.isNotBlank() || word.importantNotes.isNotBlank()) {
                        RoundedSectionCard(
                            title = "الملاحظات",
                            icon = Icons.Rounded.EditNote
                        ) {
                            if (word.notes.isNotBlank()) {
                                DetailFieldBlock(label = "ملاحظات عليها", content = word.notes)
                            }
                            if (word.importantNotes.isNotBlank()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = ImportantNoteBg,
                                    border = BorderStroke(1.dp, ImportantNoteBorder)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Lightbulb,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "م&ملاحظات هامة",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = word.importantNotes,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Spaced Repetition (SRS) Schedule & Manual Adjustment
                    SrsDetailCard(
                        entry = word,
                        onReviewNowClick = { onReviewNowClick(word) },
                        onManualAdjustClick = { showManualSrsDialog = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
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
private fun DetailFieldBlock(label: String, content: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatWordForCopy(word: WordEntry): String = buildString {
    if (word.word.isNotBlank()) appendLine(word.word)
    if (word.arabicMeaning.isNotBlank()) appendLine("المعنى: ${word.arabicMeaning}")
    if (word.books.isNotEmpty()) appendLine("الكتاب: ${word.books.joinToString("، ")}")
    val locs = word.locations.filter { it.isNotBlank() }.joinToString(" | ") { it.formattedLabel() }
    if (locs.isNotBlank()) appendLine("المواقع: $locs")
    if (word.partsOfSpeech.isNotEmpty()) appendLine("موقعها من الكلام: ${word.partsOfSpeech.joinToString("، ")}")
    if (word.synonyms.isNotEmpty()) appendLine("المرادفات: ${word.synonyms.joinToString("، ")}")
    if (word.antonyms.isNotEmpty()) appendLine("المضادات: ${word.antonyms.joinToString("، ")}")
    if (word.definition.isNotBlank()) appendLine("التعريف: ${word.definition}")
    if (word.derivatives.isNotBlank()) appendLine("الاشتقاقات: ${word.derivatives}")
    if (word.expressions.isNotBlank()) appendLine("التعبيرات اللغوية: ${word.expressions}")
    if (word.notes.isNotBlank()) appendLine("ملاحظات: ${word.notes}")
    if (word.importantNotes.isNotBlank()) appendLine("م&ملاحظات: ${word.importantNotes}")
}.trim()
