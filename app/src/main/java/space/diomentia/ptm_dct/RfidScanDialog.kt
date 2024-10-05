package space.diomentia.ptm_dct

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import space.diomentia.ptm_dct.data.RfidController
import space.diomentia.ptm_dct.ui.BorderedDialogContainer

@Composable
fun RfidScanDialog(
    onDismissRequest: () -> Unit = {},
    onConfirmation: (RfidController.RfidTag) -> Unit
) {
    val dismiss = {
        RfidController.stopRead()
        onDismissRequest()
    }
    var currentTag by remember { mutableStateOf<RfidController.RfidTag?>(null) }
    Dialog(dismiss) {
        BorderedDialogContainer {
            Column(
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .padding(8.dp)
                ) {
                    val textPadding = Modifier.padding(8.dp)
                    val textStyle = MaterialTheme.typography.bodyMedium
                    if (currentTag == null) {
                        Text(
                            stringResource(R.string.tag_not_found),
                            textPadding,
                            style = textStyle
                        )
                    } else {
                        Text(
                            "EPC: ${currentTag?.epc}",
                            textPadding,
                            style = textStyle
                        )
                        Text(
                            "Data: ${currentTag?.userData}",
                            textPadding,
                            style = textStyle
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = dismiss) {
                        Text(
                            stringResource(R.string.cancel),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    TextButton(
                        enabled = currentTag != null,
                        onClick = { onConfirmation(currentTag!!) }) {
                        Text(
                            stringResource(R.string.accept),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        RfidController.startRead(object : RfidController.RfidListener {
            override fun onTagFound(tag: RfidController.RfidTag?) {
                if (tag != currentTag) {
                    currentTag = tag
                }
            }
            override fun onReadStopped() {
                dismiss()
            }
        })
    }
}

@Preview(showBackground = true)
@Composable
fun RfidScanDialogPreview() {
    RfidScanDialog {}
}