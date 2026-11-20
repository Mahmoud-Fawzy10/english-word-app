package com.example.data.model

object CurriculumDefaults {
    val DEFAULT_BOOKS = listOf(
        "المعاصر",
        "A1",
        "كتاب الوزارة"
    )

    val DEFAULT_PARTS_OF_SPEECH = listOf(
        "اسم (Noun)",
        "فعل (Verb)",
        "صفة (Adj)",
        "ظرف / حال (Adv)",
        "حرف جر (Prep)",
        "ضمير (Pron)",
        "أداة ربط (Conj)",
        "تعبير / مصطلح (Idiom)"
    )

    val DEFAULT_STUDY_TAGS = listOf(
        "مراجعة ليلة الامتحان",
        "تريكات هامة",
        "كلمات التراكمي",
        "ترجمة ومقال"
    )

    val IPA_QUICK_SYMBOLS = listOf(
        "/", "ˈ", "ˌ", "ː", "ə", "ɜː", "ɪ", "iː", "ʊ", "uː",
        "æ", "ɑː", "ɒ", "ɔː", "ʌ", "e", "ʃ", "ʒ", "tʃ", "dʒ", "θ", "ð", "ŋ"
    )
}

enum class SearchField(val labelAr: String) {
    ALL("الكل"),
    WORD("الكلمة"),
    IPA("النطق IPA"),
    ARABIC_MEANING("المعنى بالعربي"),
    BOOK("الكتاب"),
    LOCATION("الوحدة / الدرس / الصفحة"),
    PART_OF_SPEECH("موقعها من الكلام"),
    SYNONYMS("المرادفات"),
    ANTONYMS("المضادات"),
    PREPOSITIONS("حروف الجر"),
    COLLOCATIONS("المتلازمات اللفظية"),
    DEFINITION("التعريف"),
    DERIVATIVES("الاشتقاقات"),
    EXPRESSIONS("التعبيرات اللغوية"),
    LINGUISTIC_NOTES("الملاحظات اللغوية"),
    EXAMPLES("الأمثلة"),
    NOTES("الملاحظات"),
    STUDY_TAGS("المجموعات")
}

enum class BrowseGroupMode(val labelAr: String) {
    FLAT_LIST("قائمة"),
    BY_PART_OF_SPEECH("أقسام الكلام"),
    BY_CURRICULUM("الوحدة والدرس"),
    BY_STUDY_TAG("المجموعات"),
    BY_DIFFICULTY("الصعوبة")
}

enum class DuplicatePolicy(val labelAr: String, val descriptionAr: String) {
    SKIP("تجاهل المكررة وأخذ الباقي", "يتجاهل أي كلمة مكررة (في القاموس أو داخل الملف) ويستورد باقي الكلمات الجديدة فقط"),
    MERGE("دمج وتحديث المكررة", "يدمج المواقع والمرادفات والمعاني الجديدة في الكلمة الموجودة بدون تكرار ويستورد الباقي")
}

enum class WordDifficulty(val level: Int, val labelAr: String) {
    NORMAL(0, "عادي"),
    EASY(1, "سهل"),
    MEDIUM(2, "متوسط"),
    HARD(3, "صعب")
}

data class AdvancedSearchFilter(
    val selectedBooks: Set<String> = emptySet(),
    val unitQuery: String = "",
    val lessonQuery: String = "",
    val pageQuery: String = "",
    val selectedPartsOfSpeech: Set<String> = emptySet(),
    val wordQuery: String = "",
    val meaningQuery: String = "",
    val synonymAntonymQuery: String = "",
    val prepositionsQuery: String = "",
    val collocationsQuery: String = "",
    val linguisticNotesQuery: String = "",
    val selectedStudyTags: Set<String> = emptySet(),
    val selectedDifficulties: Set<Int> = emptySet(),
    val favoritesOnly: Boolean = false,
    val hasNotesOnly: Boolean = false,
    val hasExpressionsOnly: Boolean = false,
    val hardOrMistakesOnly: Boolean = false,
    val incompleteOnly: Boolean = false
) {
    val activeCount: Int
        get() = listOf(
            selectedBooks.isNotEmpty(),
            unitQuery.isNotBlank(),
            lessonQuery.isNotBlank(),
            pageQuery.isNotBlank(),
            selectedPartsOfSpeech.isNotEmpty(),
            wordQuery.isNotBlank(),
            meaningQuery.isNotBlank(),
            synonymAntonymQuery.isNotBlank(),
            prepositionsQuery.isNotBlank(),
            collocationsQuery.isNotBlank(),
            linguisticNotesQuery.isNotBlank(),
            selectedStudyTags.isNotEmpty(),
            selectedDifficulties.isNotEmpty(),
            favoritesOnly,
            hasNotesOnly,
            hasExpressionsOnly,
            hardOrMistakesOnly,
            incompleteOnly
        ).count { it }

    val isActive: Boolean get() = activeCount > 0
}

enum class SortOption(val labelAr: String) {
    NEWEST("الأحدث إضافةً"),
    OLDEST("الأقدم إضافةً"),
    ALPHA_ASC("أبجدياً (A → Z)"),
    ALPHA_DESC("أبجدياً عكسياً (Z → A)"),
    UNIT_ASC("حسب رقم الوحدة"),
    PAGE_ASC("حسب رقم الصفحة"),
    BOOK("حسب الكتاب"),
    FAVORITES_FIRST("المفضلة أولاً"),
    DIFFICULTY_DESC("الأكثر صعوبة أولاً"),
    MOST_REVIEWED("الأكثر مراجعة")
}

data class WordFormConfig(
    val showIpa: Boolean = true,
    val showPartOfSpeech: Boolean = true,
    val showBooksAndLocations: Boolean = true,
    val showSynonymsAntonyms: Boolean = true,
    val showPrepositionsCollocations: Boolean = true,
    val showDefinitionDerivativesExpressions: Boolean = true,
    val showExamples: Boolean = true,
    val showNotesAndLinguistic: Boolean = true,
    val showDifficultyAndTags: Boolean = true,
    val showPersonalHint: Boolean = true
)
