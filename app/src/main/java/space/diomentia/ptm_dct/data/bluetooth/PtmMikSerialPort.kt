package space.diomentia.ptm_dct.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beepiz.bluetooth.gattcoroutines.ExperimentalBleGattCoroutinesCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import space.diomentia.ptm_dct.data.mik.MikAuth
import space.diomentia.ptm_dct.data.mik.MikJournalEntry
import space.diomentia.ptm_dct.data.mik.MikStatus
import space.diomentia.ptm_dct.queueJob
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID


@SuppressLint("MissingPermission")
@OptIn(ExperimentalBleGattCoroutinesCoroutinesApi::class)
class PtmMikSerialPort(device: BluetoothDevice) : PtmGattInterface(device) {
    enum class Command(private val command: String, private val hasArgs: Boolean = false) {
        Authentication("Authentication."),
        GetStatus("GetStatus."),
        SetDateTime("SetDateTime %s.", true),
        Setup("Setup %s.", true),
        GetSetup("GetSetup."),
        GetJournal("GetJournal.")
        ;

        override fun toString(): String = command

        fun withArgs(vararg args: String): String =
            if (hasArgs)
                when (this) {
                    Setup -> command.format(args.joinToString(", "))
                    else -> command.format(args.joinToString(" "))
                }
            else
                throw IllegalArgumentException("This command cannot have any arguments")
    }

    companion object {
        const val MTU: Int = 512
        const val MAX_READ_WAIT: Long = 2000L

        val SERVICE_BATTERY: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val CHAR_BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val SERVICE_DATA: UUID = UUID.fromString("0000FFF0-0000-1000-8000-00805f9b34fb")
        val CHAR_READ: UUID = UUID.fromString("0000FFF1-0000-1000-8000-00805f9b34fb")
        val CHAR_WRITE: UUID = UUID.fromString("0000FFF2-0000-1000-8000-00805f9b34fb")
    }


    var batteryLevel by mutableIntStateOf(0)
        private set
    var authInfo by mutableStateOf<MikAuth?>(null)
        private set
    var statusInfo by mutableStateOf<MikStatus?>(null)
        private set
    var setupInfo by mutableStateOf<String?>(null)
    var hasLastCommandSucceeded by mutableStateOf<Pair<Command?, Boolean>>(null to true)
        private set
    val journal = mutableStateListOf<MikJournalEntry>()

    private val readData = Channel<String>(Channel.UNLIMITED)

    override fun connect() {
        super.connect()
        mCoroutineScope.queueJob(mJobQueue) {
            mGatt.requestMtu(MTU)
        }
        toggleNotifications(SERVICE_DATA, CHAR_READ, true)
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

    private fun commandCallback(
        command: Command,
        value: String
    ) {
        hasLastCommandSucceeded = command to when (command) {
            Command.Authentication -> MikAuth.parse(value).also { authInfo = it } != null
            Command.GetStatus -> MikStatus.parse(value).also { statusInfo = it } != null
            Command.SetDateTime, Command.Setup -> value == "Ok"
            Command.GetSetup -> { setupInfo = value; true }
            Command.GetJournal -> MikJournalEntry.parse(value)?.also { journal.add(it) } != null
        }
    }

    fun sendCommand(command: Command) {
        when (command) {
            Command.GetJournal -> journal.clear()
            else -> Unit
        }
        mCoroutineScope.queueJob(mJobQueue) {
            writeCharacteristic(
                SERVICE_DATA,
                CHAR_WRITE,
                command.toString().toByteArray()
            )
            mCoroutineScope.queueJob(mJobQueue) receiveData@{
                if (!isConnected)
                    return@receiveData
                var data = ""
                val reader = suspend {
                    withTimeoutOrNull(MAX_READ_WAIT) {
                        data = readData.receive()
                        commandCallback(command, data)
                        true
                    }.also {
                        if (it == null) {
                            hasLastCommandSucceeded = command to false
                        }
                    }
                }
                when (command) {
                    Command.GetJournal ->
                        while (!data.contains("EndJournal")) {
                            if (reader() == null)
                                return@receiveData
                        }
                    else -> reader()
                }
            }
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
}