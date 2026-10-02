package com.recite.words

import com.recite.words.data.ReciteData
import com.recite.words.data.ReciteDataStore
import com.recite.words.data.ReciteRepository
import com.recite.words.data.WordBook
import com.recite.words.data.WordItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** [ReciteRepository] 的单元测试：覆盖参考项目两个 HTML 共有的全部数据操作。 */
class ReciteRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    /** 每个测试用独立目录 + 确定的 id 序列，保证可复现。 */
    private fun newRepository(): ReciteRepository {
        val file = tempFolder.newFolder().resolve(ReciteDataStore.FILE_NAME)
        var sequence = 0
        return ReciteRepository(ReciteDataStore(file)) {
            sequence += 1
            "book_test_$sequence"
        }
    }

    @Test
    fun loadsDefaultDataOnFirstRun() {
        val repository = newRepository()
        val data = repository.data.value
        assertEquals(1, data.wordBookList.size)
        assertEquals("基础词本", data.wordBookList[0].bookName)
        assertEquals("book_default", data.currentBookId)
        assertEquals(6, data.wordBookList[0].wordItems.size)
    }

    @Test
    fun createBookAddsAndSwitchesCurrentBook() {
        val repository = newRepository()
        val newId = repository.createBook("四级核心")

        val data = repository.data.value
        assertEquals(2, data.wordBookList.size)
        assertEquals(newId, data.currentBookId)
        assertEquals("四级核心", repository.currentBook()?.bookName)
        assertTrue(data.wrongWordMap.containsKey(newId))
    }

    @Test
    fun deleteBookRefusesToRemoveLastBook() {
        val repository = newRepository()
        assertFalse(repository.deleteBook("book_default"))
        assertEquals(1, repository.data.value.wordBookList.size)
    }

    @Test
    fun deleteBookRemovesWordsAndWrongMap() {
        val repository = newRepository()
        val newId = repository.createBook("临时本")
        repository.markWrongWord("apple")

        assertTrue(repository.deleteBook(newId))

        val data = repository.data.value
        assertEquals(1, data.wordBookList.size)
        assertEquals("book_default", data.currentBookId)
        assertFalse(data.wrongWordMap.containsKey(newId))
    }

    @Test
    fun importWordsAppendsAndReportsSkippedLines() {
        val repository = newRepository()
        val result = repository.importWords("apple-n.苹果\nbanana-n.香蕉\n坏行没有分隔符")

        assertEquals(2, result.items.size)
        assertTrue(result.hasFailure)
        assertEquals(3, result.skippedLines.single().lineNumber)
        assertEquals(8, repository.currentBook()?.wordItems?.size)
    }

    @Test
    fun addWordRejectsBlankFields() {
        val repository = newRepository()
        assertFalse(repository.addWord("  ", "n.空"))
        assertFalse(repository.addWord("empty", "   "))
        assertTrue(repository.addWord("zebra", "n.斑马"))
        assertEquals(7, repository.currentBook()?.wordItems?.size)
    }

    @Test
    fun updateWordKeepsWrongWordInSync() {
        val repository = newRepository()
        repository.markWrongWord("apple")

        val index = repository.currentBook()!!.wordItems.indexOfFirst { it.word == "apple" }
        assertTrue(repository.updateWord(index, "apples", "n.苹果（复数）"))

        val data = repository.data.value
        assertEquals("apples", data.currentBook()!!.wordItems[index].word)
        assertFalse("旧拼写不应留在错词库", data.wrongWordsOf("book_default").contains("apple"))
        assertTrue(data.wrongWordsOf("book_default").contains("apples"))
    }

    @Test
    fun deleteWordAlsoRemovesItFromWrongWords() {
        val repository = newRepository()
        repository.markWrongWord("banana")
        val index = repository.currentBook()!!.wordItems.indexOfFirst { it.word == "banana" }

        assertTrue(repository.deleteWord(index))

        val data = repository.data.value
        assertEquals(5, data.currentBook()!!.wordItems.size)
        assertFalse(data.wrongWordsOf("book_default").contains("banana"))
    }

    @Test
    fun toggleAndClearWrongWords() {
        val repository = newRepository()
        repository.toggleWrongWord("apple")
        assertTrue(repository.data.value.wrongWordsOf("book_default").contains("apple"))

        repository.toggleWrongWord("apple")
        assertFalse(repository.data.value.wrongWordsOf("book_default").contains("apple"))

        repository.markWrongWord("apple")
        repository.markWrongWord("banana")
        repository.clearWrongWords()
        assertTrue(repository.data.value.wrongWordsOf("book_default").isEmpty())
    }

    @Test
    fun markWrongWordIsIdempotent() {
        val repository = newRepository()
        repeat(3) { repository.markWrongWord("apple") }
        assertEquals(setOf("apple"), repository.data.value.wrongWordsOf("book_default"))
    }

    @Test
    fun replaceAllFixesInvalidCurrentBookId() {
        val repository = newRepository()
        repository.replaceAll(
            ReciteData(
                wordBookList = listOf(WordBook("only", "唯一本", listOf(WordItem("cat", "n.猫")))),
                currentBookId = "not_exist",
                wrongWordMap = emptyMap(),
            ),
        )
        assertEquals("only", repository.data.value.currentBookId)
    }

    @Test
    fun backupRoundTripsThroughJson() {
        val repository = newRepository()
        repository.createBook("备份本")
        repository.markWrongWord("apple")
        val json = repository.backupJson()

        val restored = repository.parseBackup(json)

        assertEquals(repository.data.value.wordBookList, restored.wordBookList)
        assertEquals(repository.data.value.wrongWordMap, restored.wrongWordMap)
        assertNotNull(restored.currentBook())
    }
}
