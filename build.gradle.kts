// 顶层构建脚本：只声明插件，不在此处配置模块。
// 注意：AGP 9 起 Kotlin 支持已内置，不能也不应再应用 org.jetbrains.kotlin.android。
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
