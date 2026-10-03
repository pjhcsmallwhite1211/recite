package com.recite.words.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.recite.words.data.ReciteData
import com.recite.words.data.WordItem
import com.recite.words.speech.WordSpeaker
import com.recite.words.ui.theme.DangerRed
import com.recite.words.ui.theme.DangerRedActive

/**
 * 卡片背词界面，对齐参考项目 `code_20260803.html`：
 * 点击卡片翻面 → 上一个 / 下一个循环 → 错词按钮状态随当前词联动 →
 * 切换词自动朗读 → 底部错词库面板。
 */
@Composable
fun CardScreen(
    data: ReciteData,
    viewModel: ReciteViewModel,
    speaker: WordSpeaker,
    modifier: Modifier = Modifier,
) {
    val book = data.currentBook()
    val wrongWords = data.wrongWordsOf(book?.bookId.orEmpty())

    var onlyWrongMode by remember { mutableStateOf(false) }
    var index by remember { mutableIntStateOf(0) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // 「只刷错词」改变待刷词表；词表本身随数据变化自动重建
    val wordList = remember(book, onlyWrongMode, wrongWords) {
        val items = book?.wordItems.orEmpty()
        if (onlyWrongMode) items.filter { it.word in wrongWords } else items
    }

    // 切换词本 / 模式 / 增删词之后回到第一张
    LaunchedEffect(wordList) { index = 0 }

    val safeIndex = if (wordList.isEmpty()) 0 else index.coerceIn(0, wordList.lastIndex)
    val currentItem = wordList.getOrNull(safeIndex)
    val isCurrentWrong = currentItem != null && currentItem.word in wrongWords

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = if (wordList.isEmpty()) "进度: 0 / 0" else "进度: ${safeIndex + 1} / ${wordList.size}",
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (currentItem == null) {
                EmptyHint(
                    title = if (onlyWrongMode) "当前词本没有错词" else "本子暂无单词",
                    hint = if (onlyWrongMode) "切回全部单词继续背" else "打开设置导入词表",
                )
            } else {
                // 切换单词时自动朗读，与参考项目一致
                LaunchedEffect(currentItem) { speaker.speak(currentItem.word) }
                FlipCard(item = currentItem, modifier = Modifier.fillMaxWidth())
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
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
            ) { Text("⬅️ 上一个", maxLines = 1) }

            Button(
                onClick = { currentItem?.let { viewModel.recite.toggleWrongWord(it.word) } },
                modifier = Modifier.weight(1.2f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrentWrong) DangerRedActive else DangerRed,
                ),
            ) { Text(if (isCurrentWrong) "✅ 已标记错词" else "❌ 错词", maxLines = 1) }

            OutlinedButton(
                onClick = { currentItem?.let { speaker.speak(it.word) } },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            ) { Text("🔊", maxLines = 1) }

            Button(
                onClick = { if (wordList.isNotEmpty()) index = (safeIndex + 1) % wordList.size },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
            ) { Text("下一个 ➡️", maxLines = 1) }
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
                viewModel.showMessage("已清空错词库")
            },
            onDismiss = { showClearConfirm = false },
        )
    }
}

/** 可点击翻面的单词卡片。 */
@Composable
private fun FlipCard(item: WordItem, modifier: Modifier = Modifier) {
    var flipped by remember(item) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        label = "cardFlip",
    )

    Card(
        modifier = modifier
            .height(200.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { flipped = !flipped },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (rotation <= 90f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = item.word,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "(点击卡片查看释义)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                // 背面再翻转 180°，避免正文字被镜像
                Box(
                    modifier = Modifier.graphicsLayer { rotationY = 180f }.padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = item.cn,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
