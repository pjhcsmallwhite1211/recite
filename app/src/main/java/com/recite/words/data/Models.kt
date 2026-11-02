package com.recite.words.data

import kotlinx.serialization.Serializable

/**
 * 词条：一个英文单词及其中文释义。
 *
 * 字段名刻意与参考项目 `code_20260803.html` 的存储结构保持一致，
 * 这样网页版导出的备份 JSON 可以被 App 直接导入。
 */
@Serializable
data class WordItem(
    /** 英文单词，例如 `survive`。 */
    val word: String,
    /** 中文释义，可含词性前缀，例如 `v.幸存；存活`。 */
    val cn: String,
)

/**
 * 单词本：一组词条加上一个稳定 id。
 *
 * @property bookId 稳定标识，创建时生成，不随重命名变化。
 * @property bookName 展示名称。
 * @property wordItems 词条列表，顺序即展示顺序。
 */
@Serializable
data class WordBook(
    val bookId: String,
    val bookName: String,
    val wordItems: List<WordItem> = emptyList(),
)

/**
 * App 的全部可持久化状态。
 *
 * 与网页原型（`code_20260803.html` / `test.html`）的存储结构保持一致，双向可互相导入导出。
 *
 * @property wrongWordMap 键为 `bookId`，值为该本子中被标记为错词的单词。
 *   **结构与网页版一致，不得改动** —— 改了网页版备份就无法导入。
 * @property wrongCountMap 错词的「被答错次数」：`bookId -> (word -> 次数)`。
 *
 *   这是**新增字段**，而不是改动 [wrongWordMap]：老版本数据与网页版备份里都没有它，
 *   反序列化时会回落到空 Map，读次数时再回落到 [DEFAULT_WRONG_COUNT]。
 *   因此老数据导入后每个错词直接显示「1 次」，不需要任何迁移脚本。
 */
@Serializable
data class ReciteData(
    val wordBookList: List<WordBook> = emptyList(),
    val currentBookId: String = "",
    val wrongWordMap: Map<String, List<String>> = emptyMap(),
    val wrongCountMap: Map<String, Map<String, Int>> = emptyMap(),
) {
    /** 当前选中的单词本；id 失效时回退到第一个本子。 */
    fun currentBook(): WordBook? =
        wordBookList.firstOrNull { it.bookId == currentBookId } ?: wordBookList.firstOrNull()

    /** 指定本子的错词集合。 */
    fun wrongWordsOf(bookId: String): Set<String> = wrongWordMap[bookId]?.toSet().orEmpty()

    /**
     * 某个错词被答错的次数。
     *
     * 没有计数记录（老数据、网页版导入、或刚手动标记还没答错）时返回 [DEFAULT_WRONG_COUNT]。
     */
    fun wrongCountOf(bookId: String, word: String): Int =
        wrongCountMap[bookId]?.get(word) ?: DEFAULT_WRONG_COUNT

    /** 指定本子的错词集合及各自次数，供界面直接渲染。 */
    fun wrongWordCountsOf(bookId: String): Map<String, Int> =
        wrongWordsOf(bookId).associateWith { wrongCountOf(bookId, it) }

    companion object {
        /** 错词次数的默认值（也是老数据与网页版导入后的初始值）。 */
        const val DEFAULT_WRONG_COUNT = 1
    }
}
