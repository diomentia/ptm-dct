package space.diomentia.ptm_dct

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.alpha
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
import androidx.core.content.IntentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import space.diomentia.ptm_dct.data.ApplicationPreferences
import space.diomentia.ptm_dct.data.LocalGattConnection
import space.diomentia.ptm_dct.data.LocalSnackbarHostState
import space.diomentia.ptm_dct.data.Session
import space.diomentia.ptm_dct.data.bluetooth.PtmMikSerialPort
import space.diomentia.ptm_dct.data.exportJournalToExcel
import space.diomentia.ptm_dct.ui.DownArrowContainer
import space.diomentia.ptm_dct.ui.PtmFilledButton
import space.diomentia.ptm_dct.ui.PtmSnackbarHost
import space.diomentia.ptm_dct.ui.PtmTopBar
import space.diomentia.ptm_dct.ui.setupEdgeToEdge
import space.diomentia.ptm_dct.ui.theme.PtmTheme
import space.diomentia.ptm_dct.ui.theme.blue_mirage
import java.text.DecimalFormatSymbols
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MeasurementsActivity : ComponentActivity() {
    private lateinit var mDevice: BluetoothDevice
    private var mSerialPort by mutableStateOf<PtmMikSerialPort?>(null)
    private val mCoroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mDevice = IntentCompat
            .getParcelableExtra(
                intent,
                PairingActivity.EXTRA_CONNECTED_DEVICE,
                BluetoothDevice::class.java
            )!!
        setupEdgeToEdge(activity = this)
        val snackbarHostState = SnackbarHostState()
        setContent {
            PtmTheme {
                CompositionLocalProvider(
                    LocalSnackbarHostState provides snackbarHostState,
                    LocalGattConnection provides mSerialPort
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
        if (mSerialPort == null) {
            mCoroutineScope.launch {
                mSerialPort = PtmMikSerialPort(
                    mDevice,
                    commandTimeout = ApplicationPreferences.getCommandTimeout(applicationContext)
                )
                Session.mikState = mSerialPort?.mikState
            }
        }
        mSerialPort!!.connect()
    }

    override fun onDestroy() {
        super.onDestroy()
        mSerialPort?.cancel()
        mSerialPort = null
    }
}

@OptIn(DelicateCoroutinesApi::class)
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
            if (it == PtmMikSerialPort.Command.GetStatus)
                return@let
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
            {
                context.startActivity(
                    Intent(context, JournalActivity::class.java)
                )
            },
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.open_journal),
                style = MaterialTheme.typography.titleMedium
            )
        }

        val demoPassport by ApplicationPreferences.rememberDemoPassport()
        PtmFilledButton(
            {
                demoPassport?.let { pdf ->
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            setData(pdf)
                            setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                            setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    )
                }
            },
            enabled = demoPassport != null,
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.open_kip_passport),
                style = MaterialTheme.typography.titleMedium
            )
        }

        val reportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/vnd")
        ) { uri ->
            if (uri == null)
                return@rememberLauncherForActivityResult
            val workbook = gatt.mikState.exportJournalToExcel(context)
                ?: return@rememberLauncherForActivityResult
            GlobalScope.launch {
                context.contentResolver.openOutputStream(uri).let {
                    workbook.write(it)
                }
            }
            context as Activity
            context.setResult(Activity.RESULT_OK)
            context.finish()
        }
        PtmFilledButton(
            {
                reportLauncher.launch(
                    "journal_%s.xlsx".format(
                        LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern(
                                "yyyy-MM-dd_hhmmss"
                            )
                        )
                    )
                )
            },
            enabled = gatt.mikState.endJournalReading,
            modifier = Modifier
                .padding(4.dp)
                .fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.generate_report),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            "Serial: ${gatt.mikState.authInfo?.serialNumber}, " +
                    "Ver: ${gatt.mikState.authInfo?.firmwareVersion}, " +
                    "manufactured: ${
                        gatt.mikState.authInfo?.dateOfManufacture?.format(
                            DateTimeFormatter.ISO_LOCAL_DATE
                        )
                    }",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .alpha(.5f)
                .padding(top = 16.dp)
        )
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
            .padding(8.dp)
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Battery5Bar,
            contentDescription = stringResource(R.string.current_charge)
        )
        Text(
            "%.1fV".format(gatt?.mikState?.statusInfo?.battery ?: 0f),
            style = MaterialTheme.typography.labelMedium
        )

        Icon(
            Icons.Default.Thermostat,
            contentDescription = stringResource(R.string.current_temperature)
        )
        Text(
            "${gatt?.mikState?.statusInfo?.controllerTemperature ?: 0}°",
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
        val fontSize = with(LocalDensity.current) { 28.dp.toSp() }
        Surface(
            border = BorderStroke(8.dp, MaterialTheme.colorScheme.primary),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .wrapContentSize()
                .padding(12.dp)
                .width(with(LocalDensity.current) { fontSize.toDp() * 5 })
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
                        modifier = Modifier.size(with(LocalDensity.current) { fontSize.toDp() * .65f })
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
                            val parts = "%.1f"
                                .format(cellValue)
                                .split(DecimalFormatSymbols.getInstance().decimalSeparator)
                            append(parts[0])
                            withStyle(SpanStyle(fontSize = 0.5f.em)) {
                                append(".${parts[1]}mV")
                            }
                        } else {
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
            VoltageCell(1, gatt?.mikState?.statusInfo?.voltage?.getOrNull(0))
            VoltageCell(2, gatt?.mikState?.statusInfo?.voltage?.getOrNull(1))
        }
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            VoltageCell(3, gatt?.mikState?.statusInfo?.voltage?.getOrNull(2))
            VoltageCell(4, gatt?.mikState?.statusInfo?.voltage?.getOrNull(3))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainPreview() {
    Contents()
}