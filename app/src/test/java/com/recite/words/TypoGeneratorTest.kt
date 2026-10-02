package com.recite.words

import com.recite.words.domain.TypoGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** [TypoGenerator] 的单元测试：对齐参考项目 `test.html` 的扰动策略与触发概率。 */
class TypoGeneratorTest {

    @Test
    fun typoRateUsesReferenceThresholds() {
        // 阈值取自参考项目 test.html：len<=7 或 len>=14 → 0.6，8~13 → 0.3
        assertEquals(TypoGenerator.SHORT_OR_LONG_RATE, TypoGenerator.typoRate("ability"), 0.0) // 7
        assertEquals(TypoGenerator.MID_RATE, TypoGenerator.typoRate("abandoned"), 0.0) // 9
        assertEquals(TypoGenerator.MID_RATE, TypoGenerator.typoRate("consequence"), 0.0) // 11
        assertEquals(TypoGenerator.MID_RATE, TypoGenerator.typoRate("environmental"), 0.0) // 13
        assertEquals(TypoGenerator.SHORT_OR_LONG_RATE, TypoGenerator.typoRate("responsibility"), 0.0) // 14
    }

    @Test
    fun generateUsuallyProducesDifferentWord() {
        val generator = TypoGenerator(Random(20260803))
        val words = listOf("permanent", "definite", "recruit", "survive", "apple", "environmental")
        var differentCount = 0
        repeat(40) {
            for (word in words) {
                if (generator.generate(word) != word) differentCount++
            }
        }
        // 假词必须几乎总是与原词不同，否则干扰项会等于正确答案
        assertTrue("生成结果与原词相同的比例过高: $differentCount/240", differentCount >= 230)
    }

    @Test
    fun generateKeepsSingleCharWordAsIs() {
        assertEquals("a", TypoGenerator(Random(1)).generate("a"))
    }

    @Test
    fun generateChangesWordWithoutVowels() {
        val generator = TypoGenerator(Random(7))
        // 无元音词会退化为相邻字符交换
        assertNotEquals("fly", generator.generate("fly"))
        assertNotEquals("rhythm", generator.generate("rhythm"))
    }

    @Test
    fun shouldGenerateFollowsRateDistribution() {
        val generator = TypoGenerator(Random(99))
        var shortHits = 0
        var midHits = 0
        repeat(2000) {
            if (generator.shouldGenerate("apple")) shortHits++
            if (generator.shouldGenerate("abandoned")) midHits++
        }
        // 粗粒度区间断言：0.6 与 0.3 应当明显分开
        assertTrue("短词命中率异常: $shortHits/2000", shortHits in 1000..1400)
        assertTrue("中长词命中率异常: $midHits/2000", midHits in 450..750)
    }
}
