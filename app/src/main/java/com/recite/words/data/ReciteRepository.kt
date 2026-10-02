package com.recite.words.data

import com.recite.words.domain.ImportResult
import com.recite.words.domain.WordImportParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全部业务数据的唯一读写入口。
 *
 * 参考项目 `code_20260803.html` 与 `test.html` 的数据操作完全相同
 * （新建 / 切换 / 删除词本、导入词表、错词增删、清空、备份导入导出），
 * 这里收敛成一层，供「卡片背词」与「多选测试」两个界面共用——单一状态源。
 *
 * 纯 Kotlin，不依赖 Android，可直接用临时文件做单元测试。
 */
class ReciteRepository(
    private val store: ReciteDataStore,
    private val idGenerator: () -> String = defaultBookIdGenerator(),
) {

    private val _data = MutableStateFlow(store.load())

    /** 当前全部数据，UI 订阅它。 */
    val data: StateFlow<ReciteData> = _data.asStateFlow()

    /** 当前单词本；无任何词本时返回 null。 */
    fun currentBook(): WordBook? = _data.value.currentBook()

    /** 切换当前单词本。 */
    fun switchBook(bookId: String) {
        mutate { it.copy(currentBookId = bookId) }
    }

    /** 新建单词本并切过去，返回新本子的 id。 */
    fun createBook(name: String): String {
        val bookId = idGenerator()
        mutate {
            it.copy(
                wordBookList = it.wordBookList + WordBook(bookId = bookId, bookName = name),
                currentBookId = bookId,
                wrongWordMap = it.wrongWordMap + (bookId to emptyList<String>()),
            )
        }
        return bookId
    }

    /** 删除单词本；只剩一本时拒绝并返回 false。 */
    fun deleteBook(bookId: String): Boolean {
        val current = _data.value
        if (current.wordBookList.size <= 1) return false
        val remaining = current.wordBookList.filterNot { it.bookId == bookId }
        if (remaining.size == current.wordBookList.size) return false

        mutate {
            it.copy(
                wordBookList = remaining,
                currentBookId = if (it.currentBookId == bookId) {
                    remaining.first().bookId
                } else {
                    it.currentBookId
                },
                wrongWordMap = it.wrongWordMap - bookId,
            )
        }
        return true
    }

    /** 把词表文本导入当前单词本，返回解析结果供 UI 提示。 */
    fun importWords(text: String): ImportResult {
        val result = WordImportParser.parse(text)
        if (result.items.isEmpty()) return result
        mutateCurrentBook { it.copy(wordItems = it.wordItems + result.items) }
        return result
    }

    /** 追加单个词条；单词与释义都不能为空。 */
    fun addWord(word: String, cn: String): Boolean {
        val trimmedWord = word.trim()
        val trimmedCn = cn.trim()
        if (trimmedWord.isEmpty() || trimmedCn.isEmpty()) return false
        mutateCurrentBook { it.copy(wordItems = it.wordItems + WordItem(trimmedWord, trimmedCn)) }
        return true
    }

    /** 修改第 [index] 个词条；同时把错词库里的旧拼写同步为新拼写。 */
    fun updateWord(index: Int, word: String, cn: String): Boolean {
        val trimmedWord = word.trim()
        val trimmedCn = cn.trim()
        if (trimmedWord.isEmpty() || trimmedCn.isEmpty()) return false

        val book = currentBook() ?: return false
        if (index !in book.wordItems.indices) return false
        val previousWord = book.wordItems[index].word

        mutate { data ->
            val target = data.currentBook() ?: return@mutate data
            val items = target.wordItems.toMutableList().also { it[index] = WordItem(trimmedWord, trimmedCn) }
            val wrong = data.wrongWordsOf(target.bookId).toMutableSet().apply {
                if (remove(previousWord)) add(trimmedWord)
            }
            data.copy(
                wordBookList = data.wordBookList.replaceBook(target.copy(wordItems = items)),
                wrongWordMap = data.wrongWordMap + (target.bookId to wrong.toList()),
            )
        }
        return true
    }

    /** 删除第 [index] 个词条，并从错词库移除该词。 */
    fun deleteWord(index: Int): Boolean {
        val book = currentBook() ?: return false
        if (index !in book.wordItems.indices) return false
        val removedWord = book.wordItems[index].word

        mutate { data ->
            val target = data.currentBook() ?: return@mutate data
            val items = target.wordItems.toMutableList().also { it.removeAt(index) }
            val wrong = data.wrongWordsOf(target.bookId) - removedWord
            data.copy(
                wordBookList = data.wordBookList.replaceBook(target.copy(wordItems = items)),
                wrongWordMap = data.wrongWordMap + (target.bookId to wrong.toList()),
            )
        }
        return true
    }

    /** 切换某个词的错词标记。 */
    fun toggleWrongWord(word: String) {
        mutateWrongWords { if (word in it) it - word else it + word }
    }

    /** 把某个词标记为错词（答题答错时调用）。 */
    fun markWrongWord(word: String) {
        mutateWrongWords { it + word }
    }

    /** 清空当前单词本的错词库。 */
    fun clearWrongWords() {
        mutateWrongWords { emptySet() }
    }

    /** 解析备份 JSON 文本；不改动现有数据，交由调用方确认后再 [replaceAll]。 */
    fun parseBackup(text: String): ReciteData = store.decode(text)

    /** 导出为备份 JSON 文本。 */
    fun backupJson(): String = store.encode(_data.value)

    /** 当前词本错词的导出文本：每行 `word-cn`，与参考项目的导出格式一致。 */
    fun currentWrongWordsText(): String {
        val book = _data.value.currentBook() ?: return ""
        val wrongWords = _data.value.wrongWordsOf(book.bookId)
        return book.wordItems
            .filter { it.word in wrongWords }
            .joinToString("\n") { "${it.word}-${it.cn}" }
    }

    /** 用外部备份数据整体替换现有数据，并修正失效的 currentBookId。 */
    fun replaceAll(backup: ReciteData) {
        val validCurrentId = backup.currentBookId
            .takeIf { id -> backup.wordBookList.any { it.bookId == id } }
            ?: backup.wordBookList.firstOrNull()?.bookId.orEmpty()

        val next = backup.copy(currentBookId = validCurrentId)
        _data.value = next
        store.save(next)
    }

    private fun mutate(transform: (ReciteData) -> ReciteData) {
        val next = transform(_data.value)
        _data.value = next
        store.save(next)
    }

    private fun mutateCurrentBook(transform: (WordBook) -> WordBook) {
        mutate { data ->
            val book = data.currentBook() ?: return@mutate data
            data.copy(wordBookList = data.wordBookList.replaceBook(transform(book)))
        }
    }

    private fun mutateWrongWords(transform: (Set<String>) -> Set<String>) {
        mutate { data ->
            val bookId = data.currentBook()?.bookId ?: return@mutate data
            data.copy(
                wrongWordMap = data.wrongWordMap + (bookId to transform(data.wrongWordsOf(bookId)).toList()),
            )
        }
    }

    private fun List<WordBook>.replaceBook(book: WordBook): List<WordBook> =
        map { if (it.bookId == book.bookId) book else it }

    companion object {
        /** 默认的词本 id 生成器：时间戳 + 自增序号，避免同毫秒重复。 */
        fun defaultBookIdGenerator(): () -> String {
            var counter = 0
            return {
                counter += 1
                "book_${System.currentTimeMillis()}_$counter"
            }
        }
    }
}
