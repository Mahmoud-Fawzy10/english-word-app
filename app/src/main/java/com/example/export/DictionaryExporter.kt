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
        titleAr = "ملف Excel (.xlsx)",
        subtitleAr = "جدول إكسل حقيقي بأعمدة منسقة جاهز للفتح",
        extension = "xlsx",
        mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ),
    GOOGLE_SHEETS(
        titleAr = "جدول Google Sheets (.csv)",
        subtitleAr = "متوافق تماماً مع Google Sheets باللغة العربية أوفلاين",
        extension = "csv",
        mimeType = "text/csv"
    )
}

object DictionaryExporter {

    private val HEADERS = listOf(
        "الكلمة",
        "المعنى بالعربي",
        "الكتاب",
        "المواقع (وحدة / درس / صفحة)",
        "موقعها من الكلام",
        "المرادفات",
        "المضادات",
        "التعريف",
        "الاشتقاقات",
        "التعبيرات اللغوية",
        "ملاحظات عليها",
        "م&ملاحظات هامة",
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
        }
    }

    fun exportToShareableUri(
        context: Context,
        words: List<WordEntry>,
        format: ExportFormat
    ): Uri {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        // Clean previous cached files to keep storage tidy
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

    private fun wordToRow(entry: WordEntry): List<String> {
        val locationsStr = entry.locations
            .filter { it.isNotBlank() }
            .joinToString(" | ") { it.formattedLabel() }

        return listOf(
            entry.word,
            entry.arabicMeaning,
            entry.books.joinToString("، "),
            locationsStr,
            entry.partsOfSpeech.joinToString("، "),
            entry.synonyms.joinToString("، "),
            entry.antonyms.joinToString("، "),
            entry.definition,
            entry.derivatives,
            entry.expressions,
            entry.notes,
            entry.importantNotes,
            if (entry.isFavorite) "نعم" else "لا"
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

        // Draw top banner on first page
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
                if (entry.partsOfSpeech.isNotEmpty()) {
                    append("  [${entry.partsOfSpeech.joinToString("، ")}]")
                }
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
                if (entry.definition.isNotBlank()) add("التعريف: ${entry.definition}")
                if (entry.derivatives.isNotBlank()) add("الاشتقاقات: ${entry.derivatives}")
                if (entry.expressions.isNotBlank()) add("التعبيرات اللغوية: ${entry.expressions}")
                if (entry.notes.isNotBlank()) add("ملاحظات: ${entry.notes}")
                if (entry.importantNotes.isNotBlank()) add("م&ملاحظات هامة: ${entry.importantNotes}")
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
        // Write UTF-8 BOM so Google Sheets and Excel detect Arabic encoding automatically
        outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
            writer.appendLine(HEADERS.joinToString(",") { escapeCsv(it) })
            words.forEach { entry ->
                writer.appendLine(wordToRow(entry).joinToString(",") { escapeCsv(it) })
            }
            writer.flush()
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
    // 3. Offline Pure-Kotlin Excel (.xlsx OpenXML) Generator
    // -------------------------------------------------------------------------
    private fun exportToXlsx(words: List<WordEntry>, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", CONTENT_TYPES_XML)
            writeZipEntry(zip, "_rels/.rels", ROOT_RELS_XML)
            writeZipEntry(zip, "xl/workbook.xml", WORKBOOK_XML)
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS_XML)
            writeZipEntry(zip, "xl/styles.xml", STYLES_XML)
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", buildWorksheetXml(words))
        }
    }

    private fun writeZipEntry(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildWorksheetXml(words: List<WordEntry>): String {
        val sb = StringBuilder()
        sb.append(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView tabSelected="1" rightToLeft="1" workbookViewId="0"/>
  </sheetViews>
  <sheetFormatPr defaultRowHeight="22"/>
  <cols>
    <col min="1" max="1" width="22" customWidth="1"/>
    <col min="2" max="2" width="24" customWidth="1"/>
    <col min="3" max="3" width="20" customWidth="1"/>
    <col min="4" max="4" width="28" customWidth="1"/>
    <col min="5" max="7" width="20" customWidth="1"/>
    <col min="8" max="12" width="30" customWidth="1"/>
    <col min="13" max="13" width="12" customWidth="1"/>
  </cols>
  <sheetData>
"""
        )

        // Header row (row 1, style 1 = Cobalt Blue background + white bold text)
        sb.append("    <row r=\"1\" ht=\"28\" customHeight=\"1\">\n")
        HEADERS.forEachIndexed { colIndex, header ->
            val ref = "${colLetter(colIndex)}1"
            sb.append("      <c r=\"$ref\" s=\"1\" t=\"inlineStr\"><is><t>${xmlEscape(header)}</t></is></c>\n")
        }
        sb.append("    </row>\n")

        // Data rows
        words.forEachIndexed { rowIdx, entry ->
            val excelRow = rowIdx + 2
            val styleId = if (rowIdx % 2 == 1) 2 else 0
            sb.append("    <row r=\"$excelRow\">\n")
            wordToRow(entry).forEachIndexed { colIndex, cellVal ->
                val ref = "${colLetter(colIndex)}$excelRow"
                sb.append("      <c r=\"$ref\" s=\"$styleId\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${xmlEscape(cellVal)}</t></is></c>\n")
            }
            sb.append("    </row>\n")
        }

        sb.append(
            """  </sheetData>
</worksheet>"""
        )
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

    private const val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private const val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val WORKBOOK_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="قاموسي" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

    private const val WORKBOOK_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
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
