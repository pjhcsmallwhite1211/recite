package com.recite.words.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * 单词朗读封装，替代参考项目的 `window.speechSynthesis`。
 *
 * 使用系统 TTS 引擎，美式英语、语速 [SPEECH_RATE]（与参考项目一致）。
 * 引擎初始化是异步的：初始化完成前的朗读请求会被安静地忽略，
 * 因此语言与语速在首次朗读时才配置，避免构造期间的竞态。
 */
class WordSpeaker(context: Context) : AutoCloseable {

    private var engine: TextToSpeech? = null
    private var isReady = false
    private var isConfigured = false

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            isReady = status == TextToSpeech.SUCCESS
        }
    }

    /** 朗读一个单词或短语；空串忽略。 */
    fun speak(text: String) {
        val tts = engine ?: return
        if (!isReady || text.isBlank()) return

        if (!isConfigured) {
            tts.language = Locale.US
            tts.setSpeechRate(SPEECH_RATE)
            isConfigured = true
        }

        tts.stop()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    /** 释放引擎；必须在界面销毁时调用。 */
    override fun close() {
        isReady = false
        engine?.stop()
        engine?.shutdown()
        engine = null
    }

    private companion object {
        const val SPEECH_RATE = 0.9f
        const val UTTERANCE_ID = "recite-word"
    }
}
