package space.diomentia.ptm_dct

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@Composable
fun getBtAdapter(): BluetoothAdapter? = LocalContext.current
    .getSystemService(BluetoothManager::class.java)
    .adapter

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun getBtPermissionsState(): MultiplePermissionsState = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
    rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    )
} else {
    rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.BLUETOOTH
        )
    )
}

@Composable
fun listenBtState(
    updater: (Boolean) -> Unit
) {
    val btAdapter = getBtAdapter()
    LaunchedEffect(Unit) {
        updater(btAdapter?.state == BluetoothAdapter.STATE_ON)
    }
    LocalContext.current.registerReceiver(
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                updater(
                    (intent
                        ?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
                        ?: BluetoothAdapter.STATE_OFF) == BluetoothAdapter.STATE_ON
                )
            }
        },
        IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
    )
}