package space.diomentia.ptm_dct.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import space.diomentia.ptm_dct.ui.theme.blue_oxford

@Composable
fun SquareOutlinedButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonColors(
        contentColor = MaterialTheme.colorScheme.primary,
        containerColor = Color.Transparent,
        disabledContentColor = blue_oxford,
        disabledContainerColor = Color.Transparent
    ),
    roundedCorners: Boolean = false,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val buttonShape: Shape = RoundedCornerShape(if (roundedCorners) 25 else 0)
    var buttonSize by remember { mutableStateOf(0.dp) }
    Surface(
        modifier = Modifier
            .clip(buttonShape)
            .onSizeChanged { size ->
                val newSize = with(density) {
                    max(
                        size.width.toDp(),
                        size.height.toDp()
                    )
                }
                if (newSize != buttonSize) {
                    buttonSize = newSize
                }
            }
            .then(modifier),
        shape = buttonShape,
        border = BorderStroke(
            buttonSize / 16,
            if (enabled) colors.contentColor else colors.disabledContentColor
        ),
        color = if (enabled) colors.containerColor else colors.containerColor,
        contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
    ) {
        Box(Modifier.padding(buttonSize / 8)) {
            content()
        }
    }
}