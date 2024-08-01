package space.diomentia.ptm_dct.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PtmColorScheme = darkColorScheme(
    primary = blue_aquamarine,
    onPrimary = blue_zodiac,
    secondary = green_haze,
    onSecondary = white,
    tertiary = blue_cerulean,
    onTertiary = blue_zodiac,
    background = blue_zodiac,
    onBackground = white,
    surface = green_haze,
    onSurface = white,
    surfaceContainer = blue_oxford
)

@Composable
fun PtmDctTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = PtmColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}