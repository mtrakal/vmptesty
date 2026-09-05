package cz.mtrakal.vmptesty.wear

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * Námořní modrá místo výchozí fialové Material 3.
 *
 * Hodinky mají OLED, takže pozadí zůstává černé — kreslit tmavě modrou plochu
 * přes celý displej by jen ubíralo výdrž a snižovalo kontrast.
 */
private val NauticalColors = ColorScheme(
    primary = Color(0xFF8FCDF7),
    primaryDim = Color(0xFF5EA9D8),
    primaryContainer = Color(0xFF06507A),
    onPrimary = Color(0xFF00344F),
    onPrimaryContainer = Color(0xFFCBE6FF),

    secondary = Color(0xFF9FCFE0),
    secondaryDim = Color(0xFF6FAABE),
    secondaryContainer = Color(0xFF11485A),
    onSecondary = Color(0xFF00323F),
    onSecondaryContainer = Color(0xFFC8ECF8),

    tertiary = Color(0xFF7FD2C1),
    tertiaryDim = Color(0xFF55AC9B),
    tertiaryContainer = Color(0xFF0C4D42),
    onTertiary = Color(0xFF00382F),
    onTertiaryContainer = Color(0xFFB8EFE3),

    // Karty odpovědí: tmavě modrošedé odstíny, ne fialové.
    surfaceContainerLow = Color(0xFF0D1620),
    surfaceContainer = Color(0xFF14212D),
    surfaceContainerHigh = Color(0xFF1E2E3D),
    onSurface = Color(0xFFE2ECF5),
    onSurfaceVariant = Color(0xFFA9BECE),

    outline = Color(0xFF7A93A6),
    outlineVariant = Color(0xFF3B4B59),

    background = Color.Black,
    onBackground = Color(0xFFE2ECF5),

    error = Color(0xFFFFB4AB),
    errorDim = Color(0xFFD3675C),
    errorContainer = Color(0xFF6B2019),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
)

/** Zelená pro "správně" — Material 3 pro úspěch vlastní barvu nemá. */
internal val CorrectColor = Color(0xFF7BD88F)

/** Podklad karty se správnou odpovědí. */
internal val CorrectContainerColor = Color(0xFF14351F)

@Composable
fun VmpWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NauticalColors, content = content)
}
