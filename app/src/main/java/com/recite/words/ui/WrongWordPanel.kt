package com.recite.words.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
 * 错词库面板，两个界面共用。
 *
 * 对齐参考项目两个 HTML 的 `.wrong-wrap`：标题带计数、可切换「只刷错词」、
 * 可清空全部；错词以 chip 形式列出，点击跳到对应词/题。
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "错词库 (${wrongWords.size})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = onToggleMode) {
                Text(
                    text = if (onlyWrongMode) "返回全部" else "只刷错词",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClearAll) {
                Text("清空全部", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (wrongWords.isEmpty()) {
            Text(
                text = "暂无标记的错词",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        } else {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                wrongWords.forEach { word ->
                    AssistChip(
                        onClick = { onJumpTo(word) },
                        label = {
                            Text(word, style = MaterialTheme.typography.labelMedium)
                        },
                    )
                }
            }
        }
    }
}
