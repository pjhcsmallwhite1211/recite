package com.recite.words.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.recite.words.data.ReciteData
import com.recite.words.data.WordItem
import com.recite.words.ui.theme.DangerRed

/**
 * 「编辑当前单词本」面板，对齐参考项目两个 HTML 的同名弹窗：
 * 追加单词、逐条修改、逐条删除（删除会同步清理错词库）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordEditSheet(
    data: ReciteData,
    viewModel: ReciteViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val book = data.currentBook()

    var newWord by remember { mutableStateOf("") }
    var newCn by remember { mutableStateOf("") }
    var pendingDeleteIndex by remember { mutableStateOf<Int?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "编辑当前单词本：${book?.bookName.orEmpty()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                OutlinedTextField(
                    value = newWord,
                    onValueChange = { newWord = it },
                    label = { Text("单词") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = newCn,
                    onValueChange = { newCn = it },
                    label = { Text("释义") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        if (viewModel.recite.addWord(newWord, newCn)) {
                            newWord = ""
                            newCn = ""
                        } else {
                            viewModel.showMessage("单词和释义不能为空")
                        }
                    },
                ) { Text("添加") }
            }

            HorizontalDivider()

            val items = book?.wordItems.orEmpty()
            if (items.isEmpty()) {
                Text(
                    text = "当前词本还没有单词，可在设置里导入词表",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    itemsIndexed(items) { index, item ->
                        WordEditRow(
                            item = item,
                            onSave = { word, cn ->
                                if (!viewModel.recite.updateWord(index, word, cn)) {
                                    viewModel.showMessage("单词、释义不能为空")
                                }
                            },
                            onDelete = { pendingDeleteIndex = index },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("关闭") }
        }
    }

    pendingDeleteIndex?.let { index ->
        val word = book?.wordItems?.getOrNull(index)?.word.orEmpty()
        AlertDialog(
            onDismissRequest = { pendingDeleteIndex = null },
            title = { Text("删除单词") },
            text = { Text("确定删除「$word」？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.recite.deleteWord(index)
                    pendingDeleteIndex = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteIndex = null }) { Text("取消") }
            },
        )
    }
}

/** 单条词条的编辑行；输入框内容跟随词条本体，保存前不写回数据。 */
@Composable
private fun WordEditRow(
    item: WordItem,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
) {
    var word by remember(item) { mutableStateOf(item.word) }
    var cn by remember(item) { mutableStateOf(item.cn) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OutlinedTextField(
            value = word,
            onValueChange = { word = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = cn,
            onValueChange = { cn = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onSave(word, cn) }) { Text("保存") }
        TextButton(onClick = onDelete) { Text("删除", color = DangerRed) }
    }
}
