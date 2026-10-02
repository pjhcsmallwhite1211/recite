package com.recite.words

import com.recite.words.data.WordBook
import com.recite.words.data.WordItem
import com.recite.words.domain.QuizMode
import com.recite.words.domain.QuizOptionBuilder
import com.recite.words.domain.WordPool
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
