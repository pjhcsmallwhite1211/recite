package com.recite.words.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.recite.words.data.ReciteData
import com.recite.words.data.WordBook
import com.recite.words.ui.theme.DangerRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 设置面板：词本管理 + 导入词表 + 导出错词 + JSON 备份导入导出。
 *
 * 对齐参考项目两个 HTML 的「设置与单词本管理」弹窗；
 * 网页版用 `<input type="file">` 与 Blob 下载，这里换成系统文件选择器（SAF）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    data: ReciteData,
    viewModel: ReciteViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var newBookName by remember { mutableStateOf("") }
    var importText by remember { mutableStateOf("") }
    var pendingDeleteBookId by remember { mutableStateOf<String?>(null) }
    var pendingBackup by remember { mutableStateOf<ReciteData?>(null) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            writeTextToUri(context, uri, viewModel.recite.backupJson())
                .onSuccess { viewModel.showMessage("✅ 备份已导出") }
                .onFailure { viewModel.showMessage("导出失败：${it.message}") }
        }
    }

    val exportWrongWordsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            val text = viewModel.recite.currentWrongWordsText()
            if (text.isBlank()) {
                viewModel.showMessage("当前本子没有错词")
            } else {
                writeTextToUri(context, uri, text)
                    .onSuccess { viewModel.showMessage("✅ 错词已导出") }
                    .onFailure { viewModel.showMessage("导出失败：${it.message}") }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            readTextFromUri(context, uri)
                .mapCatching { viewModel.recite.parseBackup(it) }
                .onSuccess { pendingBackup = it }
                .onFailure { viewModel.showMessage("解析备份失败：${it.message}") }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "设置与单词本管理",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            // ---------- 新建词本 ----------
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newBookName,
                    onValueChange = { newBookName = it },
                    label = { Text("新单词本名称") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val name = newBookName.trim()
                        if (name.isEmpty()) {
                            viewModel.showMessage("请输入单词本名称")
                        } else {
                            viewModel.recite.createBook(name)
                            newBookName = ""
                        }
                    },
                ) { Text("新建") }
            }

            // ---------- 词本列表 ----------
            data.wordBookList.forEach { book ->
                BookRow(
                    book = book,
                    isCurrent = book.bookId == data.currentBookId,
                    canDelete = data.wordBookList.size > 1,
                    onSwitch = { viewModel.recite.switchBook(book.bookId) },
                    onDelete = { pendingDeleteBookId = book.bookId },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 6.dp))

            // ---------- 导入词表 ----------
            Text("导入新词（存入当前单词本）", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = importText,
                onValueChange = { importText = it },
                placeholder = {
                    Text("每行格式：单词%中文\n示例：\nsurvive%v.幸存；存活\nlook for%短语：寻找")
                },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val result = viewModel.recite.importWords(importText)
                        if (result.items.isEmpty()) {
                            viewModel.showMessage("解析失败！每行：单词%释义，用百分号分隔")
                        } else {
                            val skipped = if (result.hasFailure) {
                                "，跳过 ${result.skippedLines.size} 行无法解析的内容"
                            } else {
                                ""
                            }
                            viewModel.showMessage("成功导入 ${result.items.size} 个词$skipped")
                            importText = ""
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("导入到当前本子") }

                Button(
                    onClick = {
                        val bookName = data.currentBook()?.bookName ?: "词本"
                        exportWrongWordsLauncher.launch("错词本_$bookName.txt")
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("导出错词") }
            }
            Text(
                text = "每行「单词%释义」，百分号分隔；也兼容短横、制表符或空格",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(Modifier.padding(vertical = 6.dp))

            // ---------- 备份 ----------
            Text("完整数据备份", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { exportBackupLauncher.launch("word_backup_${todayStamp()}.json") },
                    modifier = Modifier.weight(1f),
                ) { Text("导出备份(JSON)") }

                Button(
                    onClick = { importBackupLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.weight(1f),
                ) { Text("加载备份(JSON)") }
            }
            Text(
                text = "加载备份会覆盖当前全部数据，请先导出保存！",
                style = MaterialTheme.typography.bodySmall,
                color = DangerRed,
            )

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("关闭") }
        }
    }

    // 删除词本二次确认
    pendingDeleteBookId?.let { bookId ->
        val bookName = data.wordBookList.firstOrNull { it.bookId == bookId }?.bookName.orEmpty()
        AlertDialog(
            onDismissRequest = { pendingDeleteBookId = null },
            title = { Text("删除单词本") },
            text = { Text("确定删除「$bookName」？单词与错词全部清空，不可恢复！") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.recite.deleteBook(bookId)
                    pendingDeleteBookId = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteBookId = null }) { Text("取消") }
            },
        )
    }

    // 加载备份二次确认
    pendingBackup?.let { backup ->
        AlertDialog(
            onDismissRequest = { pendingBackup = null },
            title = { Text("加载备份") },
            text = {
                Text(
                    "⚠️ 加载备份将完全覆盖现有所有单词本与错词，确定继续？" +
                        "\n\n备份内含 ${backup.wordBookList.size} 个单词本。",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.recite.replaceAll(backup)
                    pendingBackup = null
                    viewModel.showMessage("✅ 备份加载成功")
                    onDismiss()
                }) { Text("覆盖") }
            },
            dismissButton = {
                TextButton(onClick = { pendingBackup = null }) { Text("取消") }
            },
        )
    }
}

/** 一行词本：名称、词数、切换与删除。 */
@Composable
private fun BookRow(
    book: WordBook,
    isCurrent: Boolean,
    canDelete: Boolean,
    onSwitch: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = if (isCurrent) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${book.bookName} (${book.wordItems.size}词)",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onSwitch, enabled = !isCurrent) { Text("切换") }
            TextButton(onClick = onDelete, enabled = canDelete) {
                Text("删除", color = DangerRed)
            }
        }
    }
}

/** 导出文件名里的日期戳（避免用 API 26+ 才有的 java.time）。 */
private fun todayStamp(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun writeTextToUri(context: Context, uri: Uri, text: String): Result<Unit> = runCatching {
    val stream = context.contentResolver.openOutputStream(uri) ?: error("无法打开输出流")
    stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
}

private fun readTextFromUri(context: Context, uri: Uri): Result<String> = runCatching {
    val stream = context.contentResolver.openInputStream(uri) ?: error("无法打开输入流")
    stream.use { it.readBytes().decodeToString() }
}
