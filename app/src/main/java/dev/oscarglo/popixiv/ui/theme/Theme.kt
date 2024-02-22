package dev.oscarglo.popixiv.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import dev.oscarglo.popixiv.util.Prefs

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val system = isSystemInDarkTheme()

    val theme by Prefs.APPEARANCE_THEME.state()
    println(theme)

    val darkTheme = when (theme) {
        "system" -> system
        "dark" -> true
        else -> false
    }

    val colors = when {
        darkTheme -> darkColors(
            primary = Color(0xff0096fa),
            onPrimary = Color.White,
        )
        else -> lightColors(
            primary = Color(0xff0096fa),
            onPrimary = Color.White
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode)
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.primary.toArgb()
        }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        content = content
    )
}