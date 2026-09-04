package cz.mtrakal.vmptesty.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Material 3 nemá barvu pro "správně" — `error` má jen svůj protipól v podobě
 * primary. Zelenou proto dodáváme sami, ve dvou variantách podle světla/tmy,
 * aby zvýraznění správné odpovědi bylo čitelné v obou režimech.
 */
@Composable
@ReadOnlyComposable
internal fun correctColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFF7BD88F) else Color(0xFF1B7F3B)

@Composable
@ReadOnlyComposable
internal fun correctContainerColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFF14351F) else Color(0xFFD7F2DE)

private val LightColors = lightColorScheme(
    primary = Color(0xFF14618E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCBE6FF),
    onPrimaryContainer = Color(0xFF001E30),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FCDF7),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF004B70),
    onPrimaryContainer = Color(0xFFCBE6FF),
)

/** Námořně modré téma, jinak Material 3 defaulty. */
@Composable
fun VmpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
