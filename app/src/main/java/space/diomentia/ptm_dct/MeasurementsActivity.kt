package space.diomentia.ptm_dct

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.content.IntentCompat
import kotlinx.coroutines.launch
import space.diomentia.ptm_dct.data.LocalGattConnection
import space.diomentia.ptm_dct.data.LocalSnackbarHostState
import space.diomentia.ptm_dct.data.bluetooth.PtmMikSerialPort
import space.diomentia.ptm_dct.ui.DownArrowContainer
import space.diomentia.ptm_dct.ui.PtmFilledButton
import space.diomentia.ptm_dct.ui.PtmSnackbarHost
import space.diomentia.ptm_dct.ui.PtmTopBar
import space.diomentia.ptm_dct.ui.setupEdgeToEdge
import space.diomentia.ptm_dct.ui.theme.PtmTheme
import space.diomentia.ptm_dct.ui.theme.blue_mirage
import java.time.LocalDateTime

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
                                navigation = {
                                    IconButton(onClick = {
                                        setResult(RESULT_CANCELED)
                                        finish()
                                    }) {
                                        Icon(
                                            Icons.AutoMirrored.Default.ArrowBack,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp),
                                            contentDescription = stringResource(R.string.back)
                                        )
                                    }
                                },
                                actions = {
                                    StatusBar()
                                }
                            )
                        },
                        snackbarHost = { PtmSnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Contents(modifier = Modifier.padding(innerPadding))
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
        mGattConnection?.cancel()
        mGattConnection = null
        finish()
    }
}

@Composable
private fun Contents(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = LocalSnackbarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val gatt = LocalGattConnection.current ?: throw IllegalStateException()
    LaunchedEffect(Unit) {
        gatt.run {
            sendCommand(PtmMikSerialPort.Command.Authentication)
            sendCommand(PtmMikSerialPort.Command.GetStatus)
            sendCommand(PtmMikSerialPort.Command.GetSetup)
            setDateTime(LocalDateTime.now())
            sendCommand(PtmMikSerialPort.Command.GetJournal)
            updateStatus()
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
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DownArrowContainer {
            Column {
                VoltageGrid(Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.weight(1f))
        PtmFilledButton(
            {},
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.open_journal),
                style = MaterialTheme.typography.titleMedium
            )
        }
        PtmFilledButton(
            {},
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.open_kip_passport),
                style = MaterialTheme.typography.titleMedium
            )
        }
        PtmFilledButton(
            {},
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.generate_report),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    if (!gatt.isConnected) {
        Text(
            stringResource(R.string.no_connection),
            modifier = Modifier
                .fillMaxSize()
                .background(blue_mirage.copy(alpha = .8f))
                .wrapContentSize()
                .padding(32.dp),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,

        )
    }
}

@Composable
private fun StatusBar(
    modifier: Modifier = Modifier
) {
    /*
    Surface(
        shape = RoundedCornerShape(100),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {}
     */
    val gatt = LocalGattConnection.current
    Row(
        modifier = Modifier
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Battery5Bar,
            contentDescription = stringResource(R.string.current_charge)
        )
        Text(
            "%.1fV".format(gatt?.statusInfo?.battery ?: 0f),
            style = MaterialTheme.typography.labelMedium
        )

        Icon(
            Icons.Default.Thermostat,
            contentDescription = stringResource(R.string.current_temperature)
        )
        Text(
            "${gatt?.statusInfo?.controllerTemperature ?: 0}°",
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun VoltageGrid(
    modifier: Modifier = Modifier
) {
    @Composable
    fun VoltageCell(
        cellNumber: Int,
        cellValue: Float?,
        modifier: Modifier = Modifier
    ) {
        val fontSize = with(LocalDensity.current) { 24.dp.toSp() }
        Surface(
            border = BorderStroke(8.dp, MaterialTheme.colorScheme.primary),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .wrapContentSize()
                .padding(12.dp)
                .width(with (LocalDensity.current) { fontSize.toDp() * 5 })
                .then(modifier)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(with (LocalDensity.current) { fontSize.toDp() * .65f })
                    )
                    Text(
                        "#$cellNumber:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = fontSize * .5
                    )
                }
                Text(
                    buildAnnotatedString {
                        if (cellValue != null) {
                            val parts = "%.1f".format(cellValue).split(".")
                            append(parts[0])
                            withStyle(SpanStyle(fontSize = 0.5f.em)) {
                                append(".${parts[1]}mV")
                            }
                        }
                        else {
                            append("—")
                        }
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = fontSize
                )
            }
        }
    }

    val gatt = LocalGattConnection.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .then(modifier)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            VoltageCell(1, gatt?.statusInfo?.voltage?.getOrNull(0))
            VoltageCell(2, gatt?.statusInfo?.voltage?.getOrNull(1))
        }
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            VoltageCell(3, gatt?.statusInfo?.voltage?.getOrNull(2))
            VoltageCell(4, gatt?.statusInfo?.voltage?.getOrNull(3))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    Contents()
}