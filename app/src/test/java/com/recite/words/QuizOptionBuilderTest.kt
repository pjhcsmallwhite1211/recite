package com.recite.words

import com.recite.words.data.WordBook
import com.recite.words.data.WordItem
import com.recite.words.domain.QuizDifficulty
import com.recite.words.domain.QuizMode
import com.recite.words.domain.QuizOptionBuilder
import com.recite.words.domain.WordPool
import com.recite.words.domain.WordSimilarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** [QuizOptionBuilder] 与 [WordPool] 的单元测试：对齐参考项目 `test.html` 的出题规则。 */
class QuizOptionBuilderTest {

    private fun poolOf(vararg items: WordItem): WordPool =
        WordPool.from(listOf(WordBook("b", "测试本", items.toList())))

    private fun builder(seed: Int = 20260803) = QuizOptionBuilder(Random(seed))

    @Test
    fun poolSeparatesPhrasesFromSingleWords() {
        val pool = poolOf(
            WordItem("apple", "n.苹果"),
            WordItem("look for", "短语：寻找"),
            WordItem("give up", "短语：放弃"),
        )
        assertEquals(3, pool.all.size)
        assertEquals(1, pool.singles.size)
        assertEquals(2, pool.phrases.size)
    }

    @Test
    fun buildsFourOptionsWithExactlyOneCorrect() {
        val pool = poolOf(
            WordItem("apple", "n.苹果"),
            WordItem("banana", "n.香蕉"),
            WordItem("cherry", "n.樱桃"),
            WordItem("durian", "n.榴莲"),
            WordItem("elder", "n.接骨木"),
        )
        val options = builder().buildOptions(
            correct = WordItem("apple", "n.苹果"),
            mode = QuizMode.EN_TO_CN,
            pool = pool,
        )

        assertEquals(QuizOptionBuilder.OPTION_COUNT, options.size)
        assertEquals(1, options.count { it.isRight })
        assertEquals("n.苹果", options.first { it.isRight }.text)
        assertEquals(options.size, options.map { it.text }.toSet().size)
    }

    @Test
    fun cnToEnReturnsWordAsCorrectAnswer() {
        val pool = poolOf(
            WordItem("apple", "n.苹果"),
            WordItem("banana", "n.香蕉"),
            WordItem("cherry", "n.樱桃"),
            WordItem("durian", "n.榴莲"),
            WordItem("elder", "n.接骨木"),
        )
        val options = builder().buildOptions(
            correct = WordItem("banana", "n.香蕉"),
            mode = QuizMode.CN_TO_EN,
            pool = pool,
        )
        assertEquals("banana", options.first { it.isRight }.text)
    }

    @Test
    fun phraseQuestionPrefersPhraseDistractors() {
        val pool = poolOf(
            WordItem("apple", "n.苹果"),
            WordItem("banana", "n.香蕉"),
            WordItem("look for", "短语：寻找"),
            WordItem("give up", "短语：放弃"),
            WordItem("take off", "短语：起飞"),
            WordItem("put on", "短语：穿上"),
        )
        val options = builder().buildOptions(
            correct = WordItem("look for", "短语：寻找"),
            mode = QuizMode.CN_TO_EN,
            pool = pool,
        )
        assertEquals(QuizOptionBuilder.OPTION_COUNT, options.size)
        // 短语池足够时，干扰项不应混入单个单词
        val distractors = options.filter { !it.isRight }.map { it.text }
        assertTrue("干扰项混入了单词: $distractors", distractors.none { !it.contains(' ') })
    }

    @Test
    fun returnsFewerOptionsWhenPoolTooSmall() {
        val pool = poolOf(WordItem("apple", "n.苹果"))
        val options = builder().buildOptions(
            correct = WordItem("apple", "n.苹果"),
            mode = QuizMode.EN_TO_CN,
            pool = pool,
        )
        assertEquals(1, options.size)
        assertTrue(options.single().isRight)
    }

    // ---------- 难度档 ----------

    private val difficultyPool = poolOf(
        WordItem("apple", "n.苹果"),
        WordItem("appear", "v.出现"),
        WordItem("appeal", "v.呼吁"),
        WordItem("apply", "v.申请"),
        WordItem("banana", "n.香蕉"),
        WordItem("zebra", "n.斑马"),
    )

    @Test
    fun easyDifficultyNeverInventsFakeWords() {
        val realWords = difficultyPool.all.map { it.word }.toSet()
        repeat(60) { seed ->
            val options = builder(seed).buildOptions(
                correct = WordItem("apple", "n.苹果"),
                mode = QuizMode.CN_TO_EN,
                pool = difficultyPool,
                difficulty = QuizDifficulty.EASY,
            )
            options.forEach { option ->
                assertTrue(
                    "简单档不应出现词表外的假词: ${option.text}",
                    option.text in realWords,
                )
            }
        }
    }

    @Test
    fun hardDifficultyPicksMoreSimilarDistractors() {
        val correct = WordItem("apple", "n.苹果")
        fun averageSimilarity(difficulty: QuizDifficulty): Double {
            var sum = 0.0; var count = 0
            repeat(40) { seed ->
                val options = builder(seed).buildOptions(correct, QuizMode.CN_TO_EN, difficultyPool, difficulty)
                options.filter { !it.isRight }.forEach {
                    sum += WordSimilarity.similarity(it.text, correct.word); count++
                }
            }
            return sum / count
        }
        val normal = averageSimilarity(QuizDifficulty.NORMAL)
        val hard = averageSimilarity(QuizDifficulty.HARD)
        assertTrue("困难档干扰项应更形近: normal=$normal hard=$hard", hard > normal)
    }

    @Test
    fun englishToChineseNeverShowsEnglishFakeWords() {
        // 回归:重构时丢过 mode 判断,导致英译中的中文选项里混进英文假词(如 divetion)
        val realMeanings = difficultyPool.all.map { it.cn }.toSet()
        repeat(80) { seed ->
            for (difficulty in QuizDifficulty.entries) {
                val options = builder(seed).buildOptions(
                    correct = WordItem("apple", "n.苹果"),
                    mode = QuizMode.EN_TO_CN,
                    pool = difficultyPool,
                    difficulty = difficulty,
                )
                options.forEach { option ->
                    assertTrue(
                        "英译中的选项必须是词表里的释义,却出现: ${option.text}",
                        option.text in realMeanings,
                    )
                }
            }
        }
    }

    @Test
    fun normalIsTheDefaultDifficulty() {
        val withDefault = builder(7).buildOptions(WordItem("apple", "n.苹果"), QuizMode.EN_TO_CN, difficultyPool)
        val explicitNormal = builder(7).buildOptions(
            WordItem("apple", "n.苹果"), QuizMode.EN_TO_CN, difficultyPool, QuizDifficulty.NORMAL,
        )
        assertEquals(withDefault, explicitNormal)
    }

    @Test
    fun neverDuplicatesOptionTextAcrossManyRuns() {
        val pool = poolOf(
            WordItem("apple", "n.苹果"),
            WordItem("banana", "n.香蕉"),
            WordItem("cherry", "n.樱桃"),
            WordItem("durian", "n.榴莲"),
            WordItem("elder", "n.接骨木"),
            WordItem("fig", "n.无花果"),
        )
        repeat(200) { seed ->
            for (mode in QuizMode.entries) {
                val options = builder(seed).buildOptions(
                    correct = WordItem("apple", "n.苹果"),
                    mode = mode,
                    pool = pool,
                )
                assertEquals(
                    "seed=$seed mode=$mode 出现重复选项",
                    options.size,
                    options.map { it.text }.toSet().size,
                )
                assertEquals(
                    "seed=$seed mode=$mode 正确项数量异常",
                    1,
                    options.count { it.isRight },
                )
            }
        }
    }
}
