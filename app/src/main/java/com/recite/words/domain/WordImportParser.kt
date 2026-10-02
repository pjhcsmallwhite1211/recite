package com.recite.words.domain

import com.recite.words.data.WordItem

/** 一行无法解析的词表输入。 */
data class SkippedLine(
    /** 行号，从 1 开始。 */
    val lineNumber: Int,
    /** 原始行内容。 */
    val text: String,
)

/** 词表文本的解析结果。 */
data class ImportResult(
    /** 成功解析出的词条。 */
    val items: List<WordItem>,
    /** 解析失败的行，供 UI 提示用户修正，避免静默丢词。 */
    val skippedLines: List<SkippedLine>,
) {
    /** 是否存在解析失败的行。 */
    val hasFailure: Boolean get() = skippedLines.isNotEmpty()
}

/**
 * 词表文本解析器，每行一条词条。
 *
 * 分隔方式按顺序尝试：
 * 1. 制表符：`word<TAB>释义`（最可靠，单词含连字符时请用这种）
 * 2. 短横 / 破折号（取第一个）：`word-释义`，与参考项目网页版行为一致
 * 3. 空白：`word 释义`
 *
 * 空行自动跳过；解析失败的行记入 [ImportResult.skippedLines]。
 */
object WordImportParser {

    /** 识别为分隔符的短横 / 破折号字符（半角、em dash、en dash、全角）。 */
    private val DASH_SEPARATORS = charArrayOf('-', '\u2014', '\u2013', '\uFF0D')

    /** 兜底规则：非空白串 + 空白 + 其余。 */
    private val WHITESPACE_SPLIT = Regex("""^(\S+)\s+(.+)$""")

    /** 解析整段词表文本。 */
    fun parse(text: String): ImportResult {
        val items = mutableListOf<WordItem>()
        val skipped = mutableListOf<SkippedLine>()
        text.split('\n').forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEachIndexed
            val item = parseLine(line)
            if (item == null) {
                skipped += SkippedLine(index + 1, line)
            } else {
                items += item
            }
        }
        return ImportResult(items, skipped)
    }

    private fun parseLine(line: String): WordItem? {
        val tabAt = line.indexOf('\t')
        if (tabAt > 0) {
            fromParts(line.substring(0, tabAt), line.substring(tabAt + 1))?.let { return it }
        }

        for (separator in DASH_SEPARATORS) {
            val at = line.indexOf(separator)
            if (at > 0) {
                fromParts(line.substring(0, at), line.substring(at + 1))?.let { return it }
            }
        }

        val match = WHITESPACE_SPLIT.find(line) ?: return null
        return fromParts(match.groupValues[1], match.groupValues[2])
    }

    private fun fromParts(word: String, cn: String): WordItem? {
        val trimmedWord = word.trim()
        val trimmedCn = cn.trim()
        if (trimmedWord.isEmpty() || trimmedCn.isEmpty()) return null
        return WordItem(trimmedWord, trimmedCn)
    }
}
