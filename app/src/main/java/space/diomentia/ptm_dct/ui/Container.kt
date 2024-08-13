package space.diomentia.ptm_dct.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow

@Composable
fun SideArrowContainer(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
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
                    lineTo(screenSize.width - absCoordinates.x * (1 + slant), size.height / 2f)
                    lineTo(screenSize.width * (1f - slant) - absCoordinates.x, size.height)
                    lineTo(-absCoordinates.x, size.height)
                }
            else
                Path().apply {
                    moveTo(screenSize.width - absCoordinates.x, 0f)
                    lineTo(screenSize.width * slant - absCoordinates.x, 0f)
                    lineTo(-absCoordinates.x * (1 - slant), size.height / 2f)
                    lineTo(screenSize.width * slant - absCoordinates.x, size.height)
                    lineTo(screenSize.width - absCoordinates.x, size.height)
                }
            onDrawBehind {
                drawPath(sideArrow, containerColor)
            }
        }
        .then(modifier)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

@Composable
fun DownArrowContainer(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
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
                drawPath(downArrow, containerColor)
            }
        }
        .then(modifier)
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}