package com.recite.words.domain

/**
 * 词形相似度工具：基于 Levenshtein 编辑距离。
 *
 * 有两个用途：
 * 1. **难度档**：「困难」档要挑与正确答案形近的真词做干扰项；
 * 2. **假词质检**：拼写扰动生成的假词若与原词差太远（例如 `cat` → `st`），
 *    一眼就能排除，起不到混淆作用，应当丢弃重造。
 */
object WordSimilarity {

    /** 两词的编辑距离（大小写不敏感）。 */
    fun editDistance(a: String, b: String): Int {
        val source = a.lowercase()
        val target = b.lowercase()
        if (source.isEmpty()) return target.length
        if (target.isEmpty()) return source.length

        var previous = IntArray(target.length + 1) { it }
        var current = IntArray(target.length + 1)

        for (i in 1..source.length) {
            current[0] = i
            for (j in 1..target.length) {
                val substitution = if (source[i - 1] == target[j - 1]) 0 else 1
                current[j] = minOf(
                    current[j - 1] + 1,        // 插入
                    previous[j] + 1,           // 删除
                    previous[j - 1] + substitution, // 替换
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[target.length]
    }

    /**
     * 归一化相似度：1.0 表示完全相同，0.0 表示完全不同。
     *
     * 等价于「1 − 编辑距离 / 较长串长度」，因此结果恒在 [0, 1]。
     */
    fun similarity(a: String, b: String): Double {
        val longest = maxOf(a.length, b.length)
        if (longest == 0) return 1.0
        return 1.0 - editDistance(a, b).toDouble() / longest
    }
}
