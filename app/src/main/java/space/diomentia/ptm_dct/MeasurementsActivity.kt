package space.diomentia.ptm_dct

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import space.diomentia.ptm_dct.data.LocalGattConnection
import space.diomentia.ptm_dct.data.LocalSnackbarHostState
import space.diomentia.ptm_dct.data.bluetooth.PtmMikGatt
import space.diomentia.ptm_dct.ui.DownArrowContainer
import space.diomentia.ptm_dct.ui.PtmSnackbarHost
import space.diomentia.ptm_dct.ui.PtmTopBar
import space.diomentia.ptm_dct.ui.setupEdgeToEdge
import space.diomentia.ptm_dct.ui.theme.PtmTheme

class MeasurementsActivity : ComponentActivity() {
    private var mDevice: BluetoothDevice? = null
    private var mGattConnection by mutableStateOf<PtmMikGatt?>(null)

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
            mGattConnection = PtmMikGatt(mDevice!!)
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
            mGattConnection = PtmMikGatt(mDevice!!)
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
    val gatt = LocalGattConnection.current
    if (gatt?.isConnected == true) {
        gatt.listenVoltage()
        gatt.listenBatteryLevel()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DownArrowContainer {
            Surface(
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
                        when (gatt?.batteryLevel) {
                            in 75..100 -> Icons.Default.BatteryFull
                            in 50 until 75 -> Icons.Default.Battery5Bar
                            in 25 until 50 -> Icons.Default.Battery2Bar
                            else -> Icons.Default.Battery0Bar
                        },
                        contentDescription = stringResource(R.string.current_charge)
                    )
                    Text("${gatt?.batteryLevel}%", style = MaterialTheme.typography.labelMedium)
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = stringResource(R.string.current_voltage)
                    )
                    Text("${gatt?.voltage}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainPreview() {
    Contents()
}