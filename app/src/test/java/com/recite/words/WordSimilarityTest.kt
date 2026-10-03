package com.recite.words

import com.recite.words.domain.WordSimilarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** [WordSimilarity] 的单元测试。 */
class WordSimilarityTest {

    @Test
    fun identicalWordsHaveZeroDistance() {
        assertEquals(0, WordSimilarity.editDistance("apple", "apple"))
        assertEquals(1.0, WordSimilarity.similarity("apple", "apple"), 1e-9)
    }

    @Test
    fun distanceCountsSingleEdits() {
        assertEquals(1, WordSimilarity.editDistance("apple", "appli"))   // 替换
        assertEquals(1, WordSimilarity.editDistance("apple", "apples"))  // 插入
        assertEquals(1, WordSimilarity.editDistance("apple", "aple"))    // 删除
        assertEquals(2, WordSimilarity.editDistance("apple", "app"))
    }

    @Test
    fun distanceIsCaseInsensitive() {
        assertEquals(0, WordSimilarity.editDistance("Apple", "apple"))
    }

    @Test
    fun distanceHandlesEmptyInput() {
        assertEquals(3, WordSimilarity.editDistance("", "cat"))
        assertEquals(3, WordSimilarity.editDistance("cat", ""))
        assertEquals(0, WordSimilarity.editDistance("", ""))
    }

    @Test
    fun similarityStaysWithinZeroAndOne() {
        for (pair in listOf("cat" to "st", "apple" to "imle", "a" to "zebra", "" to "")) {
            val value = WordSimilarity.similarity(pair.first, pair.second)
            assertTrue("相似度越界: ${pair.first}/${pair.second} = $value", value in 0.0..1.0)
        }
    }

    @Test
    fun rejectsTheTwoBadTyposFoundInPractice() {
        // 这两个是修正前真实造出来的"假词",应当被判为不像
        assertTrue("'cat'->'st' 应判为不像", WordSimilarity.similarity("st", "cat") < 0.5)
        assertTrue("'apple'->'imle' 应判为不像", WordSimilarity.similarity("imle", "apple") < 0.5)
    }

    @Test
    fun acceptsReasonableTypos() {
        // 修正后的策略应当造出这类词
        assertTrue("'cat'->'stat'", WordSimilarity.similarity("stat", "cat") >= 0.5)
        assertTrue("'apple'->'imple'", WordSimilarity.similarity("imple", "apple") >= 0.5)
        assertTrue("'permanent'->'immanent'", WordSimilarity.similarity("immanent", "permanent") >= 0.5)
    }
}
