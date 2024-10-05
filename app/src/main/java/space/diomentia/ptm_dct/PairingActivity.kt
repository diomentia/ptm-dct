package space.diomentia.ptm_dct

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import kotlinx.coroutines.launch
import space.diomentia.ptm_dct.data.LocalBtAdapter
import space.diomentia.ptm_dct.data.LocalSnackbarHostState
import space.diomentia.ptm_dct.data.bluetooth.checkBtPermissions
import space.diomentia.ptm_dct.data.bluetooth.getBtAdapter
import space.diomentia.ptm_dct.ui.PtmSnackbarHost
import space.diomentia.ptm_dct.ui.PtmTopBar
import space.diomentia.ptm_dct.ui.setupEdgeToEdge
import space.diomentia.ptm_dct.ui.theme.PtmTheme
import java.io.IOException

private var mIsDiscovering by mutableStateOf(false)
private val mFoundDevices = mutableStateMapOf<String, BluetoothDevice>()
private val mBondedDevices = mutableStateListOf<BluetoothDevice>()

class PairingActivity : ComponentActivity() {
    var mBtAdapter: BluetoothAdapter? = null

    companion object {
        const val EXTRA_CONNECTED_DEVICE = "connected_device"
    }

    private val btReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!checkBtPermissions(baseContext)) {
                return
            }
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    if (mBtAdapter?.state != BluetoothAdapter.STATE_ON) {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> mIsDiscovering = true
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> mIsDiscovering = false
                BluetoothDevice.ACTION_FOUND -> {
                    IntentCompat.getParcelableExtra(
                        intent,
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    )?.let { mFoundDevices[it.address] = it }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    mBondedDevices.apply {
                        clear()
                        mBtAdapter?.bondedDevices?.filterNotNullTo(this)
                    }
                    mFoundDevices.clear()
                    mBtAdapter?.apply {
                        cancelDiscovery()
                        startDiscovery()
                    }
                }
            }
        }
    }

    fun finishWithResult(device: BluetoothDevice) {
        val data = Intent(baseContext, this::class.java)
            .putExtra(EXTRA_CONNECTED_DEVICE, device)
        setResult(RESULT_OK, data)
        finish()
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBtAdapter = getBtAdapter(applicationContext)
        setupEdgeToEdge(activity = this)
        val snackbarHostState = SnackbarHostState()
        setContent {
            CompositionLocalProvider(
                LocalSnackbarHostState provides snackbarHostState,
                LocalBtAdapter provides mBtAdapter
            ) {
                PtmTheme {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = { PtmTopBar(
                            title = { Text(stringResource(R.string.choose_kip)) },
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
                                IconButton(
                                    onClick = {
                                        mBondedDevices.apply {
                                            clear()
                                            mBtAdapter?.bondedDevices?.filterNotNullTo(this)
                                        }
                                        mFoundDevices.clear()
                                        mBtAdapter?.apply {
                                            if (isDiscovering) {
                                                cancelDiscovery()
                                            }
                                            startDiscovery()
                                        }
                                    }
                                ) {
                                    Icon(
                                        if (mIsDiscovering) Icons.Default.Refresh else Icons.Default.Search,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp),
                                        contentDescription = stringResource(if (mIsDiscovering) R.string.refresh else R.string.start_search)
                                    )
                                }
                            }
                        ) },
                        snackbarHost = { PtmSnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Contents(padding = innerPadding)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
        mBtAdapter?.apply {
            mBondedDevices.apply {
                clear()
                mBtAdapter?.bondedDevices?.filterNotNullTo(this)
            }
        }
        registerReceiver(
            btReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            }
        )
    }

    @SuppressLint("MissingPermission")
    override fun onStop() {
        super.onStop()
        mBtAdapter?.cancelDiscovery()
        unregisterReceiver(btReceiver)
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun Contents(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(0.dp)
) {
    if (!checkBtPermissions(LocalContext.current)) {
        (LocalContext.current as? Activity)?.finish()
            ?: throw Exception("Bluetooth permissions should have been granted already")
    }
    LazyColumn(
        modifier = Modifier
            .padding(
                start = padding.calculateStartPadding(LocalLayoutDirection.current),
                top = 0.dp,
                end = padding.calculateEndPadding(LocalLayoutDirection.current),
                bottom = padding.calculateBottomPadding()
            )
            .then(Modifier.padding(horizontal = 16.dp))
            .then(modifier)
    ) {
        item { Spacer(Modifier.requiredHeight(padding.calculateTopPadding())) }
        item {
            Text(
                stringResource(R.string.known_devices),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        mBondedDevices.forEach { device ->
            item {
                ConnectableBtDevice(device)
            }
        }
        item {
            Text(
                stringResource(R.string.found_devices),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        mFoundDevices.forEach { (_, device) ->
            item {
                ConnectableBtDevice(device)
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun ConnectableBtDevice(
    device: BluetoothDevice,
    modifier: Modifier = Modifier,
) {
    if (device.name == null || device.address == null) {
        return
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    Column(
        modifier = Modifier
            .clickable {
                try {
                    device.connectGatt(context, false, object : BluetoothGattCallback() {
                        override fun onConnectionStateChange(
                            gatt: BluetoothGatt?,
                            status: Int,
                            newState: Int
                        ) {
                            super.onConnectionStateChange(gatt, status, newState)
                            if (newState == BluetoothProfile.STATE_CONNECTED) {
                                gatt?.disconnect()
                                (context as? PairingActivity)?.finishWithResult(device)
                            }
                        }
                    })?.connect()
                } catch (ioe: IOException) {
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar("An error occurred: $ioe")
                    }
                }
            }
            .padding(16.dp)
            .then(modifier)
    ) {
        Text(
            device.name ?: device.address,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(4.dp))
        if (device.name != null) {
            Text(
                device.address,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.alpha(.5f)
            )
        }
    }
}