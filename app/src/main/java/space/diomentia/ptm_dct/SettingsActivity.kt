package space.diomentia.ptm_dct

import android.content.ContentResolver
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.alorma.compose.settings.ui.SettingsMenuLink
import kotlinx.coroutines.launch
import space.diomentia.ptm_dct.data.ApplicationSettings
import space.diomentia.ptm_dct.data.LocalSnackbarHostState
import space.diomentia.ptm_dct.data.PasswordHash
import space.diomentia.ptm_dct.data.Session
import space.diomentia.ptm_dct.ui.BorderedDialogContainer
import space.diomentia.ptm_dct.ui.PtmSnackbarHost
import space.diomentia.ptm_dct.ui.PtmTopBar
import space.diomentia.ptm_dct.ui.theme.PtmTheme
import space.diomentia.ptm_dct.ui.theme.blue_zodiac
import space.diomentia.ptm_dct.ui.theme.white
import java.io.File

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(blue_zodiac.copy(alpha = .25f).toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
        )
        val snackbarHostState = SnackbarHostState()
        setContent {
            CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState
            ) {
                PtmTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            PtmTopBar(
                                title = {
                                    Text(
                                        resources.getString(R.string.settings),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                },
                                navigation = {
                                    IconButton(onClick = { finish() }) {
                                        Icon(
                                            Icons.AutoMirrored.Default.ArrowBack,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp),
                                            contentDescription = stringResource(R.string.back)
                                        )
                                    }
                                }
                            )
                        },
                        snackbarHost = { PtmSnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        val coroutineScope = rememberCoroutineScope()
                        val colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                        var showChangePasswordDialog by remember { mutableStateOf(false) }
                        if (showChangePasswordDialog) {
                            ChangePasswordDialog(onDismissRequest = {
                                showChangePasswordDialog = false
                            })
                        }
                        Column(
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            SettingsMenuLink(
                                title = { Text(stringResource(R.string.change_password)) },
                                colors = colors
                            ) {
                                if (Session.userLevel >= Session.AccessLevel.Admin) {
                                    showChangePasswordDialog = true
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar(
                                            applicationContext.getString(R.string.access_denied)
                                        )
                                    }
                                }
                            }

                            val demoPassportLauncher = rememberLauncherForActivityResult(
                                ActivityResultContracts.OpenDocument()
                            ) { uri ->
                                if (uri == null)
                                    return@rememberLauncherForActivityResult
                                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                ApplicationSettings.demoPassport = uri
                            }
                            SettingsMenuLink(
                                title = { Text(stringResource(R.string.choose_demo_passport)) },
                                colors = colors
                            ) {
                                demoPassportLauncher.launch(arrayOf("application/pdf"))
                            }
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
        PasswordHash.encrypt(adminPassword)?.let {
            ApplicationSettings.passwordAdmin = it
            onDismissRequest()
        } ?: Unit
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
                        singleLine = true,
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
                    TextButton(
                        enabled = PasswordHash.checkPassword(adminPassword),
                        onClick = onConfirmation
                    ) {
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