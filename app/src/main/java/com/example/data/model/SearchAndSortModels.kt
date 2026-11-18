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
}

enum class SearchField(val labelAr: String) {
    ALL("الكل"),
    WORD("الكلمة"),
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
