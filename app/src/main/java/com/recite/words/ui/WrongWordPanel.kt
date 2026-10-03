package com.recite.words.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 错词列表的最大高度：约两行 chip。
 *
 * 超过之后列表**内部滚动**，而不是把整个面板撑高 —— 否则错词一多，
 * 上方的卡片 / 题目卡片会被挤没（参考原型用 `max-height + overflow-y` 解决同一问题）。
 */
private val WrongListMaxHeight = 64.dp

/** 标题行按钮的高度，压低以让整个面板更紧凑。 */
private val HeaderButtonHeight = 30.dp

/**
 * 错词库面板，两个界面共用。
 *
 * 对齐参考项目两个 HTML 的 `.wrong-wrap`：标题带计数、可切换「只刷错词」、
 * 可清空全部；错词以 chip 形式列出，点击跳到对应词/题。
 *
 * 布局纪律：整个面板高度**有上限**（标题行 + 最多两行 chip），
 * 超出部分在列表内部滚动，保证上方主内容永远不被挤压。
 *
 * @param wrongWords 当前词本的错词集合。
 * @param onlyWrongMode 当前是否处于「只刷错词」模式。
 * @param onJumpTo 点击某个错词时回调。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WrongWordPanel(
    wrongWords: Set<String>,
    onlyWrongMode: Boolean,
    onToggleMode: () -> Unit,
    onClearAll: () -> Unit,
    onJumpTo: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalDivider()

        // ---------- 标题行 ----------
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "错词库 (${wrongWords.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            TextButton(
                onClick = onToggleMode,
                modifier = Modifier.height(HeaderButtonHeight),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Text(
                    text = if (onlyWrongMode) "返回全部" else "只刷错词",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = onClearAll,
                modifier = Modifier.height(HeaderButtonHeight),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Text("清空全部", style = MaterialTheme.typography.labelMedium)
            }
        }

        // ---------- 错词列表 ----------
        if (wrongWords.isEmpty()) {
            Text(
                text = "暂无标记的错词",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
            )
        } else {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    // 先限高(外层),再让内容可滚动(内层) —— 顺序不能反
                    .heightIn(max = WrongListMaxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                wrongWords.forEach { word ->
                    AssistChip(
                        onClick = { onJumpTo(word) },
                        modifier = Modifier.height(28.dp),
                        label = {
                            Text(
                                text = word,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
