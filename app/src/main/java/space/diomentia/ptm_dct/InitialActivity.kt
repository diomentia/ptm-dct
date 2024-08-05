package space.diomentia.ptm_dct

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import space.diomentia.ptm_dct.data.LocalSnackbarHostState
import space.diomentia.ptm_dct.data.RfidController
import space.diomentia.ptm_dct.data.Session
import space.diomentia.ptm_dct.ui.PtmOutlinedButton
import space.diomentia.ptm_dct.ui.PtmTopBar
import space.diomentia.ptm_dct.ui.SideArrowContainer
import space.diomentia.ptm_dct.ui.theme.PtmDctTheme
import space.diomentia.ptm_dct.ui.theme.blue_zodiac
import space.diomentia.ptm_dct.ui.theme.white

private enum class Step {
    Password, RfidManager, RfidTag, BluetoothTurnOn, BluetoothPair;

    fun next(): Step = entries[this.ordinal + 1]
}

private val LocalStep = compositionLocalOf { mutableStateOf(Step.Password) }

class InitialActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(blue_zodiac.copy(alpha = .25f).toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
        )
        setContent {
            PtmDctTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                CompositionLocalProvider(
                    LocalSnackbarHostState provides snackbarHostState,
                    LocalStep provides remember { mutableStateOf(Step.Password) }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            PtmTopBar(
                                title = {
                                    Text(
                                        resources.getString(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            )
                        },
                        snackbarHost = { SnackbarHost(snackbarHostState) {
                            val interactionSource = remember { MutableInteractionSource() }
                            Snackbar(
                                it,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                }
                            )
                        } }
                    ) { innerPadding ->
                        Contents(Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@Composable
fun Contents(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .then(modifier),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SideArrowContainer(
            toRight = false,
            slantFactor = 4
        ) {
            Image(
                painter = painterResource(R.drawable.logo_ptm),
                contentDescription = stringResource(R.string.logo_ptm_description),
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .heightIn(min = 75.dp, max = 175.dp)
                    .fillMaxSize()
            )
        }
        // TODO: segmented button USB/Bluetooth
        Spacer(Modifier.weight(1f))
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            placeholder = {
                Text(
                    stringResource(R.string.password),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = white.copy(alpha = .25f)
                    )
                )
            },
            value = Session.userPassword,
            onValueChange = { Session.userPassword = it }
        )
        if (Session.userPassword == "") {
            LocalStep.current.value = Step.Password
        } else if (LocalStep.current.value == Step.Password) {
            LocalStep.current.value = LocalStep.current.value.next()
        }
        Row(
            modifier = Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StartScanButton()
            BluetoothPairingButton()
        }
    }
}

private fun stepHint(
    currentStep: Step,
    context: Context,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    val message = context.getString(
        when (currentStep) {
            Step.Password -> R.string.input_password
            Step.RfidManager -> R.string.wait_for_rfid_manager
            Step.RfidTag -> R.string.find_rfid_tag
            Step.BluetoothTurnOn -> R.string.please_turn_on_bluetooth
            Step.BluetoothPair -> R.string.pair_bluetooth
        }
    )
    coroutineScope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(message)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StartScanButton(
    modifier: Modifier = Modifier
) {
    val currentStep = LocalStep.current.value
    if (currentStep == Step.RfidManager && RfidController.isAvailable ||
        currentStep == Step.RfidTag && Session.rfidTag != null) {
        LocalStep.current.value = currentStep.next()
    }
    var enabled by remember { mutableStateOf(false) }
    enabled = currentStep >= Step.RfidTag

    var showDialog by remember { mutableStateOf(false) }
    if (enabled && showDialog) {
        RfidScanDialog(
            onDismissRequest = { showDialog = false },
            onConfirmation = { tag ->
                Session.rfidTag = tag
                showDialog = false
            }
        )
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    PtmOutlinedButton(
        roundedCorners = true,
        enabled = enabled,
        modifier = Modifier
            .combinedClickable(
                enabled = true,
                onClickLabel = stringResource(R.string.button_start_rfid_search),
                onLongClick = {
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.button_start_rfid_search)
                        )
                    }
                }
            ) {
                if (!enabled) {
                    stepHint(currentStep, context, snackbarHostState, coroutineScope)
                    return@combinedClickable
                }
                showDialog = true
            }
            .then(modifier)
    ) {
        Icon(
            Icons.Default.Nfc,
            contentDescription = stringResource(R.string.button_start_rfid_search),
            modifier = Modifier
                .size(64.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BluetoothPairingButton(
    modifier: Modifier = Modifier
) {
    val currentStep = LocalStep.current.value
    // TODO
    if (currentStep == Step.BluetoothTurnOn && false) {
        LocalStep.current.value = currentStep.next()
    }
    var enabled by remember { mutableStateOf(false) }
    enabled = currentStep >= Step.BluetoothPair
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    PtmOutlinedButton(
        roundedCorners = true,
        enabled = enabled,
        modifier = Modifier
            .combinedClickable(
                enabled = true,
                onClickLabel = stringResource(R.string.button_bluetooth_pairing),
                onLongClick = {
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.button_bluetooth_pairing)
                        )
                    }
                }
            ) {
                if (!enabled) {
                    stepHint(currentStep, context, snackbarHostState, coroutineScope)
                    return@combinedClickable
                }
            }
            .then(modifier)
    ) {
        Icon(
            Icons.Default.Bluetooth,
            contentDescription = stringResource(R.string.button_bluetooth_pairing),
            modifier = Modifier
                .size(64.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InitialPreview() {
    PtmDctTheme {
        Contents()
    }
}