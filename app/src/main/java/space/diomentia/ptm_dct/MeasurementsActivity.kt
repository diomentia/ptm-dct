package space.diomentia.ptm_dct

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.content.IntentCompat
import kotlinx.coroutines.launch
import space.diomentia.ptm_dct.data.LocalGattConnection
import space.diomentia.ptm_dct.data.LocalSnackbarHostState
import space.diomentia.ptm_dct.data.bluetooth.PtmMikSerialPort
import space.diomentia.ptm_dct.ui.DownArrowContainer
import space.diomentia.ptm_dct.ui.PtmSnackbarHost
import space.diomentia.ptm_dct.ui.PtmTopBar
import space.diomentia.ptm_dct.ui.setupEdgeToEdge
import space.diomentia.ptm_dct.ui.theme.PtmTheme
import space.diomentia.ptm_dct.ui.theme.blue_mirage

class MeasurementsActivity : ComponentActivity() {
    private var mDevice: BluetoothDevice? = null
    private var mGattConnection by mutableStateOf<PtmMikSerialPort?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDevice = IntentCompat.getParcelableExtra(
            intent,
            PairingActivity.EXTRA_CONNECTED_DEVICE,
            BluetoothDevice::class.java
        )
        if (mDevice == null) {
            finish()
        } else {
            mGattConnection = PtmMikSerialPort(mDevice!!)
        }
        setupEdgeToEdge(activity = this)
        val snackbarHostState = SnackbarHostState()
        setContent {
            PtmTheme {
                CompositionLocalProvider(
                    LocalSnackbarHostState provides snackbarHostState,
                    LocalGattConnection provides mGattConnection
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            PtmTopBar(
                                title = { Text(stringResource(R.string.connected)) }
                            )
                        },
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

    override fun onStart() {
        super.onStart()
        if (mGattConnection == null && mDevice != null) {
            mGattConnection = PtmMikSerialPort(mDevice!!)
        }
        if (mGattConnection?.isConnected == false) {
            mGattConnection?.connect()
        }
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mGattConnection?.cancel()
        mGattConnection = null
    }
}

@Composable
private fun Contents() {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val gatt = LocalGattConnection.current ?: throw IllegalStateException()
    LaunchedEffect(Unit) {
        gatt.run {
            sendCommand(PtmMikSerialPort.Command.Authentication)
            sendCommand(PtmMikSerialPort.Command.GetStatus)
            sendCommand(PtmMikSerialPort.Command.GetSetup)
            sendCommand(PtmMikSerialPort.Command.GetJournal)
        }
    }
    if (!gatt.hasLastCommandSucceeded.second) {
        gatt.hasLastCommandSucceeded.first?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    context.resources
                        .getString(R.string.command_not_succeeded)
                        .format(it.name)
                )
            }
        }
    }
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DownArrowContainer {
            Column {
                Surface(
                    modifier = Modifier.fillMaxWidth().wrapContentSize(),
                    shape = RoundedCornerShape(100),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when (gatt.batteryLevel) {
                                in 75..100 -> Icons.Default.BatteryFull
                                in 50 until 75 -> Icons.Default.Battery5Bar
                                in 25 until 50 -> Icons.Default.Battery2Bar
                                else -> Icons.Default.Battery0Bar
                            },
                            contentDescription = stringResource(R.string.current_charge)
                        )
                        Text("${gatt.batteryLevel}%", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Text("Auth:\n${gatt.authInfo}\n")
                Text("Status:\n${gatt.statusInfo}\n")
                Text("Setup:\n${gatt.statusInfo}\n")
            }
        }
        gatt.journal.fastForEachIndexed { i, entry ->
            Text("${i + 1}. $entry")
        }
    }
    if (!gatt.isConnected) {
        Text(
            stringResource(R.string.no_connection),
            modifier = Modifier
                .fillMaxSize()
                .background(blue_mirage.copy(alpha = .7f))
                .wrapContentSize()
                .padding(32.dp),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,

        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    Contents()
}