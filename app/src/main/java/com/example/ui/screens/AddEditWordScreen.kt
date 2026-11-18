package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MergeType
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.WarningAmber
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.data.model.WordEntry
import com.example.data.model.WordLocation
import com.example.ui.components.LocationEditorCard
import com.example.ui.components.MultiSelectPillGroup
import com.example.ui.components.RoundedBlueTextField
import com.example.ui.components.RoundedSectionCard
import com.example.ui.components.TagChipInput
import com.example.ui.theme.LocalQamusiColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditWordScreen(
    existingWord: WordEntry?,
    prefillQuery: String,
    availableBooks: List<String>,
    availablePartsOfSpeech: List<String>,
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

    var word by remember(existingWord?.id) {
        mutableStateOf(
            existingWord?.word ?: if (!prefillIsArabic) prefillQuery else ""
        )
    }
    var ipa by remember(existingWord?.id) {
        mutableStateOf(existingWord?.ipa ?: "")
    }
    var showIpaKeyboard by remember { mutableStateOf(false) }

    var arabicMeaning by remember(existingWord?.id) {
        mutableStateOf(
            existingWord?.arabicMeaning ?: if (prefillIsArabic) prefillQuery else ""
        )
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

    var definition by remember(existingWord?.id) { mutableStateOf(existingWord?.definition ?: "") }
    var derivatives by remember(existingWord?.id) { mutableStateOf(existingWord?.derivatives ?: "") }
    var expressions by remember(existingWord?.id) { mutableStateOf(existingWord?.expressions ?: "") }
    var notes by remember(existingWord?.id) { mutableStateOf(existingWord?.notes ?: "") }
    var importantNotes by remember(existingWord?.id) { mutableStateOf(existingWord?.importantNotes ?: "") }
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
            synonyms.clear()
            synonyms.addAll(existingWord.synonyms)
            antonyms.clear()
            antonyms.addAll(existingWord.antonyms)
            definition = existingWord.definition
            derivatives = existingWord.derivatives
            expressions = existingWord.expressions
            notes = existingWord.notes
            importantNotes = existingWord.importantNotes
            isFavorite = existingWord.isFavorite
        }
    }

    LaunchedEffect(word, arabicMeaning, existingWord?.id) {
        if (word.isBlank() && arabicMeaning.isBlank()) {
            duplicateMatches = emptyList()
            return@LaunchedEffect
        }
        delay(220)
        duplicateMatches = onCheckDuplicates(word, arabicMeaning, existingWord?.id ?: 0L)
    }

    fun commitPendingTagsAndSave(mergeTarget: WordEntry? = null) {
        if (pendingSynonym.isNotBlank()) {
            pendingSynonym.split(',', '،', '\n').map { it.trim() }.filter { it.isNotBlank() }
                .forEach { if (!synonyms.contains(it)) synonyms.add(it) }
            pendingSynonym = ""
        }
        if (pendingAntonym.isNotBlank()) {
            pendingAntonym.split(',', '،', '\n').map { it.trim() }.filter { it.isNotBlank() }
                .forEach { if (!antonyms.contains(it)) antonyms.add(it) }
            pendingAntonym = ""
        }

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
            definition = definition,
            derivatives = derivatives,
            expressions = expressions,
            notes = notes,
            importantNotes = importantNotes,
            isFavorite = isFavorite,
            createdAt = existingWord?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            srsRepetitions = existingWord?.srsRepetitions ?: 0,
            srsIntervalDays = existingWord?.srsIntervalDays ?: 0,
            srsEaseFactor = existingWord?.srsEaseFactor ?: 2.5f,
            srsNextReviewAt = existingWord?.srsNextReviewAt ?: 0L,
            srsLastReviewedAt = existingWord?.srsLastReviewedAt ?: 0L,
            srsTotalReviews = existingWord?.srsTotalReviews ?: 0,
            srsCorrectCount = existingWord?.srsCorrectCount ?: 0
        )
        onSave(entry, mergeTarget)
    }

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
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("save_word_button"),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(imageVector = Icons.Rounded.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isEditing -> "حفظ التعديلات"
                                duplicateMatches.isNotEmpty() -> "حفظ ككلمة مستقلة"
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
                // Friendly optional-fields banner
                Surface(
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
                            text = "جميع الحقول اختيارية تماماً — يمكنك ملء الحقول التي تريدها فقط والحفظ فوراً.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Live Duplicate Warning Banner
                AnimatedVisibility(visible = duplicateMatches.isNotEmpty()) {
                    val firstMatch = duplicateMatches.firstOrNull()
                    if (firstMatch != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("duplicate_warning_banner"),
                            shape = RoundedCornerShape(24.dp),
                            color = palette.warningBg,
                            border = BorderStroke(1.5.dp, palette.warningBorder)
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
                                        tint = palette.warningText
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "هذه الكلمة موجودة بالفعل في قاموسك!",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = palette.warningText,
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
                                            color = palette.warningText
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
                                        border = BorderStroke(1.dp, palette.warningBorder),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.warningText),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("عرض الموجودة")
                                    }
                                    if (!isEditing) {
                                        Button(
                                            onClick = { commitPendingTagsAndSave(firstMatch) },
                                            shape = RoundedCornerShape(50),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = palette.warningBorder,
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
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
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

                // 2. Part of Speech (موقعها من الكلام)
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

                // 3. Books (المعاصر، A1، كتاب الوزارة + إمكانية إضافة أكثر من كتاب)
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

                // 4. Multiple Locations (وحدات / دروس / صفحات متعددة)
                RoundedSectionCard(
                    title = "مواقع الكلمة في المنهج",
                    subtitle = "أضف عدة مواقع، ويمكنك كتابة أكثر من درس أو صفحة في كل موقع (مثال: 1, 2)",
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

                // 5. Synonyms & Antonyms
                RoundedSectionCard(
                    title = "المرادفات والمضادات",
                    subtitle = "اكتب الكلمة ثم اضغط + أو ضع فاصلة",
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

                // 6. Definition, Derivatives & Linguistic Expressions
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
                        label = "تعبيراتها اللغوية وحروف الجر (Expressions)",
                        placeholder = "مثال: sustainable development / play a vital role in",
                        minLines = 2,
                        maxLines = 6,
                        modifier = Modifier.testTag("expressions_input")
                    )
                }

                // 7. Notes & Important M&Notes
                RoundedSectionCard(
                    title = "الملاحظات",
                    icon = Icons.Rounded.EditNote
                ) {
                    RoundedBlueTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "ملاحظات عليها",
                        placeholder = "أي شرح إضافي أو مثال يساعدك في تذكر الكلمة...",
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
                                label = "م&ملاحظات عليها (تريكات وملاحظات هامة)",
                                placeholder = "فروق لغوية، تنبيهات امتحانات، تريكات هامة...",
                                minLines = 2,
                                maxLines = 6,
                                leadingIcon = Icons.Rounded.Lightbulb,
                                modifier = Modifier.testTag("important_notes_input")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
