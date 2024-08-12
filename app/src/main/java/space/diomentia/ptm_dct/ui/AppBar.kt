package space.diomentia.ptm_dct.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
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
            shape = slanted,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .clipToBounds()
                    .padding(16.dp)
            ) {
                Box(Modifier.padding(horizontal = 8.dp)) {
                    navigation()
                }
                Box(Modifier.padding(horizontal = 8.dp)) {
                    ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                        title()
                    }
                }
            }
        }
        if (actions != null) {
            Surface(
                shape = backSlanted,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Box(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .clipToBounds()
                        .padding(16.dp)
                ) {
                    actions()
                }
            }
        }
    }
}