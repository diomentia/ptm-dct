package space.diomentia.ptm_dct.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
private fun getWindowSize(): Size {
    return LocalConfiguration.current.let {
        Size(
            with(LocalDensity.current) { it.screenWidthDp.dp.toPx() },
            with(LocalDensity.current) { it.screenHeightDp.dp.toPx() }
        )
    }
}

@Composable
fun SideArrowContainer(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    toRight: Boolean = true,
    slantFactor: Int = 8,
    content: @Composable () -> Unit
) {
    val slant = 1f / slantFactor
    val screenSize = getWindowSize()
    var absCoordinates by remember { mutableStateOf(Offset.Zero) }
    Box(Modifier
        .onGloballyPositioned { coordinates ->
            absCoordinates = coordinates.positionInWindow()
        }
        .drawWithCache {
            val sideArrow = if (toRight)
                Path().apply {
                    moveTo(-absCoordinates.x, 0f)
                    lineTo(screenSize.width * (1f - slant) - absCoordinates.x, 0f)
                    lineTo(screenSize.width - absCoordinates.x, size.height / 2f)
                    lineTo(screenSize.width * (1f - slant) - absCoordinates.x, size.height)
                    lineTo(-absCoordinates.x, size.height)
                }
            else
                Path().apply {
                    moveTo(screenSize.width - absCoordinates.x, 0f)
                    lineTo(screenSize.width * slant - absCoordinates.x, 0f)
                    lineTo(-absCoordinates.x, size.height / 2f)
                    lineTo(screenSize.width * slant - absCoordinates.x, size.height)
                    lineTo(screenSize.width - absCoordinates.x, size.height)
                }
            onDrawBehind {
                drawPath(sideArrow, color)
            }
        }
        .then(modifier)
    ) {
        content()
    }
}

@Composable
fun DownArrowContainer(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    slantFactor: Int = 10,
    content: @Composable () -> Unit
) {
    val screenSize = getWindowSize()
    var absCoordinates by remember { mutableStateOf(Offset.Zero) }
    val offset = screenSize.height / slantFactor
    Box(Modifier
        .onGloballyPositioned { coordinates ->
            absCoordinates = coordinates.positionInWindow()
        }
        .drawWithCache {
            val downArrow = Path().apply {
                moveTo(-absCoordinates.x, -absCoordinates.y)
                lineTo(-absCoordinates.x, size.height - offset / 2)
                lineTo(screenSize.width / 2f - absCoordinates.x, size.height + offset / 2)
                lineTo(screenSize.width - absCoordinates.x, size.height - offset / 2)
                lineTo(screenSize.width - absCoordinates.x, -absCoordinates.y)
            }
            onDrawBehind {
                drawPath(downArrow, color)
            }
        }
        .then(modifier)
    ) {
        content()
    }
}