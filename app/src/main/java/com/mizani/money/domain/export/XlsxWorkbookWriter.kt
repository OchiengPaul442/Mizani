package com.mizani.money.domain.export

import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal sealed interface ExcelCell {
    data class Text(val value: String) : ExcelCell
    data class Number(val value: String) : ExcelCell
    data class Date(val value: LocalDate) : ExcelCell
}

internal data class ExcelSheet(
    val name: String,
    val headers: List<String>,
    val rows: List<List<ExcelCell>>,
)

/** Writes a small, dependency-free OOXML workbook. All user text is stored as inline strings. */
internal object XlsxWorkbookWriter {
    private const val MAX_CELL_CHARACTERS = 32_767

    fun write(output: OutputStream, sheets: List<ExcelSheet>) {
        require(sheets.isNotEmpty()) { "A workbook must contain at least one sheet" }
        require(sheets.size <= 255) { "A workbook cannot contain more than 255 sheets" }
        require(sheets.all { it.headers.isNotEmpty() && it.headers.size <= 16_384 }) {
            "A worksheet must contain between 1 and 16,384 columns"
        }
        require(sheets.all { it.rows.size <= 1_048_575 }) {
            "A worksheet cannot contain more than 1,048,575 data rows"
        }

        ZipOutputStream(output, StandardCharsets.UTF_8).use { zip ->
            val writer = OutputStreamWriter(zip, StandardCharsets.UTF_8)
            writeEntry(zip, writer, "[Content_Types].xml") { xml ->
                xml.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                xml.write("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                xml.write("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                xml.write("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                xml.write("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
                xml.write("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
                sheets.indices.forEach { index ->
                    xml.write("<Override PartName=\"/xl/worksheets/sheet${index + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
                }
                xml.write("</Types>")
            }

            writeEntry(zip, writer, "_rels/.rels") { xml ->
                xml.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                xml.write("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
                xml.write("<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>")
                xml.write("</Relationships>")
            }

            writeEntry(zip, writer, "xl/workbook.xml") { xml ->
                xml.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                xml.write("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">")
                xml.write("<bookViews><workbookView xWindow=\"0\" yWindow=\"0\" windowWidth=\"24000\" windowHeight=\"12000\"/></bookViews><sheets>")
                sheets.forEachIndexed { index, sheet ->
                    xml.write("<sheet name=\"${escapeXml(sheet.name)}\" sheetId=\"${index + 1}\" r:id=\"rId${index + 1}\"/>")
                }
                xml.write("</sheets></workbook>")
            }

            writeEntry(zip, writer, "xl/_rels/workbook.xml.rels") { xml ->
                xml.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                xml.write("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
                sheets.indices.forEach { index ->
                    xml.write("<Relationship Id=\"rId${index + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${index + 1}.xml\"/>")
                }
                val stylesId = sheets.size + 1
                xml.write("<Relationship Id=\"rId$stylesId\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>")
                xml.write("</Relationships>")
            }

            writeEntry(zip, writer, "xl/styles.xml", STYLES_XML)
            sheets.forEachIndexed { index, sheet ->
                writeEntry(zip, writer, "xl/worksheets/sheet${index + 1}.xml") { xml ->
                    writeWorksheet(xml, sheet)
                }
            }
        }
    }

    private fun writeWorksheet(writer: Writer, sheet: ExcelSheet) {
        val lastColumn = columnName(sheet.headers.size)
        val lastRow = sheet.rows.size + 1
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        writer.write("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        writer.write("<dimension ref=\"A1:$lastColumn$lastRow\"/>")
        writer.write("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>")
        writer.write("<sheetFormatPr defaultRowHeight=\"18\"/><cols>")
        sheet.headers.forEachIndexed { index, header ->
            val width = header.length.coerceIn(10, 34)
            writer.write("<col min=\"${index + 1}\" max=\"${index + 1}\" width=\"$width\" customWidth=\"1\"/>")
        }
        writer.write("</cols><sheetData>")
        writer.write("<row r=\"1\">")
        sheet.headers.forEachIndexed { index, header -> writeTextCell(writer, "${columnName(index + 1)}1", header, 1) }
        writer.write("</row>")
        sheet.rows.forEachIndexed { rowIndex, row ->
            val rowNumber = rowIndex + 2
            writer.write("<row r=\"$rowNumber\">")
            row.take(sheet.headers.size).forEachIndexed { columnIndex, cell ->
                val reference = "${columnName(columnIndex + 1)}$rowNumber"
                when (cell) {
                    is ExcelCell.Text -> writeTextCell(writer, reference, cell.value)
                    is ExcelCell.Number -> {
                        val decimal = cell.value.toBigDecimalOrNull()?.toPlainString() ?: "0"
                        writer.write("<c r=\"$reference\" t=\"n\"><v>$decimal</v></c>")
                    }
                    is ExcelCell.Date -> {
                        val serial = cell.value.toEpochDay() + 25_569L
                        writer.write("<c r=\"$reference\" s=\"2\" t=\"n\"><v>$serial</v></c>")
                    }
                }
            }
            writer.write("</row>")
        }
        writer.write("</sheetData><autoFilter ref=\"A1:$lastColumn$lastRow\"/></worksheet>")
    }

    private fun writeTextCell(writer: Writer, reference: String, value: String, style: Int? = null) {
        val safe = truncateCell(xmlSafe(value))
        writer.write("<c r=\"$reference\" t=\"inlineStr\"")
        if (style != null) writer.write(" s=\"$style\"")
        writer.write("><is><t xml:space=\"preserve\">${escapeXml(safe)}</t></is></c>")
    }

    private fun writeEntry(zip: ZipOutputStream, writer: Writer, path: String, content: String) {
        writeEntry(zip, writer, path) { it.write(content) }
    }

    private fun writeEntry(zip: ZipOutputStream, writer: Writer, path: String, write: (Writer) -> Unit) {
        zip.putNextEntry(ZipEntry(path))
        write(writer)
        writer.flush()
        zip.closeEntry()
    }

    private fun xmlSafe(value: String): String = buildString(value.length) {
        value.codePoints().forEach { codePoint ->
            if (codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD || codePoint in 0x20..0xD7FF || codePoint in 0xE000..0xFFFD || codePoint in 0x10000..0x10FFFF) {
                appendCodePoint(codePoint)
            }
        }
    }

    private fun truncateCell(value: String): String {
        if (value.length <= MAX_CELL_CHARACTERS) return value
        var end = MAX_CELL_CHARACTERS
        if (value[end - 1].isHighSurrogate()) end--
        return value.substring(0, end)
    }

    private fun escapeXml(value: String): String = buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(character)
            }
        }
    }

    private fun columnName(column: Int): String {
        var value = column
        val name = StringBuilder()
        while (value > 0) {
            val remainder = (value - 1) % 26
            name.append(('A'.code + remainder).toChar())
            value = (value - 1) / 26
        }
        return name.reverse().toString()
    }

    private val STYLES_XML = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
        <numFmts count="1"><numFmt numFmtId="164" formatCode="yyyy-mm-dd"/></numFmts>
        <fonts count="2"><font><sz val="11"/><color theme="1"/><name val="Calibri"/><family val="2"/></font><font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/><family val="2"/></font></fonts>
        <fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF146B55"/><bgColor indexed="64"/></patternFill></fill></fills>
        <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
        <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
        <cellXfs count="3"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/><xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/></cellXfs>
        <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
        </styleSheet>
    """.trimIndent()
}
