package space.diomentia.ptm_dct.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beepiz.bluetooth.gattcoroutines.ExperimentalBleGattCoroutinesCoroutinesApi
import space.diomentia.ptm_dct.queueJob
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@SuppressLint("MissingPermission")
@OptIn(ExperimentalBleGattCoroutinesCoroutinesApi::class)
class PtmMikSerialPort(private val device: BluetoothDevice) : PtmGattInterface(device) {
    companion object {
        val SERVICE_BATTERY: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val CHAR_BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val SERVICE_READ: UUID = UUID.randomUUID()
        val CHAR_READ: UUID = UUID.randomUUID()
        val SERVICE_WRITE: UUID = UUID.randomUUID()
        val CHAR_WRITE: UUID = UUID.randomUUID()
    }

    private val readAttr = mGatt.getService(SERVICE_READ)?.getCharacteristic(CHAR_READ)
    private val writeAttr = mGatt.getService(SERVICE_WRITE)?.getCharacteristic(CHAR_WRITE)

    var batteryLevel by mutableIntStateOf(0)
        private set
    var authInfo by mutableStateOf("null")
        private set
    var stateInfo by mutableStateOf("null")
        private set

    override fun characteristicCallback(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        val buffer = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN).put(value)
        when (characteristic.service.uuid) {
            SERVICE_BATTERY -> {
                when (characteristic.uuid) {
                    CHAR_BATTERY_LEVEL ->
                        batteryLevel = buffer.getInt(0)
                }
            }
        }
    }
    fun readCharacteristic(callback: (ByteArray) -> Unit) {
        readAttr ?: return
        mCoroutineScope.queueJob(mJobQueue) {
            mGatt.readCharacteristic(readAttr)
            callback(readAttr.value)
        }
    }
    fun writeCharacteristic(value: ByteArray) {
        writeAttr ?: return
        mCoroutineScope.queueJob(mJobQueue) {
            writeAttr.value = value
            mGatt.writeCharacteristic(writeAttr)
        }
    }

    fun fetchBatteryLevel() = readCharacteristic(
        SERVICE_BATTERY,
        CHAR_BATTERY_LEVEL
    )
    fun listenBatteryLevel(enable: Boolean = true) {
        fetchBatteryLevel()
        toggleNotifications(SERVICE_BATTERY, CHAR_BATTERY_LEVEL, enable)
    }

    fun authenticate() {
        writeCharacteristic("Authentication".toByteArray())
        readCharacteristic {
            authInfo = String(it)
        }
    }

    fun fetchState() {
        writeCharacteristic("Get Status 0".toByteArray())
        readCharacteristic {
            stateInfo = String(it)
        }
    }
}