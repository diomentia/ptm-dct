package space.diomentia.ptm_dct

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.IntentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.diomentia.ptm_dct.data.LocalSnackbarHostState
import space.diomentia.ptm_dct.data.LocalStep
import space.diomentia.ptm_dct.data.RfidController
import space.diomentia.ptm_dct.data.Session
import space.diomentia.ptm_dct.data.Session.Step
import space.diomentia.ptm_dct.data.bluetooth.ListenBtState
import space.diomentia.ptm_dct.data.bluetooth.btPermissions
import space.diomentia.ptm_dct.data.bluetooth.checkBtPermissions
import space.diomentia.ptm_dct.data.bluetooth.getBtAdapter
import space.diomentia.ptm_dct.ui.PtmOutlinedButton
import space.diomentia.ptm_dct.ui.PtmSnackbarHost
import space.diomentia.ptm_dct.ui.PtmTopBar
import space.diomentia.ptm_dct.ui.SideArrowContainer
import space.diomentia.ptm_dct.ui.setupEdgeToEdge
import space.diomentia.ptm_dct.ui.theme.PtmTheme
import space.diomentia.ptm_dct.ui.theme.blue_oxford
import space.diomentia.ptm_dct.ui.theme.white

class InitialActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(activity = this)
        val snackbarHostState = SnackbarHostState()
        setContent {
            CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState,
                LocalStep provides remember { mutableStateOf(Step.Password) }
            ) {
                PtmTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = { PtmTopBar(
                            title = { Text(stringResource(R.string.app_name)) },
                            actions = {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(
                                            this@InitialActivity,
                                            SettingsActivity::class.java
                                        )
                                        this@InitialActivity.startActivity(intent)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Settings,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp),
                                        contentDescription = stringResource(R.string.settings)
                                    )
                                }
                            }
                        ) },
                        snackbarHost = { PtmSnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Contents()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Contents(
    modifier: Modifier = Modifier
) {
    var currentStep by LocalStep.current
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .then(modifier),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SideArrowContainer(
            modifier = Modifier
                .padding(vertical = 8.dp),
            toRight = false,
            slantFactor = 4
        ) {
            Image(
                painter = painterResource(R.drawable.logo_ptm),
                contentDescription = stringResource(R.string.logo_ptm_description),
                modifier = Modifier
                    .padding(24.dp)
                    .heightIn(min = 64.dp, max = 175.dp)
                    .fillMaxSize()
            )
        }
        StepHelper(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )
        // TODO: segmented button USB/Bluetooth
        PasswordField(Modifier.padding(8.dp))
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ScanRfidButton()
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
            Step.UserLevel -> R.string.wrong_password
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

@Composable
private fun StepHelper(
    modifier: Modifier = Modifier
) {
    data class StepHint(val hintResource: Int, val step: Step, val isMain: Boolean = false)
    val stepHints = remember {
        arrayOf(
            StepHint(R.string.input_password, Step.Password, true),
            StepHint(R.string.wrong_password, Step.UserLevel),
            StepHint(R.string.wait_for_rfid_manager, Step.RfidManager),
            StepHint(R.string.find_rfid_tag, Step.RfidTag, true),
            StepHint(R.string.please_turn_on_bluetooth, Step.BluetoothTurnOn),
            StepHint(R.string.pair_bluetooth, Step.BluetoothPair, true)
        )
    }
    Column(
        modifier,
        horizontalAlignment = Alignment.Start
    ) {
        var mainIndex = 0
        for ((index, hint) in stepHints.withIndex()) {
            val stepState = LocalStep.current.value.ordinal - hint.step.ordinal
            val text = (remember(index) { if (hint.isMain) "${++mainIndex}. " else "" }
                    + stringResource(hint.hintResource))
            val color by animateColorAsState(
                if (hint.isMain) {
                    if (stepState == 0) white else blue_oxford
                } else {
                    MaterialTheme.colorScheme.primary
                },
                label = hint.hintResource.toString()
            )
            val fontSize = animateIntAsState(
                if (hint.isMain && stepState == 0) 18 else 14,
                label = hint.hintResource.toString()
            )
            AnimatedVisibility(
                hint.isMain && stepState > 0 || stepState == 0,
                label = hint.hintResource.toString()
            ) {
                Text(
                    text = text,
                    color = color,
                    fontWeight = if (hint.isMain && stepState == 0) FontWeight.Bold else FontWeight.Medium,
                    fontSize = fontSize.value.sp,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun PasswordField(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by LocalStep.current
    var passwordVisible by remember { mutableStateOf(false) }
    LaunchedEffect(passwordVisible) {
        if (passwordVisible) {
            delay(3000)
            passwordVisible = false
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .padding(vertical = 16.dp)
    ) {
        var passwordInput by remember { mutableStateOf("") }
        TextField(
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .then(modifier),
            label = { Text(stringResource(R.string.password)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = context.getString(
                            if (passwordVisible) R.string.hide_password else R.string.show_password
                        ),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            },
            value = passwordInput,
            onValueChange = { passwordInput = it }
        )
        val focusManager = LocalFocusManager.current
        FilledIconButton(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .padding(12.dp),
            onClick = {
                Session.updateUserLevel(passwordInput)
                if (currentStep >= Step.Password) {
                    if (passwordInput.isBlank()) {
                        currentStep = Step.Password
                    } else if (Session.userLevel == Session.AccessLevel.Guest) {
                        currentStep = Step.UserLevel
                    } else if (currentStep in arrayOf(Step.Password, Step.UserLevel)) {
                        currentStep = currentStep.next()
                    }
                }
                passwordInput = ""
                focusManager.clearFocus()
            },
            shape = RoundedCornerShape(30)
        ) {
            Icon(
                Icons.Default.Done,
                contentDescription = stringResource(R.string.apply),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        }
    }
    if (currentStep >= Step.UserLevel && Session.userLevel <= Session.AccessLevel.Guest) {
        currentStep = Step.UserLevel
    } else if (currentStep == Step.UserLevel) {
        currentStep = currentStep.next()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScanRfidButton(
    modifier: Modifier = Modifier
) {
    var currentStep by LocalStep.current
    if (currentStep == Step.RfidManager && RfidController.isAvailable ||
        currentStep == Step.RfidTag && Session.rfidTag != null) {
    // to test app without a device with RFID
    // if (currentStep == Step.RfidManager || currentStep == Step.RfidTag) {
        currentStep = currentStep.next()
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
                .requiredSize(64.dp)
        )
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BluetoothPairingButton(
    modifier: Modifier = Modifier
) {
    var currentStep by LocalStep.current
    var bluetoothEnabled by remember { mutableStateOf(false) }
    ListenBtState { bluetoothEnabled = it }
    if (currentStep > Step.BluetoothTurnOn && !bluetoothEnabled) {
        currentStep = Step.BluetoothTurnOn
    } else if (currentStep == Step.BluetoothTurnOn && bluetoothEnabled) {
        currentStep = currentStep.next()
    }
    var enabled by remember { mutableStateOf(false) }
    enabled = currentStep >= Step.BluetoothPair
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    val btPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        bluetoothEnabled = getBtAdapter(context)?.isEnabled ?: false
    }
    val btEnableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}
    val btConnectLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            return@rememberLauncherForActivityResult
        }
        IntentCompat.getParcelableExtra(result.data!!, PairingActivity.EXTRA_CONNECTED_DEVICE, BluetoothDevice::class.java)?.let { device ->
            Intent(context, MeasurementsActivity::class.java)
                .putExtra(PairingActivity.EXTRA_CONNECTED_DEVICE, device)
                .let { context.startActivity(it) }
        }
    }
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
                    if (!checkBtPermissions(context)) {
                        btPermissionsLauncher.launch(btPermissions.toTypedArray())
                        return@combinedClickable
                    }
                    btEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    return@combinedClickable
                }
                btConnectLauncher.launch(Intent(context, PairingActivity::class.java))
            }
            .then(modifier)
    ) {
        Icon(
            Icons.Default.Bluetooth,
            contentDescription = stringResource(R.string.button_bluetooth_pairing),
            modifier = Modifier
                .requiredSize(64.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InitialPreview() {
    PtmTheme {
        Contents()
    }
}