package com.recite.words

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recite.words.ui.AppRoot
import com.recite.words.ui.ReciteViewModel
import com.recite.words.ui.theme.ReciteTheme

/** 应用唯一 Activity；界面全部由 Compose 描述。 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReciteTheme {
                val viewModel: ReciteViewModel = viewModel()
                AppRoot(viewModel = viewModel)
            }
        }
    }
}
