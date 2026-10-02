package com.recite.words.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.recite.words.speech.WordSpeaker

/**
 * 应用外壳：顶栏 + 两个标签页。
 *
 * 「卡片背词」与「多选测试」共享同一个 [ReciteViewModel]，所以词本、错词、
 * 导入在这两个界面之间天然一致。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: ReciteViewModel) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 全应用共用一个 TTS 引擎，随界面销毁释放
    val speaker = remember { WordSpeaker(context) }
    DisposableEffect(Unit) {
        onDispose { speaker.close() }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val message = viewModel.message
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    var showSettings by remember { mutableStateOf(false) }
    var showWordEdit by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("单词记忆助手") },
                actions = {
                    TextButton(onClick = { showWordEdit = true }) { Text("📝编辑词本") }
                    TextButton(onClick = { showSettings = true }) { Text("⚙️ 设置") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TabRow(selectedTabIndex = viewModel.currentTab.ordinal) {
                AppTab.entries.forEach { tab ->
                    Tab(
                        selected = viewModel.currentTab == tab,
                        onClick = { viewModel.currentTab = tab },
                        text = { Text(tab.label) },
                    )
                }
            }

            when (viewModel.currentTab) {
                AppTab.CARD -> CardScreen(
                    data = data,
                    viewModel = viewModel,
                    speaker = speaker,
                    modifier = Modifier.weight(1f),
                )

                AppTab.QUIZ -> QuizScreen(
                    data = data,
                    viewModel = viewModel,
                    speaker = speaker,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            data = data,
            viewModel = viewModel,
            onDismiss = { showSettings = false },
        )
    }

    if (showWordEdit) {
        WordEditSheet(
            data = data,
            viewModel = viewModel,
            onDismiss = { showWordEdit = false },
        )
    }
}
