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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MergeType
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CurriculumDefaults
import com.example.data.model.WordDifficulty
import com.example.data.model.WordEntry
import com.example.data.model.WordFormConfig
import com.example.data.model.WordLocation
import com.example.ui.components.LocationEditorCard
import com.example.ui.components.MultiSelectPillGroup
import com.example.ui.components.RoundedBlueTextField
import com.example.ui.components.RoundedSectionCard
import com.example.ui.components.TagChipInput
import com.example.ui.theme.LocalQamusiColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditWordScreen(
    existingWord: WordEntry?,
    prefillQuery: String,
    availableBooks: List<String>,
    availablePartsOfSpeech: List<String>,
    availableStudyTags: List<String>,
    formConfig: WordFormConfig,
    onUpdateFormConfig: (WordFormConfig) -> Unit,
    onCheckDuplicates: suspend (word: String, arabicMeaning: String, excludeId: Long) -> List<WordEntry>,
    onSpeakWord: (String) -> Unit,
    onOpenExistingWord: (Long) -> Unit,
    onSave: (entry: WordEntry, mergeTarget: WordEntry?) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val palette = LocalQamusiColors.current
    val isEditing = existingWord != null
    val prefillIsArabic = remember(prefillQuery) {
        prefillQuery.any { it in '\u0600'..'\u06FF' }
    }

    var showCustomizeSheet by remember { mutableStateOf(false) }

    var word by remember(existingWord?.id) {
        mutableStateOf(existingWord?.word ?: if (!prefillIsArabic) prefillQuery else "")
    }
    var ipa by remember(existingWord?.id) { mutableStateOf(existingWord?.ipa ?: "") }
    var showIpaKeyboard by remember { mutableStateOf(false) }

    var arabicMeaning by remember(existingWord?.id) {
        mutableStateOf(existingWord?.arabicMeaning ?: if (prefillIsArabic) prefillQuery else "")
    }
    var selectedBooks by remember(existingWord?.id) {
        mutableStateOf(existingWord?.books?.toSet() ?: emptySet())
    }
    var bookPool by remember(availableBooks) {
        mutableStateOf((CurriculumDefaults.DEFAULT_BOOKS + availableBooks + selectedBooks).distinct())
    }
    val locations = remember(existingWord?.id) {
        mutableStateListOf<WordLocation>().apply {
            if (existingWord != null && existingWord.locations.isNotEmpty()) {
                addAll(existingWord.locations)
            } else {
                add(WordLocation())
            }
        }
    }
    var selectedPos by remember(existingWord?.id) {
        mutableStateOf(existingWord?.partsOfSpeech?.toSet() ?: emptySet())
    }
    var posPool by remember(availablePartsOfSpeech) {
        mutableStateOf((CurriculumDefaults.DEFAULT_PARTS_OF_SPEECH + availablePartsOfSpeech + selectedPos).distinct())
    }

    val synonyms = remember(existingWord?.id) {
        mutableStateListOf<String>().apply { existingWord?.synonyms?.let { addAll(it) } }
    }
    var pendingSynonym by remember { mutableStateOf("") }

    val antonyms = remember(existingWord?.id) {
        mutableStateListOf<String>().apply { existingWord?.antonyms?.let { addAll(it) } }
    }
    var pendingAntonym by remember { mutableStateOf("") }

    val prepositions = remember(existingWord?.id) {
        mutableStateListOf<String>().apply { existingWord?.prepositions?.let { addAll(it) } }
    }
    var pendingPreposition by remember { mutableStateOf("") }

    val collocations = remember(existingWord?.id) {
        mutableStateListOf<String>().apply { existingWord?.collocations?.let { addAll(it) } }
    }
    var pendingCollocation by remember { mutableStateOf("") }

    val examples = remember(existingWord?.id) {
        mutableStateListOf<String>().apply { existingWord?.examples?.let { addAll(it) } }
    }
    var pendingExample by remember { mutableStateOf("") }

    var definition by remember(existingWord?.id) { mutableStateOf(existingWord?.definition ?: "") }
    var derivatives by remember(existingWord?.id) { mutableStateOf(existingWord?.derivatives ?: "") }
    var expressions by remember(existingWord?.id) { mutableStateOf(existingWord?.expressions ?: "") }
    var linguisticNotes by remember(existingWord?.id) { mutableStateOf(existingWord?.linguisticNotes ?: "") }
    var personalHint by remember(existingWord?.id) { mutableStateOf(existingWord?.personalHint ?: "") }
    var notes by remember(existingWord?.id) { mutableStateOf(existingWord?.notes ?: "") }
    var importantNotes by remember(existingWord?.id) { mutableStateOf(existingWord?.importantNotes ?: "") }
    var difficulty by remember(existingWord?.id) { mutableIntStateOf(existingWord?.difficulty ?: 0) }
    var selectedStudyTags by remember(existingWord?.id) {
        mutableStateOf(existingWord?.studyTags?.toSet() ?: emptySet())
    }
    var tagPool by remember(availableStudyTags) {
        mutableStateOf((CurriculumDefaults.DEFAULT_STUDY_TAGS + availableStudyTags + selectedStudyTags).distinct())
    }
    var isFavorite by remember(existingWord?.id) { mutableStateOf(existingWord?.isFavorite ?: false) }

    var duplicateMatches by remember { mutableStateOf<List<WordEntry>>(emptyList()) }

    LaunchedEffect(existingWord) {
        if (existingWord != null) {
            word = existingWord.word
            ipa = existingWord.ipa
            arabicMeaning = existingWord.arabicMeaning
            selectedBooks = existingWord.books.toSet()
            locations.clear()
            if (existingWord.locations.isNotEmpty()) locations.addAll(existingWord.locations) else locations.add(WordLocation())
            selectedPos = existingWord.partsOfSpeech.toSet()
            synonyms.clear(); synonyms.addAll(existingWord.synonyms)
            antonyms.clear(); antonyms.addAll(existingWord.antonyms)
            prepositions.clear(); prepositions.addAll(existingWord.prepositions)
            collocations.clear(); collocations.addAll(existingWord.collocations)
            examples.clear(); examples.addAll(existingWord.examples)
            definition = existingWord.definition
            derivatives = existingWord.derivatives
            expressions = existingWord.expressions
            linguisticNotes = existingWord.linguisticNotes
            personalHint = existingWord.personalHint
            notes = existingWord.notes
            importantNotes = existingWord.importantNotes
            difficulty = existingWord.difficulty
            selectedStudyTags = existingWord.studyTags.toSet()
            isFavorite = existingWord.isFavorite
        }
    }

    LaunchedEffect(word, arabicMeaning, existingWord?.id) {
        if (word.isBlank() && arabicMeaning.isBlank()) {
            duplicateMatches = emptyList()
            return@LaunchedEffect
        }
        delay(180)
        duplicateMatches = onCheckDuplicates(word, arabicMeaning, existingWord?.id ?: 0L)
    }

    fun flushPendingChips(pending: String, list: MutableList<String>, splitCommas: Boolean = true): String {
        if (pending.isNotBlank()) {
            val parts = if (splitCommas) pending.split(',', '،', '\n') else listOf(pending)
            parts.map { it.trim() }.filter { it.isNotBlank() }
                .forEach { if (!list.contains(it)) list.add(it) }
        }
        return ""
    }

    fun commitPendingTagsAndSave(mergeTarget: WordEntry? = null) {
        pendingSynonym = flushPendingChips(pendingSynonym, synonyms)
        pendingAntonym = flushPendingChips(pendingAntonym, antonyms)
        pendingPreposition = flushPendingChips(pendingPreposition, prepositions)
        pendingCollocation = flushPendingChips(pendingCollocation, collocations)
        pendingExample = flushPendingChips(pendingExample, examples, splitCommas = false)

        val entry = WordEntry(
            id = existingWord?.id ?: 0L,
            word = word,
            ipa = ipa,
            arabicMeaning = arabicMeaning,
            books = selectedBooks.toList(),
            locations = locations.toList(),
            partsOfSpeech = selectedPos.toList(),
            synonyms = synonyms.toList(),
            antonyms = antonyms.toList(),
            prepositions = prepositions.toList(),
            collocations = collocations.toList(),
            definition = definition,
            derivatives = derivatives,
            expressions = expressions,
            linguisticNotes = linguisticNotes,
            examples = examples.toList(),
            personalHint = personalHint,
            notes = notes,
            importantNotes = importantNotes,
            difficulty = difficulty,
            studyTags = selectedStudyTags.toList(),
            isFavorite = isFavorite,
            createdAt = existingWord?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            srsRepetitions = existingWord?.srsRepetitions ?: 0,
            srsIntervalDays = existingWord?.srsIntervalDays ?: 0,
            srsEaseFactor = existingWord?.srsEaseFactor ?: 2.5f,
            srsNextReviewAt = existingWord?.srsNextReviewAt ?: 0L,
            srsLastReviewedAt = existingWord?.srsLastReviewedAt ?: 0L,
            srsTotalReviews = existingWord?.srsTotalReviews ?: 0,
            srsCorrectCount = existingWord?.srsCorrectCount ?: 0,
            lastWrongAt = existingWord?.lastWrongAt ?: 0L
        )
        onSave(entry, mergeTarget)
    }

    val isDuplicateBlocked = duplicateMatches.isNotEmpty()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "تعديل الكلمة" else "إضافة كلمة جديدة",
                        fontWeight = FontWeight.Bold
                    )
                },
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
                    if (word.isNotBlank()) {
                        IconButton(
                            onClick = { onSpeakWord(word) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("speak_word_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "نطق الكلمة محلياً",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(
                        onClick = { showCustomizeSheet = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("customize_form_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "تخصيص حقول الإدخال",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { isFavorite = !isFavorite },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "مفضلة",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { commitPendingTagsAndSave(null) },
                        enabled = !isDuplicateBlocked,
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("save_word_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
                            disabledContentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            imageVector = if (isDuplicateBlocked) Icons.Rounded.Block else Icons.Rounded.Check,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isDuplicateBlocked -> "التكرار مرفوض — الكلمة موجودة مسبقاً"
                                isEditing -> "حفظ التعديلات"
                                else -> "حفظ الكلمة في قاموسي"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
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
            Column(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(PaddingValues(horizontal = 16.dp, vertical = 10.dp)),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Friendly optional-fields banner + quick form customization link
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { showCustomizeSheet = true },
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "جميع الحقول اختيارية تماماً — اضغط هنا لتخصيص الحقول الظاهرة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Live Strict Anti-Duplication Rejection Banner
                AnimatedVisibility(visible = duplicateMatches.isNotEmpty()) {
                    val firstMatch = duplicateMatches.firstOrNull()
                    if (firstMatch != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("duplicate_warning_banner"),
                            shape = RoundedCornerShape(24.dp),
                            color = palette.dangerBg,
                            border = BorderStroke(1.5.dp, palette.dangerBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WarningAmber,
                                        contentDescription = null,
                                        tint = palette.dangerText
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "مرفوض التكرار: هذه الكلمة مسجلة مسبقاً!",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = palette.dangerText,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = buildString {
                                                append(firstMatch.displayTitle())
                                                if (firstMatch.arabicMeaning.isNotBlank() && firstMatch.word.isNotBlank()) {
                                                    append(" • ")
                                                    append(firstMatch.arabicMeaning)
                                                }
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = palette.dangerText
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onOpenExistingWord(firstMatch.id) },
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, palette.dangerBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.dangerText),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("open_existing_duplicate_button")
                                    ) {
                                        Text("عرض الموجودة")
                                    }
                                    if (!isEditing) {
                                        Button(
                                            onClick = { commitPendingTagsAndSave(firstMatch) },
                                            shape = RoundedCornerShape(50),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("merge_duplicate_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MergeType,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("دمج معها")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 1. Word, IPA & Arabic Meaning
                RoundedSectionCard(
                    title = "الكلمة والنطق والمعنى",
                    icon = Icons.Rounded.Translate
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoundedBlueTextField(
                            value = word,
                            onValueChange = { word = it },
                            label = "الكلمة (Word / Phrase)",
                            placeholder = "مثال: sustainable",
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("word_input")
                        )
                        if (word.isNotBlank()) {
                            Surface(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onSpeakWord(word) }
                                    .testTag("speak_field_button"),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                        contentDescription = "استماع للنطق",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    if (formConfig.showIpa) {
                        RoundedBlueTextField(
                            value = ipa,
                            onValueChange = { ipa = it },
                            label = "الرمز الصوتي للنطق (IPA)",
                            placeholder = "مثال: /səˈsteɪnəbl/",
                            singleLine = true,
                            leadingIcon = Icons.Rounded.RecordVoiceOver,
                            modifier = Modifier.testTag("ipa_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showIpaKeyboard) "إخفاء رموز IPA السريعة" else "إظهار رموز IPA السريعة",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { showIpaKeyboard = !showIpaKeyboard }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("toggle_ipa_symbols_button")
                            )
                            if (ipa.isNotBlank()) {
                                Text(
                                    text = "مسح IPA",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { ipa = "" }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        AnimatedVisibility(visible = showIpaKeyboard) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(CurriculumDefaults.IPA_QUICK_SYMBOLS) { sym ->
                                    Surface(
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { ipa += sym },
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = sym,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    RoundedBlueTextField(
                        value = arabicMeaning,
                        onValueChange = { arabicMeaning = it },
                        label = "معناها بالعربي",
                        placeholder = "مثال: مستدام / صديق للبيئة",
                        minLines = 1,
                        maxLines = 3,
                        modifier = Modifier.testTag("arabic_meaning_input")
                    )
                }

                // 2. Part of Speech
                if (formConfig.showPartOfSpeech) {
                    RoundedSectionCard(
                        title = "موقعها من الكلام",
                        subtitle = "يمكنك اختيار أكثر من نوع",
                        icon = Icons.Rounded.Category
                    ) {
                        MultiSelectPillGroup(
                            options = posPool,
                            selectedOptions = selectedPos,
                            onToggle = { pos ->
                                selectedPos = if (selectedPos.contains(pos)) selectedPos - pos else selectedPos + pos
                            },
                            allowCustomAdd = true,
                            customDialogTitle = "إضافة موقع من الكلام",
                            customDialogHint = "مثال: Phrasal Verb",
                            onAddCustom = { newPos ->
                                if (!posPool.contains(newPos)) posPool = posPool + newPos
                                selectedPos = selectedPos + newPos
                            }
                        )
                    }
                }

                // 3 & 4. Books & Multiple Locations
                if (formConfig.showBooksAndLocations) {
                    RoundedSectionCard(
                        title = "الكتاب المذكورة فيه",
                        subtitle = "يمكنك اختيار أكثر من كتاب",
                        icon = Icons.Rounded.MenuBook
                    ) {
                        MultiSelectPillGroup(
                            options = bookPool,
                            selectedOptions = selectedBooks,
                            onToggle = { book ->
                                selectedBooks = if (selectedBooks.contains(book)) selectedBooks - book else selectedBooks + book
                            },
                            allowCustomAdd = true,
                            customDialogTitle = "إضافة كتاب جديد",
                            customDialogHint = "اسم الكتاب أو المذكرة",
                            onAddCustom = { newBook ->
                                if (!bookPool.contains(newBook)) bookPool = bookPool + newBook
                                selectedBooks = selectedBooks + newBook
                            }
                        )
                    }

                    RoundedSectionCard(
                        title = "مواقع الكلمة في المنهج",
                        subtitle = "أضف عدة مواقع (وحدة، درس، صفحة)",
                        icon = Icons.Rounded.LocationOn
                    ) {
                        locations.forEachIndexed { index, loc ->
                            LocationEditorCard(
                                index = index,
                                location = loc,
                                bookOptions = bookPool,
                                onUpdate = { updated -> locations[index] = updated },
                                onRemove = { locations.removeAt(index) },
                                canRemove = locations.size > 1
                            )
                        }

                        OutlinedButton(
                            onClick = { locations.add(WordLocation()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("add_location_button"),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddLocationAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة موقع / وحدة / درس / صفحة أخرى")
                        }
                    }
                }

                // 5. Multiple Synonyms & Multiple Antonyms
                if (formConfig.showSynonymsAntonyms) {
                    RoundedSectionCard(
                        title = "المرادفات والمضادات (متعددة)",
                        subtitle = "أضف أي عدد من المرادفات والمضادات واضغط + أو فاصلة",
                        icon = Icons.Rounded.CompareArrows
                    ) {
                        TagChipInput(
                            label = "مرادفاتها (Synonyms)",
                            placeholder = "مثال: renewable, continuous",
                            tags = synonyms,
                            pendingInput = pendingSynonym,
                            onPendingInputChange = { pendingSynonym = it },
                            onAddTag = { tag -> if (!synonyms.contains(tag)) synonyms.add(tag) },
                            onRemoveTag = { tag -> synonyms.remove(tag) },
                            chipColor = palette.synonymBg,
                            chipTextColor = palette.synonymText,
                            testTagPrefix = "synonyms"
                        )

                        TagChipInput(
                            label = "مضاداتها (Antonyms)",
                            placeholder = "مثال: temporary, unsustainable",
                            tags = antonyms,
                            pendingInput = pendingAntonym,
                            onPendingInputChange = { pendingAntonym = it },
                            onAddTag = { tag -> if (!antonyms.contains(tag)) antonyms.add(tag) },
                            onRemoveTag = { tag -> antonyms.remove(tag) },
                            chipColor = palette.antonymBg,
                            chipTextColor = palette.antonymText,
                            testTagPrefix = "antonyms"
                        )
                    }
                }

                // 6. Prepositions & Collocations
                if (formConfig.showPrepositionsCollocations) {
                    RoundedSectionCard(
                        title = "حروف الجر والمتلازمات اللفظية",
                        subtitle = "تُصدَّر كلٌّ منها في ورقة مستقلة في Excel و Google Sheets",
                        icon = Icons.Rounded.Link
                    ) {
                        TagChipInput(
                            label = "حروف الجر (Prepositions)",
                            placeholder = "مثال: insist on, interested in",
                            tags = prepositions,
                            pendingInput = pendingPreposition,
                            onPendingInputChange = { pendingPreposition = it },
                            onAddTag = { tag -> if (!prepositions.contains(tag)) prepositions.add(tag) },
                            onRemoveTag = { tag -> prepositions.remove(tag) },
                            chipColor = MaterialTheme.colorScheme.secondaryContainer,
                            chipTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            testTagPrefix = "prepositions"
                        )

                        TagChipInput(
                            label = "المتلازمات اللفظية (Collocations)",
                            placeholder = "مثال: make a decision, take responsibility",
                            tags = collocations,
                            pendingInput = pendingCollocation,
                            onPendingInputChange = { pendingCollocation = it },
                            onAddTag = { tag -> if (!collocations.contains(tag)) collocations.add(tag) },
                            onRemoveTag = { tag -> collocations.remove(tag) },
                            chipColor = MaterialTheme.colorScheme.primaryContainer,
                            chipTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            testTagPrefix = "collocations"
                        )
                    }
                }

                // 7. Definition, Derivatives & Expressions
                if (formConfig.showDefinitionDerivativesExpressions) {
                    RoundedSectionCard(
                        title = "التعريف والاشتقاقات والتعبيرات",
                        icon = Icons.Rounded.AutoStories
                    ) {
                        RoundedBlueTextField(
                            value = definition,
                            onValueChange = { definition = it },
                            label = "تعريفها (Definition)",
                            placeholder = "التعريف بالإنجليزية أو شرح مبسط",
                            minLines = 2,
                            maxLines = 5,
                            modifier = Modifier.testTag("definition_input")
                        )
                        RoundedBlueTextField(
                            value = derivatives,
                            onValueChange = { derivatives = it },
                            label = "اشتقاقاتها (Derivatives)",
                            placeholder = "مثال: sustain (v) - sustainability (n) - sustainably (adv)",
                            minLines = 2,
                            maxLines = 5,
                            modifier = Modifier.testTag("derivatives_input")
                        )
                        RoundedBlueTextField(
                            value = expressions,
                            onValueChange = { expressions = it },
                            label = "تعبيراتها اللغوية (Expressions)",
                            placeholder = "مثال: sustainable development / in the long run",
                            minLines = 2,
                            maxLines = 6,
                            modifier = Modifier.testTag("expressions_input")
                        )
                    }
                }

                // 8. Example Sentences
                if (formConfig.showExamples) {
                    RoundedSectionCard(
                        title = "أمثلة سياقية (Examples)",
                        subtitle = "أضف جملة أو أكثر للاستخدام العملي والنطق",
                        icon = Icons.Rounded.FormatQuote
                    ) {
                        TagChipInput(
                            label = "جملة مثال جديدة",
                            placeholder = "مثال: Egypt invests in sustainable energy.",
                            tags = examples,
                            pendingInput = pendingExample,
                            onPendingInputChange = { pendingExample = it },
                            onAddTag = { ex -> if (!examples.contains(ex)) examples.add(ex) },
                            onRemoveTag = { ex -> examples.remove(ex) },
                            testTagPrefix = "examples"
                        )
                    }
                }

                // 9. Linguistic Notes, Personal Hint & General / Important Notes
                if (formConfig.showNotesAndLinguistic || formConfig.showPersonalHint) {
                    RoundedSectionCard(
                        title = "الملاحظات اللغوية والتلميحات",
                        icon = Icons.Rounded.EditNote
                    ) {
                        if (formConfig.showNotesAndLinguistic) {
                            RoundedBlueTextField(
                                value = linguisticNotes,
                                onValueChange = { linguisticNotes = it },
                                label = "الملاحظات اللغوية والفروق (Language Notes)",
                                placeholder = "الفرق بين make / do أو استخدام قواعدي خاص...",
                                minLines = 2,
                                maxLines = 6,
                                leadingIcon = Icons.Rounded.AutoStories,
                                modifier = Modifier.testTag("linguistic_notes_input")
                            )
                            RoundedBlueTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = "ملاحظات عامة عليها",
                                placeholder = "أي شرح إضافي يساعدك...",
                                minLines = 2,
                                maxLines = 6,
                                leadingIcon = Icons.Rounded.Psychology,
                                modifier = Modifier.testTag("notes_input")
                            )
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = palette.importantNoteBg,
                                border = BorderStroke(1.dp, palette.importantNoteBorder)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    RoundedBlueTextField(
                                        value = importantNotes,
                                        onValueChange = { importantNotes = it },
                                        label = "م&ملاحظات هامة (تريكات الامتحان)",
                                        placeholder = "تنبيهات امتحانات وتريكات هامة...",
                                        minLines = 2,
                                        maxLines = 6,
                                        leadingIcon = Icons.Rounded.Lightbulb,
                                        modifier = Modifier.testTag("important_notes_input")
                                    )
                                }
                            }
                        }

                        if (formConfig.showPersonalHint) {
                            RoundedBlueTextField(
                                value = personalHint,
                                onValueChange = { personalHint = it },
                                label = "تلميح ذهني / رابط تذكر (Mnemonic)",
                                placeholder = "مثال: اربطها بكلمة sustain...",
                                singleLine = true,
                                leadingIcon = Icons.Rounded.Lightbulb,
                                modifier = Modifier.testTag("personal_hint_input")
                            )
                        }
                    }
                }

                // 10. Difficulty & Study Groups
                if (formConfig.showDifficultyAndTags) {
                    RoundedSectionCard(
                        title = "الصعوبة والمجموعات الدراسية",
                        icon = Icons.Rounded.Label
                    ) {
                        Text("مستوى الصعوبة:", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WordDifficulty.entries.forEach { diff ->
                                val selected = difficulty == diff.level
                                FilterChip(
                                    selected = selected,
                                    onClick = { difficulty = diff.level },
                                    label = { Text(diff.labelAr) },
                                    shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("difficulty_${diff.level}")
                                )
                            }
                        }

                        Text("المجموعات الدراسية (Study Groups):", style = MaterialTheme.typography.labelLarge)
                        MultiSelectPillGroup(
                            options = tagPool,
                            selectedOptions = selectedStudyTags,
                            onToggle = { tag ->
                                selectedStudyTags = if (selectedStudyTags.contains(tag)) {
                                    selectedStudyTags - tag
                                } else {
                                    selectedStudyTags + tag
                                }
                            },
                            allowCustomAdd = true,
                            customDialogTitle = "إضافة مجموعة دراسية جديدة",
                            customDialogHint = "مثال: مراجعة الشهر",
                            onAddCustom = { newTag ->
                                if (!tagPool.contains(newTag)) tagPool = tagPool + newTag
                                selectedStudyTags = selectedStudyTags + newTag
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showCustomizeSheet) {
        FormCustomizationSheet(
            config = formConfig,
            onUpdateConfig = onUpdateFormConfig,
            onDismiss = { showCustomizeSheet = false }
        )
    }
}
