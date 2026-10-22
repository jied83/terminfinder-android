package de.jied.terminfinder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
expect fun colorScheme(dark: Boolean): ColorScheme

@Composable
fun TerminTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme(isSystemInDarkTheme()), content = content)
}
