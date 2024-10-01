package space.diomentia.ptm_dct.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beepiz.bluetooth.gattcoroutines.ExperimentalBleGattCoroutinesCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.diomentia.ptm_dct.queueJob
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList
import java.util.Queue
import java.util.UUID

@SuppressLint("MissingPermission")
@OptIn(ExperimentalBleGattCoroutinesCoroutinesApi::class)
class PtmMikSerialPort(private val device: BluetoothDevice) : PtmGattInterface(device) {
    enum class Command(private val command: String) {
        GetStatus("GetStatus."),
        Authentication("Authentication.")
        ;

        override fun toString(): String {
            return command
        }
    }

    companion object {
        val MTU: Int = 512

        val SERVICE_BATTERY: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val CHAR_BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val SERVICE_DATA: UUID = UUID.fromString("0000FFF0-0000-1000-8000-00805f9b34fb")
        val CHAR_READ: UUID = UUID.fromString("0000FFF1-0000-1000-8000-00805f9b34fb")
        val CHAR_WRITE: UUID = UUID.fromString("0000FFF2-0000-1000-8000-00805f9b34fb")
    }

    var batteryLevel by mutableIntStateOf(0)
        private set
    var authInfo by mutableStateOf("null")
        private set
    var statusInfo by mutableStateOf("null")
        private set

    private var readJobTime: Long = 0L
    private var readJobs: Queue<Job> = LinkedList()
        set(value) {
            field = value
            readJobTime = 0L
        }
    private val readData = Channel<String>(Channel.UNLIMITED)

    override fun connect() {
        super.connect()
        mCoroutineScope.queueJob(mJobQueue) {
            mGatt.requestMtu(MTU)
        }
        enableCallbacks()
        listenBatteryLevel()
    }

    override fun characteristicCallback(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ) {
        val buffer = ByteBuffer.allocate(MTU).order(ByteOrder.LITTLE_ENDIAN).put(value)
        when (characteristic.service.uuid) {
            SERVICE_BATTERY -> {
                when (characteristic.uuid) {
                    CHAR_BATTERY_LEVEL ->
                        batteryLevel = buffer.getInt(MTU - Int.SIZE_BYTES)
                }
            }
        }
        if (characteristic.service.uuid == SERVICE_DATA && characteristic.uuid == CHAR_READ) {
            val data = String(value)
            readData.trySend(data)
        }
    }
    fun commandCallback(
        command: Command,
        value: String
    ) {
        when (command) {
            Command.GetStatus -> statusInfo = value
            Command.Authentication -> authInfo = value
        }
    }

    init {
        mCoroutineScope.launch {
            while (true) {
                if (readJobTime >= 1000L) {
                    readJobs.poll()?.cancel()
                }
                if (readJobs.peek()?.isActive == true) {
                    readJobTime += 1000L
                }
                if (!isConnected && readJobs.peek()?.isActive == true) {
                    readJobs.poll()?.cancel()
                }
                delay(1000L)
            }
        }
    }

    fun waitForData(command: Command) {
        readJobs.add(mCoroutineScope.queueJob(mJobQueue) {
            val data = readData.receive()
            commandCallback(command, data)
        })
    }
    fun enableCallbacks(enable: Boolean = true) {
        mCoroutineScope.queueJob(mJobQueue) {
            val readAttr = mGatt.getService(SERVICE_DATA)?.getCharacteristic(CHAR_READ) ?: return@queueJob
            mGatt.setCharacteristicNotificationsEnabledOnRemoteDevice(readAttr, enable)
            mCoroutineScope.launch {
                mGatt.notifications(readAttr).collect { characteristicCallback(readAttr, readAttr.value) }
            }
        }
    }
    fun sendCommand(command: Command) {
        mCoroutineScope.queueJob(mJobQueue) {
            val writeAttr = mGatt.getService(SERVICE_DATA)?.getCharacteristic(CHAR_WRITE) ?: return@queueJob
            writeAttr.value = command.toString().toByteArray()
            mGatt.writeCharacteristic(writeAttr)
        }
        waitForData(command)
    }

    fun fetchBatteryLevel() = readCharacteristic(
        SERVICE_BATTERY,
        CHAR_BATTERY_LEVEL
    )
    fun listenBatteryLevel(enable: Boolean = true) {
        fetchBatteryLevel()
        toggleNotifications(SERVICE_BATTERY, CHAR_BATTERY_LEVEL, enable)
    }
}