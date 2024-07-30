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
    onTertiary = white,
    background = blue_zodiac,
    onBackground = white
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