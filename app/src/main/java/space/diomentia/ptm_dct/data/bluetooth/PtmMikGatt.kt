package space.diomentia.ptm_dct.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beepiz.bluetooth.gattcoroutines.ExperimentalBleGattCoroutinesCoroutinesApi
import com.beepiz.bluetooth.gattcoroutines.GattConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.diomentia.ptm_dct.cancelWithQueue
import space.diomentia.ptm_dct.queueJob
import space.diomentia.ptm_dct.runQueue
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
@OptIn(ExperimentalBleGattCoroutinesCoroutinesApi::class)
class PtmMikGatt(private val device: BluetoothDevice) {
    companion object {
        val SERVICE_BATTERY: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val CHAR_BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val SERVICE_MEASUREMENT: UUID = UUID.fromString("00cfb9c8-4851-4807-b88d-9d963460a815")
        val CHAR_VOLTAGE: UUID = UUID.fromString("00002b18-0000-1000-8000-00805f9b34fb")
    }

    private val mJobQueue = Channel<Job>(
        capacity = 32,
        onBufferOverflow = BufferOverflow.DROP_LATEST
    )
    private val mCoroutineScope = CoroutineScope(Dispatchers.IO)

    private var mGatt: GattConnection = GattConnection(
        device,
        connectionSettings = GattConnection.ConnectionSettings(autoConnect = true)
    )

    var isConnected by mutableStateOf(mGatt.isConnected)
        private set
    var batteryLevel by mutableIntStateOf(0)
        private set
    var voltage by mutableFloatStateOf(0.0f)
        private set

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

    private fun characteristicCallback(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        val buffer = ByteBuffer.allocate(32).order(ByteOrder.LITTLE_ENDIAN).put(value)
        when (characteristic.service.uuid) {
            SERVICE_BATTERY -> {
                when (characteristic.uuid) {
                    CHAR_BATTERY_LEVEL ->
                        batteryLevel = buffer.getInt(0)
                }
            }
            SERVICE_MEASUREMENT -> {
                when (characteristic.uuid) {
                    CHAR_VOLTAGE ->
                        voltage = buffer.getFloat(0)
                }
            }
        }
    }

    private fun readCharacteristic(service: UUID, characteristic: UUID) {
        val char = mGatt.getService(service)?.getCharacteristic(characteristic) ?: return
        mCoroutineScope.queueJob(mJobQueue) {
            mGatt.readCharacteristic(char)
            characteristicCallback(char, char.value)
        }
    }

    private fun toggleNotifications(service: UUID, characteristic: UUID, enabled: Boolean) {
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

    fun updateBatteryLevel() = readCharacteristic(SERVICE_BATTERY, CHAR_BATTERY_LEVEL)
    fun listenBatteryLevel(enable: Boolean = true) {
        updateBatteryLevel()
        toggleNotifications(SERVICE_BATTERY, CHAR_BATTERY_LEVEL, enable)
    }

    fun updateVoltage() = readCharacteristic(SERVICE_MEASUREMENT, CHAR_VOLTAGE)
    fun listenVoltage(enable: Boolean = true) {
        updateVoltage()
        toggleNotifications(SERVICE_MEASUREMENT, CHAR_VOLTAGE, enable)
    }
}