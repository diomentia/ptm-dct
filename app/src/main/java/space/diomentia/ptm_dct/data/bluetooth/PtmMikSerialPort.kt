package space.diomentia.ptm_dct.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beepiz.bluetooth.gattcoroutines.ExperimentalBleGattCoroutinesCoroutinesApi
import com.beepiz.bluetooth.gattcoroutines.OperationFailedException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import space.diomentia.ptm_dct.await
import space.diomentia.ptm_dct.data.mik.MikAuth
import space.diomentia.ptm_dct.data.mik.MikJournalEntry
import space.diomentia.ptm_dct.data.mik.MikState
import space.diomentia.ptm_dct.data.mik.MikStatus
import space.diomentia.ptm_dct.queueJob
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
        GetJournal("GetJournal."),
        ClearJournal("ClearJournal."),
        Connection("")
        ;

        override fun toString(): String = command

        fun withArgs(vararg args: String): String =
            if (hasArgs || args.isEmpty())
                when (this) {
                    Setup -> command.format(args.joinToString(", "))
                    else -> if (args.isNotEmpty())
                        command.format(args.joinToString(" "))
                    else
                        command
                }
            else
                throw IllegalArgumentException("This command cannot have any arguments")
    }


    companion object {
        const val MTU = 512
        const val MAX_READ_WAIT = 1000L
        const val UPDATE_PERIOD = 500L

        val SERVICE_BATTERY: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val CHAR_BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
        val SERVICE_DATA: UUID = UUID.fromString("0000FFF0-0000-1000-8000-00805f9b34fb")
        val CHAR_READ: UUID = UUID.fromString("0000FFF1-0000-1000-8000-00805f9b34fb")
        val CHAR_WRITE: UUID = UUID.fromString("0000FFF2-0000-1000-8000-00805f9b34fb")
    }

    val mikState = MikState()

    var hasLastCommandSucceeded by mutableStateOf<Pair<Command?, Boolean>>(null to true)

    private val readData = Channel<String>(Channel.UNLIMITED)
    private var updater: Job? = null
    override var isConnected: Boolean
        get() = super.isConnected
        set(value) {
            super.isConnected = value
            mikState.isConnected = value
        }

    override fun connect() {
        super.connect()
        mCoroutineScope.queueJob(mJobQueue) {
            try {
                mGatt.requestMtu(MTU)
            } catch (ofe: OperationFailedException) {
                hasLastCommandSucceeded = Command.Connection to false
                Log.e("MikConnection", ofe.toString())
            }
            toggleNotifications(SERVICE_DATA, CHAR_READ, true)
            delay(2000L)
        }
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
                        mikState.batteryLevel = buffer.getInt(MTU - Int.SIZE_BYTES)
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
            Command.Connection -> throw IllegalStateException()
            Command.Authentication -> MikAuth.parse(value)?.also { mikState.authInfo = it } != null
            Command.GetStatus -> MikStatus.parse(value)?.also { mikState.statusInfo = it } != null
            Command.GetSetup -> {
                mikState.setupInfo = value; true
            }

            Command.GetJournal -> MikJournalEntry.parse(value)?.also { mikState.journal.add(it) } != null
            Command.SetDateTime, Command.Setup, Command.ClearJournal -> value
                .trim()
                .lowercase() == "ok"
        }
        if (hasLastCommandSucceeded.second) {
            Log.i("MikCommand", "${command.name}: $value")
        } else {
            Log.e("MikCommand", "${command.name}: $value")
        }
    }

    fun sendCommand(command: Command, vararg args: String): Job {
        when (command) {
            Command.GetJournal -> {
                mikState.endJournalReading = false
                mikState.journal.clear()
            }
            else -> Unit
        }
        return mCoroutineScope.queueJob(mJobQueue) {
            if (!mGatt.isConnected) {
                return@queueJob
            }
            writeCharacteristic(
                SERVICE_DATA,
                CHAR_WRITE,
                command.withArgs(*args).toByteArray()
            )
            suspend fun reader(dataValidation: (String) -> Boolean = { true }): Boolean? =
                withTimeoutOrNull(MAX_READ_WAIT) {
                    val data = readData.receive()
                    if (!dataValidation(data))
                        return@withTimeoutOrNull false
                    commandCallback(command, data)
                    return@withTimeoutOrNull true
                }.also {
                    if (it == null) {
                        Log.w("MikCommand", "Timeout")
                        hasLastCommandSucceeded = command to false
                    }
                }
            when (command) {
                Command.GetJournal -> {
                    var fails = 0
                    var entryNumber = 0
                    when (
                        reader { value ->
                            Regex("""journal entries=(\d+)""").find(value).let {
                                entryNumber = it?.groupValues?.get(1)?.toInt() ?: 0
                                return@reader it == null
                            }
                        }
                    ) {
                        true -> {
                            do {
                                when (reader { !it.contains("EndJournal") }) {
                                    false -> break
                                    null -> ++fails
                                    else -> Unit
                                }
                            } while (fails <= 3)
                        }

                        false -> {
                            Log.i(
                                "MikCommand",
                                "${Command.GetJournal}: number of entries = $entryNumber"
                            )
                            repeat(entryNumber) {
                                if (reader() == null) {
                                    ++fails
                                }
                            }
                            reader { false }
                        }

                        null -> ++fails
                    }
                    mikState.endJournalReading = true
                    if (fails > 0) {
                        sendCommand(Command.ClearJournal)
                    } else {
                        hasLastCommandSucceeded = Command.ClearJournal to false
                    }
                }

                else -> reader()
            }
        }
    }

    fun fetchBatteryLevel() = mCoroutineScope.queueJob(mJobQueue) {
        readCharacteristic(
            SERVICE_BATTERY,
            CHAR_BATTERY_LEVEL
        )
    }

    fun listenBatteryLevel(enable: Boolean = true) {
        fetchBatteryLevel()
        mCoroutineScope.queueJob(mJobQueue) {
            toggleNotifications(SERVICE_BATTERY, CHAR_BATTERY_LEVEL, enable)
        }
    }

    fun updateStatus(enable: Boolean = true) {
        if (!enable) {
            updater?.cancel()
            updater = null
            return
        }
        updater = mCoroutineScope.launch {
            while (true) {
                sendCommand(Command.GetStatus).await()
                delay(UPDATE_PERIOD)
            }
        }
    }

    fun setDateTime(dt: LocalDateTime) = sendCommand(
        Command.SetDateTime,
        dt.format(
            DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss")
        )
    )
}