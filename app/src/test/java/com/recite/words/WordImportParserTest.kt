package com.recite.words

import com.recite.words.domain.WordImportParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** [WordImportParser] 的单元测试，覆盖参考项目网页版的导入格式与各种边界。 */
class WordImportParserTest {

    @Test
    fun parsesPercentSeparatedLine() {
        val result = WordImportParser.parse("survive%v.幸存；存活")
        assertEquals(1, result.items.size)
        assertEquals("survive", result.items[0].word)
        assertEquals("v.幸存；存活", result.items[0].cn)
    }

    @Test
    fun percentSeparatorWinsOverDash() {
        // 释义里带短横也不会被误切
        val result = WordImportParser.parse("well-known%adj.著名的")
        assertEquals("well-known", result.items[0].word)
        assertEquals("adj.著名的", result.items[0].cn)
    }

    @Test
    fun stillAcceptsLegacyDashFormat() {
        val result = WordImportParser.parse("apple-n.苹果")
        assertEquals("apple", result.items[0].word)
        assertEquals("n.苹果", result.items[0].cn)
    }

    @Test
    fun parsesDashSeparatedLineLikeReferenceHtml() {
        val result = WordImportParser.parse("survive-v.幸存；存活")
        assertEquals(1, result.items.size)
        assertEquals("survive", result.items[0].word)
        assertEquals("v.幸存；存活", result.items[0].cn)
    }

    @Test
    fun parsesTabSeparatedLine() {
        val result = WordImportParser.parse("e-mail\t电子邮件")
        assertEquals(1, result.items.size)
        assertEquals("e-mail", result.items[0].word)
        assertEquals("电子邮件", result.items[0].cn)
    }

    @Test
    fun parsesWhitespaceSeparatedLine() {
        val result = WordImportParser.parse("abandon v.放弃")
        assertEquals(1, result.items.size)
        assertEquals("abandon", result.items[0].word)
        assertEquals("v.放弃", result.items[0].cn)
    }

    @Test
    fun handlesCrLfAndBlankLines() {
        val result = WordImportParser.parse("apple-n.苹果\r\n\r\n   \nbanana-n.香蕉\r\n")
        assertEquals(2, result.items.size)
        assertEquals("apple", result.items[0].word)
        assertEquals("n.香蕉", result.items[1].cn)
        assertFalse(result.hasFailure)
    }

    @Test
    fun reportsUnparsableLinesWithLineNumber() {
        val result = WordImportParser.parse("apple-n.苹果\njustoneword\nbanana-n.香蕉")
        assertEquals(2, result.items.size)
        assertTrue(result.hasFailure)
        assertEquals(1, result.skippedLines.size)
        assertEquals(2, result.skippedLines[0].lineNumber)
        assertEquals("justoneword", result.skippedLines[0].text)
    }

    @Test
    fun ignoresLineWithEmptyMeaning() {
        val result = WordImportParser.parse("apple-\nbanana-n.香蕉")
        assertEquals(1, result.items.size)
        assertEquals("banana", result.items[0].word)
        assertEquals(1, result.skippedLines.size)
    }

    @Test
    fun keepsFullMeaningWithSpacesAndPunctuation() {
        val result = WordImportParser.parse("account-n.账户；账目, 描述 v. 解释")
        assertEquals(1, result.items.size)
        assertEquals("account", result.items[0].word)
        assertEquals("n.账户；账目, 描述 v. 解释", result.items[0].cn)
    }

    @Test
    fun returnsEmptyResultForBlankInput() {
        val result = WordImportParser.parse("   \n\n  ")
        assertTrue(result.items.isEmpty())
        assertFalse(result.hasFailure)
    }
}
