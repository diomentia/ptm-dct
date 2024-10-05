package space.diomentia.ptm_dct.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
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
import kotlinx.coroutines.withTimeoutOrNull
import space.diomentia.ptm_dct.cancelWithQueue
import space.diomentia.ptm_dct.queueJob
import space.diomentia.ptm_dct.runQueue
import java.util.UUID

@SuppressLint("MissingPermission")
@OptIn(ExperimentalBleGattCoroutinesCoroutinesApi::class)
abstract class PtmGattInterface(device: BluetoothDevice) {
    companion object {
        suspend fun isAccessible(
            device: BluetoothDevice
        ): Boolean {
            if (
                device.type == BluetoothDevice.DEVICE_TYPE_LE
                || device.type == BluetoothDevice.DEVICE_TYPE_DUAL
            ) {
                val gatt = GattConnection(device)
                withTimeoutOrNull(2000L) {
                    gatt.connect()
                }
                if (gatt.isConnected) {
                    gatt.close()
                    return true
                }
            }
            return false
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
                delay(1000L)
            }
        }
        mCoroutineScope.runQueue(mJobQueue)
    }

    open fun connect() {
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
        isConnected = false
        mGatt.close()
        mCoroutineScope.cancelWithQueue(mJobQueue)
    }

    protected abstract fun characteristicCallback(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    )

    protected fun readCharacteristic(service: UUID, characteristic: UUID) {
        mCoroutineScope.queueJob(mJobQueue) {
            if (!isConnected)
                return@queueJob
            val char = mGatt.getService(service)?.getCharacteristic(characteristic) ?: return@queueJob
            mGatt.readCharacteristic(char)
            characteristicCallback(char, char.value)
        }
    }

    protected fun writeCharacteristic(service: UUID, characteristic: UUID, value: ByteArray) {
        mCoroutineScope.queueJob(mJobQueue) {
            if (!isConnected)
                return@queueJob
            val char = mGatt.getService(service)?.getCharacteristic(characteristic) ?: return@queueJob
            char.value = value
            mGatt.writeCharacteristic(char)
        }
    }

    protected fun toggleNotifications(service: UUID, characteristic: UUID, enabled: Boolean) {
        mCoroutineScope.queueJob(mJobQueue) {
            if (!isConnected)
                return@queueJob
            val char = mGatt.getService(service)?.getCharacteristic(characteristic) ?: return@queueJob
            mGatt.setCharacteristicNotificationsEnabledOnRemoteDevice(char, enabled)
            mCoroutineScope.launch {
                mGatt.notifications(char).collect { characteristicCallback(char, char.value) }
            }
        }
    }
}