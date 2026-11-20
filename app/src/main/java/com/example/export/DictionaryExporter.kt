package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.WordEntry
import com.example.domain.SrsScheduler
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ExportFormat(
    val titleAr: String,
    val subtitleAr: String,
    val extension: String,
    val mimeType: String
) {
    PDF(
        titleAr = "ملف PDF",
        subtitleAr = "تنسيق أنيق ومنسق للطباعة والمذاكرة أوفلاين",
        extension = "pdf",
        mimeType = "application/pdf"
    ),
    EXCEL(
        titleAr = "ملف Excel / Google Sheets (10 ورقات .xlsx)",
        subtitleAr = "الورقة الأساسية + 9 ورقات متخصصة (المرادفات، المضادات، التعريفات، الاشتقاقات، الملاحظات اللغوية، حروف الجر، المتلازمات، التعبيرات والأمثلة، فهرس المنهج)",
        extension = "xlsx",
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ),
    GOOGLE_SHEETS(
        titleAr = "جدول Google Sheets (.csv)",
        subtitleAr = "ملف CSV موحد متوافق تماماً مع Google Sheets والاستيراد بالعربية أوفلاين",
        extension = "csv",
        mimeType = "text/csv"
    ),
    GOOGLE_SHEETS_BUNDLE(
        titleAr = "حزمة 10 ورقات CSV لـ Google Sheets (.zip)",
        subtitleAr = "10 ملفات CSV منفصلة (الورقة الأساسية + 9 أوراق متخصصة) لاستيرادها كأوراق مستقلة",
        extension = "zip",
        mimeType = "application/zip"
    )
}

private data class SheetSpec(
    val sheetName: String,
    val fileBaseName: String,
    val headers: List<String>,
    val columnWidths: List<Int>,
    val rows: List<List<String>>
)

object DictionaryExporter {

    private val MAIN_HEADERS = listOf(
        "الكلمة",
        "النطق الصوتي (IPA)",
        "المعنى بالعربي",
        "الكتاب",
        "المواقع (وحدة / درس / صفحة)",
        "موقعها من الكلام",
        "المرادفات",
        "المضادات",
        "حروف الجر",
        "المتلازمات اللفظية",
        "التعريف",
        "الاشتقاقات",
        "التعبيرات اللغوية",
        "الملاحظات اللغوية",
        "الأمثلة",
        "التلميح الذهني",
        "ملاحظات عليها",
        "م&ملاحظات هامة",
        "المجموعات",
        "الصعوبة",
        "المفضلة"
    )

    fun suggestedFileName(format: ExportFormat): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return "Qamusi_Words_$stamp.${format.extension}"
    }

    fun exportToStream(
        words: List<WordEntry>,
        format: ExportFormat,
        outputStream: OutputStream
    ) {
        when (format) {
            ExportFormat.PDF -> exportToPdf(words, outputStream)
            ExportFormat.EXCEL -> exportToXlsx(words, outputStream)
            ExportFormat.GOOGLE_SHEETS -> exportToCsv(words, outputStream)
            ExportFormat.GOOGLE_SHEETS_BUNDLE -> exportToCsvZipBundle(words, outputStream)
        }
    }

    fun exportToShareableUri(
        context: Context,
        words: List<WordEntry>,
        format: ExportFormat
    ): Uri {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        exportDir.listFiles()?.forEach { old ->
            if (System.currentTimeMillis() - old.lastModified() > 60 * 60 * 1000L) {
                old.delete()
            }
        }
        val outFile = File(exportDir, suggestedFileName(format))
        FileOutputStream(outFile).use { fos ->
            exportToStream(words, format, fos)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
    }

    fun buildShareIntent(uri: Uri, format: ExportFormat): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "تصدير قاموسي")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "مشاركة أو فتح عبر...")
    }

    private fun wordToMainRow(entry: WordEntry): List<String> {
        val locationsStr = entry.locations
            .filter { it.isNotBlank() }
            .joinToString(" | ") { it.formattedLabel() }

        return listOf(
            entry.word,
            entry.ipa,
            entry.arabicMeaning,
            entry.books.joinToString("، "),
            locationsStr,
            entry.partsOfSpeech.joinToString("، "),
            entry.synonyms.joinToString("، "),
            entry.antonyms.joinToString("، "),
            entry.prepositions.joinToString("، "),
            entry.collocations.joinToString("، "),
            entry.definition,
            entry.derivatives,
            entry.expressions,
            entry.linguisticNotes,
            entry.examples.joinToString(" | "),
            entry.personalHint,
            entry.notes,
            entry.importantNotes,
            entry.studyTags.joinToString("، "),
            entry.difficultyLabelAr(),
            if (entry.isFavorite) "نعم" else "لا"
        )
    }

    /**
     * Builds the 10 Sheets:
     * 0 (Main): قاموسي
     * 1: المرادفات
     * 2: المضادات
     * 3: التعريفات
     * 4: الاشتقاقات
     * 5: الملاحظات اللغوية
     * 6: حروف الجر
     * 7: المتلازمات اللفظية
     * 8: التعبيرات والأمثلة
     * 9: فهرس المنهج
     */
    private fun buildTenSheets(words: List<WordEntry>): List<SheetSpec> {
        val mainSheet = SheetSpec(
            sheetName = "قاموسي",
            fileBaseName = "00_Main_Qamusi",
            headers = MAIN_HEADERS,
            columnWidths = listOf(20, 16, 22, 18, 26, 18, 22, 22, 20, 22, 28, 24, 24, 28, 30, 22, 26, 26, 20, 12, 10),
            rows = words.map(::wordToMainRow)
        )

        val synonymsRows = words.filter { it.synonyms.isNotEmpty() }.map { w ->
            listOf(
                w.displayTitle(),
                w.arabicMeaning,
                w.synonyms.joinToString("، "),
                "${w.synonyms.size}",
                w.books.joinToString("، ")
            )
        }
        val sheet1Synonyms = SheetSpec(
            sheetName = "المرادفات",
            fileBaseName = "01_Synonyms",
            headers = listOf("الكلمة", "المعنى بالعربي", "المرادفات (Synonyms)", "العدد", "الكتاب"),
            columnWidths = listOf(22, 24, 38, 10, 20),
            rows = synonymsRows
        )

        val antonymsRows = words.filter { it.antonyms.isNotEmpty() }.map { w ->
            listOf(
                w.displayTitle(),
                w.arabicMeaning,
                w.antonyms.joinToString("، "),
                "${w.antonyms.size}",
                w.books.joinToString("، ")
            )
        }
        val sheet2Antonyms = SheetSpec(
            sheetName = "المضادات",
            fileBaseName = "02_Antonyms",
            headers = listOf("الكلمة", "المعنى بالعربي", "المضادات (Antonyms)", "العدد", "الكتاب"),
            columnWidths = listOf(22, 24, 38, 10, 20),
            rows = antonymsRows
        )

        val definitionRows = words.filter { it.definition.isNotBlank() }.map { w ->
            listOf(
                w.displayTitle(),
                w.partsOfSpeech.joinToString("، "),
                w.definition,
                w.arabicMeaning,
                w.books.joinToString("، ")
            )
        }
        val sheet3Definitions = SheetSpec(
            sheetName = "التعريفات",
            fileBaseName = "03_Definitions",
            headers = listOf("الكلمة", "موقعها من الكلام", "التعريف (Definition)", "المعنى بالعربي", "الكتاب"),
            columnWidths = listOf(22, 18, 44, 24, 20),
            rows = definitionRows
        )

        val derivativesRows = words.filter { it.derivatives.isNotBlank() }.map { w ->
            listOf(
                w.displayTitle(),
                w.derivatives,
                w.partsOfSpeech.joinToString("، "),
                w.arabicMeaning
            )
        }
        val sheet4Derivatives = SheetSpec(
            sheetName = "الاشتقاقات",
            fileBaseName = "04_Derivatives",
            headers = listOf("الكلمة", "الاشتقاقات (Derivatives)", "موقعها من الكلام", "المعنى بالعربي"),
            columnWidths = listOf(22, 42, 20, 24),
            rows = derivativesRows
        )

        val lingNotesRows = words.filter {
            it.linguisticNotes.isNotBlank() || it.notes.isNotBlank() || it.importantNotes.isNotBlank()
        }.map { w ->
            listOf(
                w.displayTitle(),
                w.arabicMeaning,
                w.linguisticNotes,
                w.importantNotes,
                w.notes
            )
        }
        val sheet5LinguisticNotes = SheetSpec(
            sheetName = "الملاحظات اللغوية",
            fileBaseName = "05_Linguistic_Notes",
            headers = listOf("الكلمة", "المعنى بالعربي", "الملاحظات اللغوية", "م&ملاحظات هامة", "ملاحظات عامة"),
            columnWidths = listOf(22, 22, 38, 34, 32),
            rows = lingNotesRows
        )

        val prepRows = words.filter { it.prepositions.isNotEmpty() }.map { w ->
            listOf(
                w.displayTitle(),
                w.prepositions.joinToString("، "),
                w.arabicMeaning,
                w.examples.joinToString(" | ")
            )
        }
        val sheet6Prepositions = SheetSpec(
            sheetName = "حروف الجر",
            fileBaseName = "06_Prepositions",
            headers = listOf("الكلمة", "حروف الجر (Prepositions)", "المعنى بالعربي", "أمثلة"),
            columnWidths = listOf(22, 34, 24, 36),
            rows = prepRows
        )

        val collocationsRows = words.filter { it.collocations.isNotEmpty() }.map { w ->
            listOf(
                w.displayTitle(),
                w.collocations.joinToString("، "),
                w.arabicMeaning,
                w.expressions
            )
        }
        val sheet7Collocations = SheetSpec(
            sheetName = "المتلازمات اللفظية",
            fileBaseName = "07_Collocations",
            headers = listOf("الكلمة", "المتلازمات اللفظية (Collocations)", "المعنى بالعربي", "التعبيرات اللغوية"),
            columnWidths = listOf(22, 36, 24, 34),
            rows = collocationsRows
        )

        val exprExampleRows = words.filter {
            it.expressions.isNotBlank() || it.examples.isNotEmpty() || it.personalHint.isNotBlank()
        }.map { w ->
            listOf(
                w.displayTitle(),
                w.expressions,
                w.examples.joinToString("\n"),
                w.personalHint,
                w.arabicMeaning
            )
        }
        val sheet8Expressions = SheetSpec(
            sheetName = "التعبيرات والأمثلة",
            fileBaseName = "08_Expressions_Examples",
            headers = listOf("الكلمة", "التعبيرات اللغوية", "الأمثلة السياقية", "التلميح الذهني", "المعنى بالعربي"),
            columnWidths = listOf(22, 34, 40, 26, 22),
            rows = exprExampleRows
        )

        val curriculumRows = words.flatMap { w ->
            val locs = w.locations.filter { it.isNotBlank() }
            if (locs.isEmpty()) {
                listOf(
                    listOf(
                        w.books.joinToString("، ").ifBlank { "غير محدد" },
                        "-",
                        "-",
                        "-",
                        w.displayTitle(),
                        w.arabicMeaning,
                        w.difficultyLabelAr(),
                        w.studyTags.joinToString("، "),
                        SrsScheduler.maturityOf(w).labelAr
                    )
                )
            } else {
                locs.map { loc ->
                    listOf(
                        loc.book.ifBlank { w.books.joinToString("، ").ifBlank { "غير محدد" } },
                        loc.unit.ifBlank { "-" },
                        loc.lesson.ifBlank { "-" },
                        loc.page.ifBlank { "-" },
                        w.displayTitle(),
                        w.arabicMeaning,
                        w.difficultyLabelAr(),
                        w.studyTags.joinToString("، "),
                        SrsScheduler.maturityOf(w).labelAr
                    )
                }
            }
        }
        val sheet9Curriculum = SheetSpec(
            sheetName = "فهرس المنهج",
            fileBaseName = "09_Curriculum_Index",
            headers = listOf("الكتاب", "الوحدة", "الدرس", "الصفحة", "الكلمة", "المعنى بالعربي", "الصعوبة", "المجموعات", "مستوى الإتقان"),
            columnWidths = listOf(18, 12, 12, 12, 22, 24, 12, 22, 16),
            rows = curriculumRows
        )

        return listOf(
            mainSheet,
            sheet1Synonyms,
            sheet2Antonyms,
            sheet3Definitions,
            sheet4Derivatives,
            sheet5LinguisticNotes,
            sheet6Prepositions,
            sheet7Collocations,
            sheet8Expressions,
            sheet9Curriculum
        )
    }

    // -------------------------------------------------------------------------
    // 1. Offline PDF Generator (Android PdfDocument + StaticLayout for RTL)
    // -------------------------------------------------------------------------
    private fun exportToPdf(words: List<WordEntry>, outputStream: OutputStream) {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 32f
        val contentWidth = (pageWidth - margin * 2).toInt()

        val pdf = PdfDocument()
        var pageNumber = 1
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas

        val bgHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#155EEF")
            style = Paint.Style.FILL
        }
        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F4F8FF")
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#C7DAFF")
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DCE9FF")
            textSize = 11f
        }
        val wordTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B1F44")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val meaningPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#155EEF")
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val detailPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2B3D5B")
            textSize = 10.5f
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 9.5f
        }

        fun drawFooter(c: Canvas, pNum: Int) {
            val footerText = "صفحة $pNum • قاموسي"
            val layout = buildStaticLayout(footerText, footerPaint, contentWidth)
            c.save()
            c.translate(margin, pageHeight - 26f)
            layout.draw(c)
            c.restore()
        }

        val headerRect = RectF(margin, margin, pageWidth - margin, margin + 72f)
        canvas.drawRoundRect(headerRect, 18f, 18f, bgHeaderPaint)

        val dateLabel = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.US).format(Date())
        val bannerTitle = buildStaticLayout("قاموسي — قاموس المنهج الشخصي", titlePaint, contentWidth - 28)
        val bannerSub = buildStaticLayout("عدد الكلمات: ${words.size}  •  تاريخ التصدير: $dateLabel", subtitlePaint, contentWidth - 28)

        canvas.save()
        canvas.translate(margin + 14f, margin + 12f)
        bannerTitle.draw(canvas)
        canvas.translate(0f, bannerTitle.height + 4f)
        bannerSub.draw(canvas)
        canvas.restore()

        var cursorY = margin + 88f

        words.forEachIndexed { index, entry ->
            val innerWidth = contentWidth - 24
            val layouts = mutableListOf<Pair<StaticLayout, Float>>()

            val titleLine = buildString {
                append("${index + 1}. ")
                append(entry.displayTitle())
                if (entry.ipa.isNotBlank()) append("  ${entry.ipa}")
                if (entry.partsOfSpeech.isNotEmpty()) append("  [${entry.partsOfSpeech.joinToString("، ")}]")
            }
            layouts.add(buildStaticLayout(titleLine, wordTitlePaint, innerWidth) to 4f)

            if (entry.arabicMeaning.isNotBlank()) {
                layouts.add(buildStaticLayout("المعنى: ${entry.arabicMeaning}", meaningPaint, innerWidth) to 4f)
            }

            val detailLines = buildList {
                if (entry.books.isNotEmpty()) add("الكتاب: ${entry.books.joinToString("، ")}")
                val locStr = entry.locations.filter { it.isNotBlank() }.joinToString(" | ") { it.formattedLabel() }
                if (locStr.isNotBlank()) add("المواقع: $locStr")
                if (entry.synonyms.isNotEmpty()) add("المرادفات: ${entry.synonyms.joinToString("، ")}")
                if (entry.antonyms.isNotEmpty()) add("المضادات: ${entry.antonyms.joinToString("، ")}")
                if (entry.prepositions.isNotEmpty()) add("حروف الجر: ${entry.prepositions.joinToString("، ")}")
                if (entry.collocations.isNotEmpty()) add("المتلازمات اللفظية: ${entry.collocations.joinToString("، ")}")
                if (entry.definition.isNotBlank()) add("التعريف: ${entry.definition}")
                if (entry.derivatives.isNotBlank()) add("الاشتقاقات: ${entry.derivatives}")
                if (entry.expressions.isNotBlank()) add("التعبيرات اللغوية: ${entry.expressions}")
                if (entry.linguisticNotes.isNotBlank()) add("الملاحظات اللغوية: ${entry.linguisticNotes}")
                if (entry.examples.isNotEmpty()) add("الأمثلة: ${entry.examples.joinToString(" / ")}")
                if (entry.personalHint.isNotBlank()) add("التلميح الذهني: ${entry.personalHint}")
                if (entry.notes.isNotBlank()) add("ملاحظات: ${entry.notes}")
                if (entry.importantNotes.isNotBlank()) add("م&ملاحظات هامة: ${entry.importantNotes}")
                if (entry.studyTags.isNotEmpty()) add("المجموعات: ${entry.studyTags.joinToString("، ")}")
            }

            if (detailLines.isNotEmpty()) {
                val joined = detailLines.joinToString("\n")
                layouts.add(buildStaticLayout(joined, detailPaint, innerWidth) to 2f)
            }

            val cardContentHeight = layouts.sumOf { (l, gap) -> l.height + gap.toInt() } + 20f

            if (cursorY + cardContentHeight > pageHeight - 40f) {
                drawFooter(canvas, pageNumber)
                pdf.finishPage(page)
                pageNumber++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                cursorY = margin
            }

            val cardRect = RectF(margin, cursorY, pageWidth - margin, cursorY + cardContentHeight)
            canvas.drawRoundRect(cardRect, 14f, 14f, cardFillPaint)
            canvas.drawRoundRect(cardRect, 14f, 14f, cardBorderPaint)

            var innerY = cursorY + 10f
            layouts.forEach { (layout, gap) ->
                canvas.save()
                canvas.translate(margin + 12f, innerY)
                layout.draw(canvas)
                canvas.restore()
                innerY += layout.height + gap
            }

            cursorY += cardContentHeight + 10f
        }

        drawFooter(canvas, pageNumber)
        pdf.finishPage(page)
        pdf.writeTo(outputStream)
        pdf.close()
    }

    private fun buildStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width.coerceAtLeast(10))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setLineSpacing(2f, 1.15f)
            .setIncludePad(false)
            .build()
    }

    // -------------------------------------------------------------------------
    // 2. Offline Google Sheets / CSV Generator (UTF-8 with BOM)
    // -------------------------------------------------------------------------
    private fun exportToCsv(words: List<WordEntry>, outputStream: OutputStream) {
        outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        writer.appendLine(MAIN_HEADERS.joinToString(",") { escapeCsv(it) })
        words.forEach { entry ->
            writer.appendLine(wordToMainRow(entry).joinToString(",") { escapeCsv(it) })
        }
        writer.flush()
    }

    private fun exportToCsvZipBundle(words: List<WordEntry>, outputStream: OutputStream) {
        val sheets = buildTenSheets(words)
        ZipOutputStream(outputStream).use { zip ->
            sheets.forEach { spec ->
                zip.putNextEntry(ZipEntry("${spec.fileBaseName}.csv"))
                zip.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                val sb = StringBuilder()
                sb.appendLine(spec.headers.joinToString(",") { escapeCsv(it) })
                spec.rows.forEach { row ->
                    sb.appendLine(row.joinToString(",") { escapeCsv(it) })
                }
                zip.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    private fun escapeCsv(value: String): String {
        val clean = value.replace("\r\n", "\n")
        return if (clean.contains(',') || clean.contains('"') || clean.contains('\n')) {
            "\"${clean.replace("\"", "\"\"")}\""
        } else {
            clean
        }
    }

    // -------------------------------------------------------------------------
    // 3. Offline Pure-Kotlin 10-Sheet Excel (.xlsx OpenXML) Generator
    // -------------------------------------------------------------------------
    private fun exportToXlsx(words: List<WordEntry>, outputStream: OutputStream) {
        val sheets = buildTenSheets(words)
        ZipOutputStream(outputStream).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", buildContentTypesXml(sheets.size))
            writeZipEntry(zip, "_rels/.rels", ROOT_RELS_XML)
            writeZipEntry(zip, "xl/workbook.xml", buildWorkbookXml(sheets))
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml(sheets.size))
            writeZipEntry(zip, "xl/styles.xml", STYLES_XML)
            sheets.forEachIndexed { idx, spec ->
                val sheetNum = idx + 1
                writeZipEntry(
                    zip,
                    "xl/worksheets/sheet$sheetNum.xml",
                    buildWorksheetXml(spec, isSelected = idx == 0)
                )
            }
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildContentTypesXml(sheetCount: Int): String = buildString {
        append(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
"""
        )
        for (i in 1..sheetCount) {
            append("""  <Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" + "\n")
        }
        append(
            """  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
        )
    }

    private fun buildWorkbookXml(sheets: List<SheetSpec>): String = buildString {
        append(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
"""
        )
        sheets.forEachIndexed { idx, spec ->
            val id = idx + 1
            val safeName = spec.sheetName.take(31)
            append("""    <sheet name="${xmlEscape(safeName)}" sheetId="$id" r:id="rId$id"/>""" + "\n")
        }
        append(
            """  </sheets>
</workbook>"""
        )
    }

    private fun buildWorkbookRelsXml(sheetCount: Int): String = buildString {
        append(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
"""
        )
        for (i in 1..sheetCount) {
            append("""  <Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>""" + "\n")
        }
        val stylesRelId = sheetCount + 1
        append("""  <Relationship Id="rId$stylesRelId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""" + "\n")
        append("</Relationships>")
    }

    private fun buildWorksheetXml(spec: SheetSpec, isSelected: Boolean): String {
        val sb = StringBuilder()
        val tabAttr = if (isSelected) "tabSelected=\"1\" " else ""
        sb.append(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView ${tabAttr}rightToLeft="1" workbookViewId="0"/>
  </sheetViews>
  <sheetFormatPr defaultRowHeight="22"/>
  <cols>
"""
        )
        spec.columnWidths.forEachIndexed { idx, width ->
            val colNum = idx + 1
            sb.append("    <col min=\"$colNum\" max=\"$colNum\" width=\"$width\" customWidth=\"1\"/>\n")
        }
        sb.append("  </cols>\n  <sheetData>\n")

        sb.append("    <row r=\"1\" ht=\"28\" customHeight=\"1\">\n")
        spec.headers.forEachIndexed { colIndex, header ->
            val ref = "${colLetter(colIndex)}1"
            sb.append("      <c r=\"$ref\" s=\"1\" t=\"inlineStr\"><is><t>${xmlEscape(header)}</t></is></c>\n")
        }
        sb.append("    </row>\n")

        spec.rows.forEachIndexed { rowIdx, row ->
            val excelRow = rowIdx + 2
            val styleId = if (rowIdx % 2 == 1) 2 else 0
            sb.append("    <row r=\"$excelRow\">\n")
            row.forEachIndexed { colIndex, cellVal ->
                val ref = "${colLetter(colIndex)}$excelRow"
                sb.append("      <c r=\"$ref\" s=\"$styleId\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${xmlEscape(cellVal)}</t></is></c>\n")
            }
            sb.append("    </row>\n")
        }

        sb.append("  </sheetData>\n</worksheet>")
        return sb.toString()
    }

    private fun colLetter(index: Int): String {
        var idx = index
        val result = StringBuilder()
        while (idx >= 0) {
            result.insert(0, ('A' + (idx % 26)))
            idx = idx / 26 - 1
        }
        return result.toString()
    }

    private fun xmlEscape(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> {
                    val code = ch.code
                    if (code == 0x9 || code == 0xA || code == 0xD || code >= 0x20) {
                        sb.append(ch)
                    }
                }
            }
        }
        return sb.toString()
    }

    private const val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><color rgb="FF0B1F44"/><name val="Calibri"/></font>
    <font><b/><sz val="12"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
  </fonts>
  <fills count="4">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF155EEF"/></patternFill></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFF3F7FF"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFC7DAFF"/></left>
      <right style="thin"><color rgb="FFC7DAFF"/></right>
      <top style="thin"><color rgb="FFC7DAFF"/></top>
      <bottom style="thin"><color rgb="FFC7DAFF"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="3">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" applyAlignment="1" applyBorder="1">
      <alignment vertical="top" wrapText="1"/>
    </xf>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" applyFont="1" applyFill="1" applyAlignment="1" applyBorder="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <xf numFmtId="0" fontId="0" fillId="3" borderId="1" applyFill="1" applyAlignment="1" applyBorder="1">
      <alignment vertical="top" wrapText="1"/>
    </xf>
  </cellXfs>
</styleSheet>"""
}
