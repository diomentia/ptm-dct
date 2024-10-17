package space.diomentia.ptm_dct.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
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
        const val MAX_PAIR_WAIT = 5000L

        suspend fun isAccessible(
            device: BluetoothDevice
        ): Boolean {
            if (
                device.type == BluetoothDevice.DEVICE_TYPE_LE
                || device.type == BluetoothDevice.DEVICE_TYPE_DUAL
            ) {
                val gatt = GattConnection(
                    device,
                    GattConnection.ConnectionSettings(autoConnect = true)
                )
                withTimeoutOrNull(MAX_PAIR_WAIT) {
                    gatt.connect()
                }
                if (gatt.isConnected) {
                    gatt.close()
                    return true
                }
                gatt.close()
            }
            return false
        }
    }

    protected val mJobQueue = Channel<Job>(
        capacity = 32,
        onBufferOverflow = BufferOverflow.DROP_LATEST
    )
    protected val mCoroutineScope = CoroutineScope(Dispatchers.IO)

    protected var mGatt: GattConnection = GattConnection(device)

    open var isConnected = mGatt.isConnected
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

    fun cancel() {
        isConnected = false
        mGatt.close()
        mCoroutineScope.cancelWithQueue(mJobQueue)
    }

    protected abstract fun characteristicCallback(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    )

    protected suspend fun readCharacteristic(service: UUID, characteristic: UUID) {
        if (!mGatt.isConnected)
            return
        val char =
            mGatt.getService(service)?.getCharacteristic(characteristic) ?: return
        mGatt.readCharacteristic(char)
        characteristicCallback(char, char.value)
    }

    protected suspend fun writeCharacteristic(
        service: UUID,
        characteristic: UUID,
        value: ByteArray
    ) {
        if (!mGatt.isConnected)
            return
        val char = mGatt.getService(service)?.getCharacteristic(characteristic) ?: return
        char.value = value
        mGatt.writeCharacteristic(char)
    }

    protected suspend fun toggleNotifications(service: UUID, characteristic: UUID, enabled: Boolean) {
        if (!mGatt.isConnected)
            return
        val char = mGatt.getService(service)?.getCharacteristic(characteristic) ?: return
        mGatt.setCharacteristicNotificationsEnabledOnRemoteDevice(char, enabled)
        mCoroutineScope.launch {
            mGatt.notifications(char).collect { characteristicCallback(char, char.value) }
        }
    }
}