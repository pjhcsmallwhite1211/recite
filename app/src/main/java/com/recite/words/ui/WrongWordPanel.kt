package com.recite.words.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 错词列表的默认高度（约两行 chip）。 */
private val WrongListDefaultHeight = 64.dp

/** 收起时的最小高度：0 表示列表完全收起，只留拖动手柄与标题行。 */
private const val WrongListMinHeightDp = 0f

/** 展开上限：屏幕高度的这个比例，避免把主内容压到看不见。 */
private const val WrongListMaxScreenRatio = 0.55f

/** 标题行按钮高度，压低以让整个面板更紧凑。 */
private val HeaderButtonHeight = 30.dp

/** 拖动手柄区域的高度（够手指按住即可）。 */
private val DragHandleHeight = 20.dp

/**
 * 错词库面板，两个界面共用。
 *
 * 对齐参考项目两个 HTML 的 `.wrong-wrap`：标题带计数、可切换「只刷错词」、
 * 可清空全部；错词以 chip 形式列出，点击跳到对应词/题。
 *
 * **高度是可拖的**：顶部有一条拖动手柄，向上拖展开、向下拖收起，
 * 高度记录在 [rememberSaveable] 里（转屏不丢）。这是为了兼顾两件事——
 * 错词多时能展开逐个查看，错词少或想专心看卡片时能收起，把空间让给主内容。
 * 展开上限为屏幕高度的 [WrongListMaxScreenRatio]，保证主内容永远留着可见区域。
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
    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.toFloat()
    val maxHeightDp = screenHeightDp * WrongListMaxScreenRatio

    // 列表高度(dp)，可拖动调整；转屏/进程重建后仍保留
    var listHeightDp by rememberSaveable { mutableFloatStateOf(WrongListDefaultHeight.value) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalDivider()

        // ---------- 拖动手柄 ----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(DragHandleHeight)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { deltaPx ->
                        // delta 向下为正：向下拖 => 变小
                        listHeightDp = (listHeightDp - deltaPx / density.density)
                            .coerceIn(WrongListMinHeightDp, maxHeightDp)
                    },
                )
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        }

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

        // ---------- 错词列表（高度由拖动决定） ----------
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
                    .height(listHeightDp.dp)
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
