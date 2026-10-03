package com.recite.words.domain

/**
 * 多选测试的难度档。
 *
 * 难度**只作用在干扰项上**：选项数量固定 4 个，变的是干扰项与正确答案「像不像」，
 * 以及是否插入拼写扰动假词。出题方向（看英选中 / 看中选英）与是否乱序是独立开关，
 * 不与难度耦合。
 */
enum class QuizDifficulty(val label: String) {
    /** 只要认得这些词就能选对：干扰项随机抽，不造假词。 */
    EASY("简单"),

    /** 与参考原型一致：随机干扰项 + 按词长概率插入 1 个拼写假词。 */
    NORMAL("普通"),

    /** 必须真正辨析拼写：干扰项优先挑形近真词，并高概率插入 1 个形近假词。 */
    HARD("困难");

    /** 是否生成拼写假词。 */
    val usesTypo: Boolean get() = this != EASY

    /**
     * 假词概率的倍率（作用在 [TypoGenerator.typoRate] 的基础上，上限 1.0）。
     *
     * 简单档为 0 表示完全不造假词；困难档提高频率。
     */
    val typoRateScale: Double
        get() = when (this) {
            EASY -> 0.0
            NORMAL -> 1.0
            HARD -> 1.4
        }

    /** 干扰项是否优先挑与正确答案「形近」的真词。 */
    val prefersSimilarDistractors: Boolean get() = this == HARD
}
