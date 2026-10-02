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
 * 结构与参考项目网页版的 `localStorage` / 备份 JSON 完全一致：
 * `wordBookList` + `currentBookId` + `wrongWordMap`。
 *
 * @property wrongWordMap 键为 `bookId`，值为该本子中被标记为错词的单词。
 */
@Serializable
data class ReciteData(
    val wordBookList: List<WordBook> = emptyList(),
    val currentBookId: String = "",
    val wrongWordMap: Map<String, List<String>> = emptyMap(),
) {
    /** 当前选中的单词本；id 失效时回退到第一个本子。 */
    fun currentBook(): WordBook? =
        wordBookList.firstOrNull { it.bookId == currentBookId } ?: wordBookList.firstOrNull()

    /** 指定本子的错词集合。 */
    fun wrongWordsOf(bookId: String): Set<String> = wrongWordMap[bookId]?.toSet().orEmpty()
}
