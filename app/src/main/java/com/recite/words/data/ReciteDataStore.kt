package com.recite.words.data

import kotlinx.serialization.json.Json
import java.io.File

/**
 * [ReciteData] 的 JSON 文件持久化。
 *
 * 选型说明：V1 数据量在数千词量级，单文件全量读写足够快；而且文件格式与参考项目
 * 网页版的备份 JSON 完全一致，两边数据可以直接互相导入导出。
 *
 * 刻意只依赖 [File] 而不依赖 Android `Context`，以便在 JVM 单元测试中直接验证。
 */
class ReciteDataStore(private val file: File) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** 读取数据；文件缺失或内容损坏时回退到 [defaultData]。 */
    fun load(): ReciteData {
        if (!file.exists()) return defaultData()
        return runCatching { decode(file.readText()) }.getOrElse { defaultData() }
    }

    /** 覆盖写入数据（UTF-8，无 BOM）。 */
    fun save(data: ReciteData) {
        file.parentFile?.mkdirs()
        file.writeText(encode(data))
    }

    /** 序列化成备份 JSON 文本。 */
    fun encode(data: ReciteData): String = json.encodeToString(ReciteData.serializer(), data)

    /** 解析备份 JSON 文本；格式非法时抛异常，由调用方提示用户。 */
    fun decode(text: String): ReciteData = json.decodeFromString(ReciteData.serializer(), text)

    companion object {
        /** 内部存储中的文件名。 */
        const val FILE_NAME = "recite-data.json"

        /** 首次启动时的示例词本，取自参考项目的默认数据。 */
        fun defaultData(): ReciteData {
            val bookId = "book_default"
            return ReciteData(
                wordBookList = listOf(
                    WordBook(
                        bookId = bookId,
                        bookName = "基础词本",
                        wordItems = listOf(
                            WordItem("survive", "v.幸存；存活"),
                            WordItem("permanent", "adj.永久的"),
                            WordItem("definite", "adj.明确的"),
                            WordItem("recruit", "v.招募；招聘"),
                            WordItem("apple", "n.苹果"),
                            WordItem("banana", "n.香蕉"),
                        ),
                    ),
                ),
                currentBookId = bookId,
                wrongWordMap = mapOf(bookId to emptyList()),
            )
        }
    }
}
