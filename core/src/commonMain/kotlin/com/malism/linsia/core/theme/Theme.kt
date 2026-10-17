package com.malism.linsia.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

enum class LinsiaColorPalette {
    /** 活力多巴胺 — 暖色调、娱乐、年轻化 */
    Dopamine,
}

val LocalLinsiaColorPalette = staticCompositionLocalOf { LinsiaColorPalette.Dopamine }

@Composable
fun LinsiaTheme(
    palette: LinsiaColorPalette = LinsiaColorPalette.Dopamine,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when (palette) {
        LinsiaColorPalette.Dopamine ->
            if (darkTheme) DopamineDarkColorScheme else DopamineLightColorScheme
    }

    CompositionLocalProvider(LocalLinsiaColorPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LinsiaTypography,
            content = content,
        )
    }
}

object LinsiaTheme {
    val palette: LinsiaColorPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalLinsiaColorPalette.current

    val colorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography
}
