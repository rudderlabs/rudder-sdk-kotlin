package com.rudderstack.sampleapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.Color.Companion.White

val LightColorScheme = lightColorScheme(
    onSecondary = White,
    secondaryContainer = Blue,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    onPrimary = White,
    onTertiary = White,
    onError = White,
    primary = Blue,
    onSurfaceVariant = GreyLight,
    onPrimaryContainer = Black,
    primaryContainer = Transparent,
    onSecondaryContainer = GreyLight,
)

@Composable
fun RudderAndroidLibsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}