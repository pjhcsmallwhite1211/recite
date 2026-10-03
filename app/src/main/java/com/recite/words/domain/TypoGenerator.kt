package com.recite.words.domain

import kotlin.random.Random

/**
 * 拼写扰动假词生成器。
 *
 * 对齐参考项目 `test.html` 的 `generateTypoWord`：在「看中文选英文」模式下
 * 造一个与原词形近的假词作为干扰项，迫使学习者辨析拼写，而不是靠中文释义反推。
 *
 * 四种扰动策略（随机选一种）：
 * 1. 元音替换；若词内无元音则交换相邻字符
 * 2. 后缀替换（按词长截去 2~4 个字符后换上常见后缀）
 * 3. 前缀替换（丢掉一部分词首后接上常见前缀）
 * 4. 交换相邻两个字符
 *
 * 与原参考实现的两处**有意修正**：
 * - 前缀替换丢掉的长度自适应（`min(3, max(1, 词长/2))`）。参考实现写死丢 3 个字符，
 *   短词会被掏空：`cat` → `st`、`apple` → `imle`，那种"假词"一眼就能排除。
 * - 生成后做**相似度质检**（见 [WordSimilarity]）：与原词差太远就换策略重造，
 *   仍不行则退化为相邻字符交换，保证假词始终"形近"。
 *
 * 注入 [Random] 是为了让单元测试可复现。
 */
class TypoGenerator(private val random: Random = Random.Default) {

    /**
     * 本次是否要生成假词干扰项。
     *
     * 基础概率取自参考项目：词长 ≤7 或 ≥14 时为 [SHORT_OR_LONG_RATE]，
     * 8~13 时为 [MID_RATE]。中等长度单词的干扰词相似度已足够，故降频。
     *
     * @param rateScale 难度档的强度倍率；0 表示不生成，>1 表示更频繁。
     */
    fun shouldGenerate(word: String, rateScale: Double = 1.0): Boolean {
        if (rateScale <= 0.0) return false
        return random.nextDouble() < (typoRate(word) * rateScale).coerceAtMost(1.0)
    }

    /**
     * 生成一个与原词形近的假词；词长不足 2 时原样返回。
     *
     * 会做相似度质检：连续几种策略都造不出"像"的词时，退化为交换相邻字符
     * （对任何多字符单词，互换两位一定与原词足够接近）。
     */
    fun generate(word: String): String {
        if (word.length < 2) return word

        repeat(MAX_ATTEMPTS) {
            val candidate = attempt(word)
            if (candidate != word && WordSimilarity.similarity(candidate, word) >= MIN_SIMILARITY) {
                return candidate
            }
        }
        return swapAdjacent(word)
    }

    /** 随机用一种策略造词；若造出来与原词相同，再交换一次相邻字符。 */
    private fun attempt(word: String): String {
        val candidate = when (random.nextInt(STRATEGY_COUNT)) {
            0 -> replaceVowel(word)
            1 -> replaceSuffix(word)
            2 -> replacePrefix(word)
            else -> swapAdjacent(word)
        }
        return if (candidate == word) swapAdjacent(word) else candidate
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
        // 丢掉的长度随词长自适应：短词少丢，保证结果仍与原词共享主体
        val dropLength = minOf(MAX_PREFIX_DROP, maxOf(MIN_PREFIX_DROP, word.length / 2))
        return prefix + word.drop(dropLength)
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

        /** 假词与原词的最低相似度；低于此值视为"不像词"，丢弃重造。 */
        const val MIN_SIMILARITY = 0.5

        private const val STRATEGY_COUNT = 4
        private const val MAX_ATTEMPTS = 8
        private const val MIN_TAIL_LENGTH = 2
        private const val MAX_TAIL_LENGTH = 4
        private const val MIN_PREFIX_DROP = 1
        private const val MAX_PREFIX_DROP = 3

        private val VOWELS = charArrayOf('a', 'e', 'i', 'o', 'u')

        private val SUFFIXES = listOf(
            "tion", "sion", "ment", "ant", "ent",
            "ous", "ious", "tive", "sive", "ance", "ence",
        )

        private val PREFIXES = listOf("sp", "st", "re", "de", "con", "com", "in", "im", "ex")

        /**
         * 按词长给出生成假词的基础概率。
         */
        fun typoRate(word: String): Double =
            if (word.length <= 7 || word.length >= 14) SHORT_OR_LONG_RATE else MID_RATE
    }
}
