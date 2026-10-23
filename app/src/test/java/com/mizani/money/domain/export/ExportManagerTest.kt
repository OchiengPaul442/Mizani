package com.mizani.money.domain.export

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportManagerTest {

    @Test
    fun `csv cells starting with formula-trigger characters are neutralized with a leading quote`() {
        // csvEscape is a stateless package-level helper in com.mizani.money.domain.export.
        // Call it directly (no instance, no reflection) so the security-critical
        // injection-neutralisation invariant is tested without a Context or repository.

        assertEquals("'=SUM(A1:A10)", csvEscape("=SUM(A1:A10)"))
        assertEquals("'+1234", csvEscape("+1234"))
        assertEquals("'-1234", csvEscape("-1234"))
        assertEquals("'@cell", csvEscape("@cell"))
        // Plain values are not mangled; normal CSV round-tripping is preserved.
        assertEquals("hello", csvEscape("hello"))
        assertEquals("42", csvEscape("42"))
        // Commas still trigger standard CSV quoting.
        assertEquals("\"hello, world\"", csvEscape("hello, world"))
    }

    @Test
    fun `csv numeric amount field never formula-prefixes negative values`() {
        // Numeric cells take a formula-neutral quoting path: a negative ADJUSTMENT amount
        // must be emitted verbatim as a number, not text with a leading apostrophe.
        assertEquals("-500", csvEscapeNumeric("-500"))
        assertEquals("500", csvEscapeNumeric("500"))
        assertEquals("0", csvEscapeNumeric("0"))
        // A value that happens to contain a comma still gets standard RFC-4180 quoting.
        assertEquals("\"-1,500\"", csvEscapeNumeric("-1,500"))
    }

    @Test
    fun `free-text neutralization reaches past a leading space to the formula trigger`() {
        // A leading space defeats a naive `value.first()` check; the cell is still a formula.
        // Neutralization prepends a quote to the *original* value, preserving the leading space.
        assertEquals("' =cmd()", csvEscape(" =cmd()"))
        assertEquals("' +1", csvEscape(" +1"))
        assertEquals("' @x", csvEscape(" @x"))
    }

    @Test
    fun `free-text neutralization catches a leading tab carriage-return or newline`() {
        // Leading tab/CR/LF also bypass a `first()`-only check; the first non-whitespace
        // character is still a trigger, and the value itself begins with whitespace.
        assertEquals("'\t=cmd()", csvEscape("\t=cmd()"))
        assertEquals("\"'\n=cmd()\"", csvEscape("\n=cmd()"))
        assertEquals("\"'\r=cmd()\"", csvEscape("\r=cmd()"))
    }
}
