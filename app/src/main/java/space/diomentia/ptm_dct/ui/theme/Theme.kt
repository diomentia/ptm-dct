package space.diomentia.ptm_dct.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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
fun MCMControllerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = PtmColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}