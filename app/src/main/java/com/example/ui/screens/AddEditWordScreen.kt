package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
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
import com.example.ui.theme.AntonymChipBg
import com.example.ui.theme.AntonymChipText
import com.example.ui.theme.ImportantNoteBg
import com.example.ui.theme.ImportantNoteBorder
import com.example.ui.theme.SynonymChipBg
import com.example.ui.theme.SynonymChipText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditWordScreen(
    existingWord: WordEntry?,
    prefillQuery: String,
    availableBooks: List<String>,
    availablePartsOfSpeech: List<String>,
    onSave: (WordEntry) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val isEditing = existingWord != null
    val prefillIsArabic = remember(prefillQuery) {
        prefillQuery.any { it in '\u0600'..'\u06FF' }
    }

    var word by remember(existingWord?.id) {
        mutableStateOf(
            existingWord?.word ?: if (!prefillIsArabic) prefillQuery else ""
        )
    }
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

    LaunchedEffect(existingWord) {
        if (existingWord != null) {
            word = existingWord.word
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

    fun commitPendingTagsAndSave() {
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
            updatedAt = System.currentTimeMillis()
        )
        onSave(entry)
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
                        onClick = { commitPendingTagsAndSave() },
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
                            text = if (isEditing) "حفظ التعديلات" else "حفظ الكلمة في قاموسي",
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

                // 1. Word & Arabic Meaning
                RoundedSectionCard(
                    title = "الكلمة ومعناها",
                    icon = Icons.Rounded.Translate
                ) {
                    RoundedBlueTextField(
                        value = word,
                        onValueChange = { word = it },
                        label = "الكلمة (Word / Phrase)",
                        placeholder = "مثال: sustainable",
                        singleLine = true,
                        modifier = Modifier.testTag("word_input")
                    )
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
                        chipColor = SynonymChipBg,
                        chipTextColor = SynonymChipText,
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
                        chipColor = AntonymChipBg,
                        chipTextColor = AntonymChipText,
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
                        color = ImportantNoteBg,
                        border = BorderStroke(1.dp, ImportantNoteBorder)
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
