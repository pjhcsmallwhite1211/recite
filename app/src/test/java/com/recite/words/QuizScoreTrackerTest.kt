package com.recite.words

import com.recite.words.domain.QuizScoreTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [QuizScoreTracker] 的单元测试。
 *
 * 这组用例直接锁住「同一题答对不重复计数」的行为 —— 之前 UI 里没有去重，
 * 导致进度显示 2/6 而正确数却到 7。
 */
class QuizScoreTrackerTest {

    @Test
    fun firstCorrectOnAQuestionCounts() {
        val tracker = QuizScoreTracker()
        assertTrue("首次答对应计数", tracker.onCorrect(0))
        assertTrue(tracker.isAlreadyCorrect(0))
    }

    @Test
    fun repeatedCorrectOnSameQuestionDoesNotCountAgain() {
        val tracker = QuizScoreTracker()
        assertTrue(tracker.onCorrect(3))
        assertFalse("同一题再次答对不应重复计数", tracker.onCorrect(3))
        assertFalse(tracker.onCorrect(3))
    }

    @Test
    fun differentQuestionsCountIndependently() {
        val tracker = QuizScoreTracker()
        assertTrue(tracker.onCorrect(0))
        assertTrue(tracker.onCorrect(1))
        assertFalse(tracker.onCorrect(1))
        assertTrue(tracker.onCorrect(2))
    }

    @Test
    fun wrongAfterCorrectRevokesTheMark() {
        val tracker = QuizScoreTracker()
        assertTrue(tracker.onCorrect(5))
        tracker.onWrong(5)
        assertFalse("答错后该题不再算已答对", tracker.isAlreadyCorrect(5))
        assertTrue("撤销后再答对可以重新计数", tracker.onCorrect(5))
    }

    @Test
    fun wrongOnUnansweredQuestionIsNoop() {
        val tracker = QuizScoreTracker()
        tracker.onWrong(9)
        assertFalse(tracker.isAlreadyCorrect(9))
    }

    @Test
    fun resetClearsEverything() {
        val tracker = QuizScoreTracker()
        tracker.onCorrect(0)
        tracker.onCorrect(1)
        tracker.reset()
        assertFalse(tracker.isAlreadyCorrect(0))
        assertTrue("重置后同一题可以重新计数", tracker.onCorrect(0))
    }

    @Test
    fun simulatesTheReportedBugScenario() {
        // 复现「进度 2/6 却显示正确 7」的路径:反复答对同一题
        val tracker = QuizScoreTracker()
        var rightCount = 0
        repeat(7) {
            if (tracker.onCorrect(1)) rightCount += 1
        }
        assertEquals("同一题答对 7 次只应计 1 分", 1, rightCount)
    }
}
