package com.recite.words

import com.recite.words.data.ReciteData
import com.recite.words.data.ReciteDataStore
import com.recite.words.data.WordBook
import com.recite.words.data.WordItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** [ReciteDataStore] 的单元测试：落盘往返 + 与网页版备份 JSON 的互通。 */
class ReciteDataStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun loadReturnsDefaultDataWhenFileMissing() {
        val store = ReciteDataStore(tempFolder.newFolder().resolve(ReciteDataStore.FILE_NAME))
        val data = store.load()
        assertEquals(1, data.wordBookList.size)
        assertEquals("基础词本", data.wordBookList[0].bookName)
        assertTrue(data.currentBook() != null)
    }

    @Test
    fun saveThenLoadRoundTrips() {
        val file = tempFolder.newFolder().resolve(ReciteDataStore.FILE_NAME)
        val store = ReciteDataStore(file)
        val original = ReciteData(
            wordBookList = listOf(
                WordBook("book_a", "四级核心", listOf(WordItem("abandon", "v.放弃"))),
                WordBook("book_b", "考研", listOf(WordItem("absolute", "adj.绝对的"))),
            ),
            currentBookId = "book_b",
            wrongWordMap = mapOf("book_a" to listOf("abandon")),
        )

        store.save(original)
        val reloaded = store.load()

        assertEquals(original, reloaded)
        assertEquals("book_b", reloaded.currentBook()?.bookId)
        assertEquals(setOf("abandon"), reloaded.wrongWordsOf("book_a"))
        assertTrue(reloaded.wrongWordsOf("book_b").isEmpty())
    }

    @Test
    fun decodesBackupJsonExportedFromReferenceHtml() {
        val store = ReciteDataStore(tempFolder.newFolder().resolve(ReciteDataStore.FILE_NAME))
        val htmlBackup = """
            {
              "wordBookList": [
                {
                  "bookId": "book_1700000000000",
                  "bookName": "基础词本",
                  "wordItems": [
                    {"word": "survive", "cn": "v.幸存；存活"},
                    {"word": "apple", "cn": "n.苹果"}
                  ]
                }
              ],
              "currentBookId": "book_1700000000000",
              "wrongWordMap": {"book_1700000000000": ["apple"]},
              "exportAt": "2026-08-03T10:00:00.000Z"
            }
        """.trimIndent()

        val data = store.decode(htmlBackup)

        assertEquals(2, data.wordBookList[0].wordItems.size)
        assertEquals("v.幸存；存活", data.wordBookList[0].wordItems[0].cn)
        assertEquals(setOf("apple"), data.wrongWordsOf("book_1700000000000"))
    }

    @Test
    fun loadFallsBackToDefaultWhenJsonCorrupted() {
        val file = tempFolder.newFile(ReciteDataStore.FILE_NAME)
        file.writeText("{ this is not json")
        val store = ReciteDataStore(file)
        assertEquals(1, store.load().wordBookList.size)
    }
}
