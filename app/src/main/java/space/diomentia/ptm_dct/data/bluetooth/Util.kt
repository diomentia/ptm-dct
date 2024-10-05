package space.diomentia.ptm_dct.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat

val btPermissions = when (Build.VERSION.SDK_INT) {
    in Build.VERSION_CODES.S..Int.MAX_VALUE -> listOf(
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )
    in Build.VERSION_CODES.Q until Build.VERSION_CODES.S -> listOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    else -> listOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
}

fun checkBtPermissions(context: Context): Boolean {
    btPermissions.forEach {
        if (ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED) {
            return false
        }
    }
    return true
}

fun getBtAdapter(context: Context): BluetoothAdapter? {
    if (!checkBtPermissions(context)) return null
    return context.getSystemService(BluetoothManager::class.java).adapter
}

@Composable
fun ListenBtState(
    updater: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> updater(
                    checkBtPermissions(context) &&
                    (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF))
                        == BluetoothAdapter.STATE_ON
                )
            }
        }
    }
    val btAdapter = getBtAdapter(LocalContext.current)
    LaunchedEffect(Unit) {
        updater(btAdapter?.state == BluetoothAdapter.STATE_ON)
        context.registerReceiver(
            receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
}