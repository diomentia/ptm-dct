package space.diomentia.ptm_dct

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.alorma.compose.settings.ui.SettingsMenuLink
import space.diomentia.ptm_dct.data.ApplicationSettings
import space.diomentia.ptm_dct.data.PasswordHash
import space.diomentia.ptm_dct.ui.BorderedDialogContainer
import space.diomentia.ptm_dct.ui.PtmTopBar
import space.diomentia.ptm_dct.ui.theme.PtmDctTheme
import space.diomentia.ptm_dct.ui.theme.blue_zodiac
import space.diomentia.ptm_dct.ui.theme.white

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(blue_zodiac.copy(alpha = .25f).toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
        )
        setContent {
            PtmDctTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        PtmTopBar(
                            title = {
                                Text(
                                    resources.getString(R.string.settings),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        )
                    }
                ) { innerPadding ->
                    val colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                    var showDialog by remember { mutableStateOf(false) }
                    if (showDialog) {
                        ChangePasswordDialog(onDismissRequest = { showDialog = false })
                    }
                    Column(
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        SettingsMenuLink(
                            title = { Text(stringResource(R.string.change_password)) },
                            colors = colors
                        ) {
                            showDialog = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(
    onDismissRequest: () -> Unit = {}
) {
    var adminPassword by remember { mutableStateOf("") }
    val onConfirmation = {
        ApplicationSettings.passwordAdmin = PasswordHash.encrypt(adminPassword)
        onDismissRequest()
    }
    Dialog(onDismissRequest) {
        BorderedDialogContainer {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Column(
                    Modifier
                        .height(IntrinsicSize.Min)
                        .padding(8.dp)
                ) {
                    val textPadding = Modifier.padding(8.dp)
                    val textStyle = MaterialTheme.typography.bodyMedium
                    Text(
                        stringResource(R.string.new_password) + ":",
                        textPadding,
                        style = textStyle
                    )
                    TextField(
                        placeholder = {
                            Text(
                                stringResource(R.string.password),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = white.copy(alpha = .25f)
                                )
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        value = adminPassword,
                        onValueChange = { adminPassword = it }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            stringResource(R.string.cancel),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    TextButton(onClick = onConfirmation) {
                        Text(
                            stringResource(R.string.confirm),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}