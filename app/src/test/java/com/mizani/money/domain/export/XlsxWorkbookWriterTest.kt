package com.mizani.money.domain.export

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipInputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XlsxWorkbookWriterTest {

    @Test
    fun `workbook preserves sheets typed cells and formula safe text`() {
        val output = ByteArrayOutputStream()
        XlsxWorkbookWriter.write(
            output,
            listOf(
                ExcelSheet(
                    name = "Overview",
                    headers = listOf("Field", "Value"),
                    rows = listOf(listOf(ExcelCell.Text("Note"), ExcelCell.Text("=SUM(A1:A2)"))),
                ),
                ExcelSheet(
                    name = "Recurring",
                    headers = listOf("Amount", "Next date"),
                    rows = listOf(listOf(ExcelCell.Number("1250"), ExcelCell.Date(LocalDate.of(2026, 11, 8)))),
                ),
            ),
        )

        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
            }
        }

        val workbook = entries["xl/workbook.xml"]
        val overview = entries["xl/worksheets/sheet1.xml"]
        val recurring = entries["xl/worksheets/sheet2.xml"]
        assertNotNull(workbook)
        assertNotNull(overview)
        assertNotNull(recurring)
        assertTrue(workbook!!.contains("name=\"Overview\""))
        assertTrue(workbook.contains("name=\"Recurring\""))
        assertTrue(overview!!.contains("t=\"inlineStr\""))
        assertTrue(overview.contains("=SUM(A1:A2)"))
        assertFalse(overview.contains("<f>"))
        assertTrue(recurring!!.contains("t=\"n\"><v>1250</v>"))
        assertTrue(recurring.contains("s=\"2\" t=\"n\""))
    }
}
