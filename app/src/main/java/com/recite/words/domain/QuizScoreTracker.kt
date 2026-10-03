package com.recite.words.domain

/**
 * 一轮测验的计分规则。
 *
 * 对齐参考项目 `test.html` 的 `answeredMap` 行为：**同一题答对只计一次**；
 * 若某题答对过、之后又答错，则撤销它的已答对标记（下次答对可以重新计数）。
 *
 * 抽成纯逻辑是为了能直接单测 —— UI 层只负责把结果显示出来。
 */
class QuizScoreTracker {

    private val correctIndices = mutableSetOf<Int>()

    /**
     * 记录一次「答对」。
     *
     * @return true 表示这是该题首次答对，调用方应把正确数 +1；false 表示重复答对，不计数。
     */
    fun onCorrect(questionIndex: Int): Boolean = correctIndices.add(questionIndex)

    /** 记录一次「答错」；撤销该题的已答对标记。 */
    fun onWrong(questionIndex: Int) {
        correctIndices.remove(questionIndex)
    }

    /** 该题是否已经答对过。 */
    fun isAlreadyCorrect(questionIndex: Int): Boolean = questionIndex in correctIndices

    /** 清空本轮记录。 */
    fun reset() {
        correctIndices.clear()
    }
}
