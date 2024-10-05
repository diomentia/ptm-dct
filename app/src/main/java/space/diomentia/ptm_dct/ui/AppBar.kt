package space.diomentia.ptm_dct.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp

@Composable
fun PtmTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigation: @Composable () -> Unit = {},
    actions: (@Composable () -> Unit)? = null,
) {
    var currentHeight by remember { mutableStateOf(0.dp) }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .getActualSize {
                currentHeight = it.height
            }
            .then(modifier)
    ) {
        val slanted = GenericShape { size, _ ->
            moveTo(0f, 0f)
            lineTo(size.width + size.height / 3f, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
        }
        val backSlanted = GenericShape { size, _ ->
            moveTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(-size.height / 3f, size.height)
            lineTo(0f, 0f)
        }
        Surface(
            modifier = Modifier.defaultMinSize(minHeight = currentHeight),
            shape = slanted,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(12.dp)
            ) {
                Box {
                    navigation()
                }
                Box(Modifier.padding(horizontal = 4.dp)) {
                    ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                        title()
                    }
                }
            }
        }
        if (actions != null) {
            Surface(
                modifier = Modifier.defaultMinSize(minHeight = currentHeight),
                shape = backSlanted,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Box(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(12.dp)
                ) {
                    actions()
                }
            }
        }
    }
}