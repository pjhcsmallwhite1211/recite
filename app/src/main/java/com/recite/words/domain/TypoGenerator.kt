package com.recite.words.domain

import kotlin.random.Random

/**
 * 拼写扰动假词生成器。
 *
 * 对齐参考项目 `test.html` 的 `generateTypoWord` / `shouldGenerateTypoOption`：
 * 在「看中文选英文」模式下造一个与原词形近的假词作为干扰项，
 * 迫使学习者辨析拼写，而不是靠中文释义反推。
 *
 * 四种扰动策略（随机选一种，保底再做一次相邻字符交换）：
 * 1. 元音替换；若词内无元音则交换相邻字符
 * 2. 后缀替换（按词长截去 2~4 个字符后换上常见后缀）
 * 3. 前缀替换
 * 4. 交换相邻两个字符
 *
 * 注入 [Random] 是为了让单元测试可复现。
 */
class TypoGenerator(private val random: Random = Random.Default) {

    /**
     * 本次是否要生成假词干扰项。
     *
     * 概率取自参考项目：词长 ≤7 或 ≥14 时为 [SHORT_OR_LONG_RATE]，
     * 8~13 时为 [MID_RATE]。中等长度单词本身干扰词相似度已足够，故降频。
     */
    fun shouldGenerate(word: String): Boolean = random.nextDouble() < typoRate(word)

    /** 生成一个与原词形近的假词；词长不足 2 时原样返回。 */
    fun generate(word: String): String {
        if (word.length < 2) return word

        var result = when (random.nextInt(STRATEGY_COUNT)) {
            0 -> replaceVowel(word)
            1 -> replaceSuffix(word)
            2 -> replacePrefix(word)
            else -> swapAdjacent(word)
        }
        if (result == word) result = swapAdjacent(word)
        return result
    }

    private fun replaceVowel(word: String): String {
        val chars = word.toCharArray()
        val vowelPositions = chars.indices.filter { chars[it].lowercaseChar() in VOWELS }
        if (vowelPositions.isEmpty()) return swapAdjacent(word)

        val position = vowelPositions[random.nextInt(vowelPositions.size)]
        val previous = chars[position].lowercaseChar()
        var replacement: Char
        do {
            replacement = VOWELS[random.nextInt(VOWELS.size)]
        } while (replacement == previous)

        chars[position] =
            if (chars[position].isUpperCase()) replacement.uppercaseChar() else replacement
        return String(chars)
    }

    private fun replaceSuffix(word: String): String {
        val tailLength = minOf(MAX_TAIL_LENGTH, maxOf(MIN_TAIL_LENGTH, (word.length * 0.25).toInt()))
        val suffix = SUFFIXES[random.nextInt(SUFFIXES.size)]
        return word.dropLast(tailLength) + suffix
    }

    private fun replacePrefix(word: String): String {
        val prefix = PREFIXES[random.nextInt(PREFIXES.size)]
        return prefix + word.drop(PREFIX_DROP_LENGTH)
    }

    private fun swapAdjacent(word: String): String {
        if (word.length < 2) return word
        val chars = word.toCharArray()
        val position = random.nextInt(chars.size - 1)
        val swapped = chars[position]
        chars[position] = chars[position + 1]
        chars[position + 1] = swapped
        return String(chars)
    }

    companion object {
        /** 生成假词的概率：词长 ≤7 或 ≥14。 */
        const val SHORT_OR_LONG_RATE = 0.6

        /** 生成假词的概率：词长 8~13。 */
        const val MID_RATE = 0.3

        private const val STRATEGY_COUNT = 4
        private const val MIN_TAIL_LENGTH = 2
        private const val MAX_TAIL_LENGTH = 4
        private const val PREFIX_DROP_LENGTH = 3

        private val VOWELS = charArrayOf('a', 'e', 'i', 'o', 'u')

        private val SUFFIXES = listOf(
            "tion", "sion", "ment", "ant", "ent",
            "ous", "ious", "tive", "sive", "ance", "ence",
        )

        private val PREFIXES = listOf("sp", "st", "re", "de", "con", "com", "in", "im", "ex")

        /**
         * 按词长给出生成假词的概率，供 [shouldGenerate] 与单元测试共用。
         */
        fun typoRate(word: String): Double =
            if (word.length <= 7 || word.length >= 14) SHORT_OR_LONG_RATE else MID_RATE
    }
}
