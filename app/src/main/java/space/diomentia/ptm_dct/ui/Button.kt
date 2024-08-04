package space.diomentia.ptm_dct.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import space.diomentia.ptm_dct.ui.theme.blue_oxford

val PtmOutlinedButtonColors
    @Composable
    get() = ButtonColors(
    contentColor = MaterialTheme.colorScheme.primary,
    containerColor = Color.Transparent,
    disabledContentColor = blue_oxford,
    disabledContainerColor = Color.Transparent
)

@Composable
fun PtmOutlinedButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = PtmOutlinedButtonColors,
    roundedCorners: Boolean = false,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val buttonShape: Shape = RoundedCornerShape(if (roundedCorners) 25 else 0)
    var buttonMinSize by remember { mutableStateOf(0.dp) }
    Surface(
        modifier = Modifier
            .clip(buttonShape)
            .onSizeChanged { size ->
                val newSize = with(density) {
                    min(
                        size.width.toDp(),
                        size.height.toDp()
                    )
                }
                if (newSize != buttonMinSize) {
                    buttonMinSize = newSize
                }
            }
            .then(modifier),
        shape = buttonShape,
        border = BorderStroke(
            width = buttonMinSize / 16,
            color = if (enabled) colors.contentColor else colors.disabledContentColor
        ),
        color = if (enabled) colors.containerColor else colors.containerColor,
        contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
    ) {
        Box(Modifier.padding(buttonMinSize / 8)) {
            content()
        }
    }
}