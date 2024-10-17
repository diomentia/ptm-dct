package space.diomentia.ptm_dct.data.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import space.diomentia.ptm_dct.queueJob
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.max

class PtmMikGatt(private val device: BluetoothDevice) : PtmGattInterface(device) {
    companion object {
        val SERVICE_BATTERY: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val CHAR_BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val SERVICE_MEASUREMENT: UUID = UUID.fromString("00cfb9c8-4851-4807-b88d-9d963460a815")
        val CHAR_VOLTAGE: UUID = UUID.fromString("00002b18-0000-1000-8000-00805f9b34fb")
    }

    var batteryLevel by mutableIntStateOf(0)
        private set
    var voltage by mutableFloatStateOf(0.0f)
        private set

    override fun characteristicCallback(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        val buffer = ByteBuffer
            .wrap(value.copyInto(ByteArray(max(64, value.size))))
            .order(ByteOrder.LITTLE_ENDIAN)
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

    fun fetchBatteryLevel() = mCoroutineScope.queueJob(mJobQueue) {
        readCharacteristic(SERVICE_BATTERY, CHAR_BATTERY_LEVEL)
    }
    fun listenBatteryLevel(enable: Boolean = true) {
        fetchBatteryLevel()
        mCoroutineScope.queueJob(mJobQueue) {
            toggleNotifications(SERVICE_BATTERY, CHAR_BATTERY_LEVEL, enable)
        }
    }

    fun fetchVoltage() = mCoroutineScope.queueJob(mJobQueue) {
        readCharacteristic(SERVICE_MEASUREMENT, CHAR_VOLTAGE)
    }
    fun listenVoltage(enable: Boolean = true) {
        fetchVoltage()
        mCoroutineScope.queueJob(mJobQueue) {
            toggleNotifications(SERVICE_MEASUREMENT, CHAR_VOLTAGE, enable)
        }
    }
}