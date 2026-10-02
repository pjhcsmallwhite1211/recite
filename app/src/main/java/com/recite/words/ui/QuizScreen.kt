package com.recite.words.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.recite.words.data.ReciteData
import com.recite.words.domain.QuizMode
import com.recite.words.domain.QuizOption
import com.recite.words.domain.QuizOptionBuilder
import com.recite.words.domain.WordPool
import com.recite.words.speech.WordSpeaker
import com.recite.words.ui.theme.DangerRed
import com.recite.words.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

/** 答对后自动进入下一题的延时（对齐参考项目 `AUTO_NEXT_DELAY`）。 */
private const val AUTO_NEXT_DELAY_MS = 1200L

/**
 * 多选测试界面，对齐参考项目 `test.html`：
 * 两种出题方向、四选一、答错自动进错词库、答对延时自动下一题、
 * 「该词曾经答错」提示、底部错词库面板。
 */
@Composable
fun QuizScreen(
    data: ReciteData,
    viewModel: ReciteViewModel,
    speaker: WordSpeaker,
    modifier: Modifier = Modifier,
) {
    val book = data.currentBook()
    val wrongWords = data.wrongWordsOf(book?.bookId.orEmpty())
    val mode = viewModel.quizMode

    var onlyWrongMode by remember { mutableStateOf(false) }
    var index by remember { mutableIntStateOf(0) }
    var rightCount by remember { mutableIntStateOf(0) }
    var answerState by remember { mutableStateOf<Boolean?>(null) }
    var pickedText by remember { mutableStateOf<String?>(null) }
    var optionsSeed by remember { mutableIntStateOf(0) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val wordList = remember(book, onlyWrongMode, wrongWords) {
        val items = book?.wordItems.orEmpty()
        if (onlyWrongMode) items.filter { it.word in wrongWords } else items
    }

    // 干扰项词池跨全部词本，与参考项目 buildGlobalPool 一致
    val pool = remember(data.wordBookList) { WordPool.from(data.wordBookList) }
    val optionBuilder = remember { QuizOptionBuilder() }

    /** 回到第一题并清空本轮成绩。 */
    fun resetQuiz() {
        index = 0
        rightCount = 0
        answerState = null
        pickedText = null
        optionsSeed += 1
    }

    LaunchedEffect(wordList) { resetQuiz() }

    val safeIndex = if (wordList.isEmpty()) 0 else index.coerceIn(0, wordList.lastIndex)
    val currentItem = wordList.getOrNull(safeIndex)
    val isCurrentWrong = currentItem != null && currentItem.word in wrongWords

    val options = remember(currentItem, mode, optionsSeed) {
        currentItem?.let { optionBuilder.buildOptions(correct = it, mode = mode, pool = pool) }.orEmpty()
    }

    // 「看英文选中文」出题时直接朗读；「看中文选英文」在作答后才朗读（对齐参考项目）
    LaunchedEffect(currentItem, mode) {
        if (mode == QuizMode.EN_TO_CN && currentItem != null) speaker.speak(currentItem.word)
    }

    // 答对后延时自动下一题；答错停在原题，让用户看清正确答案
    LaunchedEffect(answerState) {
        if (answerState == true) {
            delay(AUTO_NEXT_DELAY_MS)
            if (wordList.isNotEmpty()) index = (safeIndex + 1) % wordList.size
            answerState = null
            pickedText = null
            optionsSeed += 1
        }
    }

    fun selectOption(option: QuizOption) {
        val item = currentItem ?: return
        if (answerState != null) return

        pickedText = option.text
        if (option.isRight) {
            answerState = true
            rightCount += 1
        } else {
            answerState = false
            viewModel.recite.markWrongWord(item.word)
        }
        if (mode == QuizMode.CN_TO_EN) speaker.speak(item.word)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = mode == QuizMode.EN_TO_CN,
                onClick = {
                    viewModel.quizMode = QuizMode.EN_TO_CN
                    resetQuiz()
                },
                label = { Text("看英文选中文") },
            )
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = mode == QuizMode.CN_TO_EN,
                onClick = {
                    viewModel.quizMode = QuizMode.CN_TO_EN
                    resetQuiz()
                },
                label = { Text("看中文选英文") },
            )
        }

        Text(
            text = if (wordList.isEmpty()) {
                "进度: 0/0 | 正确:0"
            } else {
                "进度: ${safeIndex + 1}/${wordList.size} | 正确:$rightCount"
            },
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (currentItem == null) {
                EmptyHint(
                    title = if (onlyWrongMode) "当前词本没有错词" else "本子暂无单词",
                    hint = if (onlyWrongMode) "切回全部单词继续练" else "打开设置导入词表",
                )
            } else {
                QuestionCard(
                    prompt = mode.promptOf(currentItem),
                    tip = if (mode == QuizMode.EN_TO_CN) "请选择正确中文释义" else "请选择正确英文单词",
                    isWrongBefore = isCurrentWrong,
                    options = options,
                    answerState = answerState,
                    pickedText = pickedText,
                    correctAnswer = mode.answerOf(currentItem),
                    onSpeak = { speaker.speak(currentItem.word) },
                    onSelect = ::selectOption,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { if (wordList.isNotEmpty()) index = (safeIndex - 1 + wordList.size) % wordList.size },
                modifier = Modifier.weight(1f),
            ) { Text("⬅️ 上一题") }

            OutlinedButton(onClick = { resetQuiz() }, modifier = Modifier.weight(1f)) { Text("🔄 重置") }

            Button(
                onClick = {
                    if (wordList.isNotEmpty()) index = (safeIndex + 1) % wordList.size
                    answerState = null
                    pickedText = null
                    optionsSeed += 1
                },
                modifier = Modifier.weight(1f),
            ) { Text("下一题 ➡️") }
        }

        WrongWordPanel(
            wrongWords = wrongWords,
            onlyWrongMode = onlyWrongMode,
            onToggleMode = { onlyWrongMode = !onlyWrongMode },
            onClearAll = {
                if (wrongWords.isEmpty()) {
                    viewModel.showMessage("当前本子错词库为空")
                } else {
                    showClearConfirm = true
                }
            },
            onJumpTo = { word ->
                val target = wordList.indexOfFirst { it.word == word }
                if (target >= 0) {
                    index = target
                    answerState = null
                    pickedText = null
                    optionsSeed += 1
                } else {
                    viewModel.showMessage("当前本子未包含单词：$word")
                }
            },
        )
    }

    if (showClearConfirm) {
        ClearWrongConfirmDialog(
            onConfirm = {
                viewModel.recite.clearWrongWords()
                showClearConfirm = false
                resetQuiz()
                viewModel.showMessage("已清空错词库")
            },
            onDismiss = { showClearConfirm = false },
        )
    }
}

/** 单道题的卡片：题干、提示、朗读按钮、四个选项与作答反馈。 */
@Composable
private fun QuestionCard(
    prompt: String,
    tip: String,
    isWrongBefore: Boolean,
    options: List<QuizOption>,
    answerState: Boolean?,
    pickedText: String?,
    correctAnswer: String,
    onSpeak: () -> Unit,
    onSelect: (QuizOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = prompt,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = tip,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isWrongBefore) {
                Text(
                    text = "⚠️该词曾经答错",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = DangerRed,
                )
            }

            OutlinedButton(
                onClick = onSpeak,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 6.dp),
            ) { Text("🔊 朗读") }

            options.forEach { option ->
                val revealed = answerState != null
                val containerColor = when {
                    revealed && option.isRight -> SuccessGreen
                    answerState == false && option.text == pickedText -> DangerRed
                    else -> MaterialTheme.colorScheme.surface
                }
                val contentColor = if (containerColor == MaterialTheme.colorScheme.surface) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    Color.White
                }
                val shape = RoundedCornerShape(9.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(shape)
                        .background(containerColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                        .clickable(enabled = !revealed) { onSelect(option) }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option.text,
                        textAlign = TextAlign.Center,
                        color = contentColor,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Text(
                text = when (answerState) {
                    true -> "✅回答正确"
                    false -> "❌错误，正确答案：$correctAnswer"
                    null -> ""
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = when (answerState) {
                    true -> SuccessGreen
                    false -> DangerRed
                    null -> Color.Transparent
                },
            )
        }
    }
}
