package com.recite.words.domain

import com.recite.words.data.WordBook
import com.recite.words.data.WordItem
import kotlin.random.Random

/** 选择题的两种出题方向，对齐参考项目 `test.html` 的 `quizMode`。 */
enum class QuizMode {
    /** 看英文选中文。 */
    EN_TO_CN,

    /** 看中文选英文。 */
    CN_TO_EN;

    /** 题干文本（取词条的哪一侧）。 */
    fun promptOf(item: WordItem): String = when (this) {
        EN_TO_CN -> item.word
        CN_TO_EN -> item.cn
    }

    /** 正确选项文本。 */
    fun answerOf(item: WordItem): String = when (this) {
        EN_TO_CN -> item.cn
        CN_TO_EN -> item.word
    }
}

/** 一个备选答案。 */
data class QuizOption(
    val text: String,
    val isRight: Boolean,
)

/**
 * 全部词本的词池，用于抽取干扰项。
 *
 * 对齐参考项目 `test.html` 的 `buildGlobalPool`：词池**跨全部单词本**，
 * 且把含空格的「短语」与单个单词分开，避免出现「选单词却混进短语」的荒谬干扰项。
 */
data class WordPool(
    /** 全部词条（跨词本）。 */
    val all: List<WordItem>,
    /** 不含空格的单词。 */
    val singles: List<WordItem>,
    /** 含空格的短语。 */
    val phrases: List<WordItem>,
) {
    companion object {
        /** 从全部单词本构建词池。 */
        fun from(books: List<WordBook>): WordPool {
            val all = books.flatMap { it.wordItems }
            val (phrases, singles) = all.partition { it.word.contains(' ') }
            return WordPool(all = all, singles = singles, phrases = phrases)
        }
    }
}

/**
 * 四选一选项构造器，对齐参考项目 `test.html` 的 `getQuizOptions`：
 *
 * 1. 正确项固定 1 个；
 * 2. 干扰项优先从**同类型词池**（短语 / 单词）里抽，再退回全量池补足；
 * 3. 「看中文选英文」模式下按概率用一个拼写扰动假词替换其中一个干扰项；
 * 4. 最终打乱顺序。
 *
 * 注入 [Random] 以便单元测试复现。
 */
class QuizOptionBuilder(
    private val random: Random = Random.Default,
    private val typoGenerator: TypoGenerator = TypoGenerator(random),
) {

    /** 构造一道题的选项；词池过小时返回的选项可能少于 [OPTION_COUNT]。 */
    fun buildOptions(correct: WordItem, mode: QuizMode, pool: WordPool): List<QuizOption> {
        val options = mutableListOf(QuizOption(mode.answerOf(correct), isRight = true))

        val preferredPool = if (correct.word.contains(' ')) pool.phrases else pool.singles
        fillFrom(preferredPool, correct, mode, options)
        if (options.size < OPTION_COUNT) {
            fillFrom(pool.all, correct, mode, options)
        }

        if (mode == QuizMode.CN_TO_EN && typoGenerator.shouldGenerate(correct.word)) {
            val fakeWord = typoGenerator.generate(correct.word)
            val firstDistractor = options.indexOfFirst { !it.isRight }
            if (firstDistractor >= 0 && options.none { it.text == fakeWord }) {
                options[firstDistractor] = QuizOption(fakeWord, isRight = false)
            }
        }

        return options.shuffled(random)
    }

    /** 依次从 [candidates] 取不重复的文本补足到 [OPTION_COUNT] 个选项。 */
    private fun fillFrom(
        candidates: List<WordItem>,
        correct: WordItem,
        mode: QuizMode,
        options: MutableList<QuizOption>,
    ) {
        for (candidate in candidates.shuffled(random)) {
            if (options.size >= OPTION_COUNT) return
            if (candidate.word == correct.word) continue
            val text = mode.answerOf(candidate)
            if (options.none { it.text == text }) {
                options += QuizOption(text, isRight = false)
            }
        }
    }

    companion object {
        /** 每题选项数。 */
        const val OPTION_COUNT = 4
    }
}
