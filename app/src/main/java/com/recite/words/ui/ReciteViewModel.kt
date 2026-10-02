package com.recite.words.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.recite.words.data.ReciteData
import com.recite.words.data.ReciteDataStore
import com.recite.words.data.ReciteRepository
import com.recite.words.domain.QuizMode
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/** 顶层标签页。 */
enum class AppTab(val label: String) {
    /** 卡片式浏览背词。 */
    CARD("卡片背词"),

    /** 四选一测验。 */
    QUIZ("多选测试"),
}

/**
 * 全应用唯一的状态持有者。
 *
 * 两个标签页共享同一个 [ReciteRepository]：词本切换、错词标记、词表导入在一处生效、
 * 两处立即可见——等价于参考项目两个 HTML 共用同一份 localStorage 数据。
 */
class ReciteViewModel(application: Application) : AndroidViewModel(application) {

    /** 数据层入口，UI 直接调用其操作（导入、标记错词、备份等）。 */
    val recite: ReciteRepository = ReciteRepository(
        ReciteDataStore(File(application.filesDir, ReciteDataStore.FILE_NAME)),
    )

    /** 全部持久化数据。 */
    val data: StateFlow<ReciteData> = recite.data

    /** 当前标签页。 */
    var currentTab by mutableStateOf(AppTab.CARD)

    /** 多选题的出题方向。 */
    var quizMode by mutableStateOf(QuizMode.EN_TO_CN)

    /** 一次性提示文本；由 UI 展示后调用 [clearMessage] 置空。 */
    var message by mutableStateOf<String?>(null)

    /** 显示一条提示。 */
    fun showMessage(text: String) {
        message = text
    }

    /** 消费掉当前提示。 */
    fun clearMessage() {
        message = null
    }
}
