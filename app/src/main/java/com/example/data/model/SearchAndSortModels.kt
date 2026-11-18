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
    DEFINITION("التعريف"),
    DERIVATIVES("الاشتقاقات"),
    EXPRESSIONS("التعبيرات اللغوية"),
    NOTES("الملاحظات")
}

enum class BrowseGroupMode(val labelAr: String) {
    FLAT_LIST("قائمة"),
    BY_PART_OF_SPEECH("أقسام الكلام"),
    BY_CURRICULUM("الوحدة والدرس")
}

enum class DuplicatePolicy(val labelAr: String, val descriptionAr: String) {
    SKIP("تخطي الكلمات المكررة", "الإبقاء على الكلمات الحالية كما هي"),
    MERGE("دمج وتحديث المكررة", "دمج المواقع والمرادفات والمعاني الجديدة في الكلمة الموجودة"),
    KEEP_BOTH("استيراد الكل (حتى المكرر)", "إضافة جميع الكلمات كإدخالات مستقلة")
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
    val favoritesOnly: Boolean = false,
    val hasNotesOnly: Boolean = false,
    val hasExpressionsOnly: Boolean = false
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
            favoritesOnly,
            hasNotesOnly,
            hasExpressionsOnly
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
    FAVORITES_FIRST("المفضلة أولاً")
}
