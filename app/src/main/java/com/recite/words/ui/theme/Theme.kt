package com.recite.words.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 配色常量取自参考项目 HTML 的 CSS 变量，保证观感与网页版一致。
/** 主色 `--primary`。 */
val PrimaryBlue = Color(0xFF4A90E2)

/** 危险色 `--danger`。 */
val DangerRed = Color(0xFFE74C3C)

/** 危险色按下态 `--danger-active`。 */
val DangerRedActive = Color(0xFFC0392B)

/** 成功色 `--success`。 */
val SuccessGreen = Color(0xFF27AE60)

/** 页面背景 `--bg`。 */
val PageBackground = Color(0xFFF5F7FA)

/** 正文色 `--text`。 */
val TextDark = Color(0xFF2C3E50)

/** 错词 chip 底色 `--gray-light`。 */
val ChipGray = Color(0xFFEEEEEE)

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = PrimaryBlue,
    onSecondary = Color.White,
    error = DangerRed,
    onError = Color.White,
    background = PageBackground,
    onBackground = TextDark,
    surface = Color.White,
    onSurface = TextDark,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    error = DangerRed,
    onError = Color.White,
)

/** 应用主题，跟随系统深浅色。 */
@Composable
fun ReciteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
