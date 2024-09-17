package space.diomentia.ptm_dct.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beepiz.bluetooth.gattcoroutines.ExperimentalBleGattCoroutinesCoroutinesApi
import com.beepiz.bluetooth.gattcoroutines.GattConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.diomentia.ptm_dct.cancelWithQueue
import space.diomentia.ptm_dct.queueJob
import space.diomentia.ptm_dct.runQueue
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
@OptIn(ExperimentalBleGattCoroutinesCoroutinesApi::class)
abstract class PtmGattInterface(private val device: BluetoothDevice) {
    companion object {
        fun checkIfAccessible(
            context: Context,
            device: BluetoothDevice,
            callback: (Boolean) -> Unit
        ) {
            try {
                device.connectGatt(context, false, object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(
                        gatt: BluetoothGatt?,
                        status: Int,
                        newState: Int
                    ) {
                        super.onConnectionStateChange(gatt, status, newState)
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            callback(true)
                            gatt?.close()
                        }
                    }
                })?.connect()
            } catch (ioe: IOException) {
                Log.e(ioe::class.java.name, ioe.toString())
            }
        }
    }

    protected val mJobQueue = Channel<Job>(
        capacity = 32,
        onBufferOverflow = BufferOverflow.DROP_LATEST
    )
    protected val mCoroutineScope = CoroutineScope(Dispatchers.IO)

    protected var mGatt: GattConnection = GattConnection(
        device,
        connectionSettings = GattConnection.ConnectionSettings(autoConnect = true)
    )

    var isConnected by mutableStateOf(mGatt.isConnected)
        protected set

    init {
        mCoroutineScope.launch {
            while (true) {
                if (isConnected != mGatt.isConnected) {
                    isConnected = mGatt.isConnected
                }
                delay(5000L)
            }
        }
        mCoroutineScope.runQueue(mJobQueue)
    }

    fun connect() {
        mCoroutineScope.queueJob(mJobQueue) {
            if (!mGatt.isConnected) {
                mGatt.connect()
                mGatt.discoverServices()
                isConnected = true
            }
        }
    }
    fun disconnect() {
        mCoroutineScope.queueJob(mJobQueue) {
            if (mGatt.isConnected) {
                mGatt.disconnect()
                isConnected = false
            }
        }
    }
    fun cancel() {
        mGatt.close()
        mCoroutineScope.cancelWithQueue(mJobQueue)
    }

    protected abstract fun characteristicCallback(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    )

    protected fun readCharacteristic(service: UUID, characteristic: UUID) {
        val char = mGatt.getService(service)?.getCharacteristic(characteristic) ?: return
        mCoroutineScope.queueJob(mJobQueue) {
            mGatt.readCharacteristic(char)
            characteristicCallback(char, char.value)
        }
    }

    protected fun writeCharacteristic(service: UUID, characteristic: UUID, value: ByteArray) {
        val char = mGatt.getService(service)?.getCharacteristic(characteristic) ?: return
        mCoroutineScope.queueJob(mJobQueue) {
            char.value = value
            mGatt.writeCharacteristic(char)
        }
    }

    protected fun toggleNotifications(service: UUID, characteristic: UUID, enabled: Boolean) {
        val char = mGatt.getService(service)?.getCharacteristic(characteristic) ?: return
        mCoroutineScope.queueJob(mJobQueue) {
            mGatt.readCharacteristic(char)
            characteristicCallback(char, char.value)
            mGatt.setCharacteristicNotificationsEnabledOnRemoteDevice(char, true)
        }
        mCoroutineScope.launch {
            mGatt.notifications(char).collect { characteristicCallback(char, char.value) }
        }
    }
}