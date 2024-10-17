package space.diomentia.ptm_dct.data.mik

import android.util.Log
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class MikAuth(
    val serialNumber: Int,
    val firmwareVersion: String,
    val dateOfManufacture: LocalDate
) {
    companion object {
        fun parse(raw: String): MikAuth? {
            val regex = Regex(
                """
                |Num:(\d+),
                | Ver PO (\d+(?:\.\d+)+),
                | Date of manufacture  (\d+\.\d+\.\d\d\d\d)
                """.trimMargin().replace("\n", "")
            )
            return try {
                val matches = regex
                    .find(raw)
                    ?.groupValues
                    ?: throw IllegalArgumentException("Cannot parse Auth: \"$raw\"")
                MikAuth(
                    serialNumber = matches[1].toInt(),
                    firmwareVersion = matches[2],
                    dateOfManufacture = LocalDate.parse(
                        matches[3],
                        DateTimeFormatter.ofPattern("dd.MM.yyyy")
                    )
                )
            } catch (e: Exception) {
                Log.e("ParseMikAuth", e.toString())
                null
            }
        }
    }
}


data class MikStatus(
    val timestamp: LocalDateTime,
    val isDoorOpen: Boolean,
    val battery: Float,
    val controllerTemperature: Int,
    val voltage: Array<Float>
) {
    companion object {
        fun parse(raw: String): MikStatus? {
            val regex = Regex(
                """
                |Date (\d+-\d+-\d\d\d\d),
                | Time (\d+:\d+:\d+),
                | door ([01]),
                | Bat=(\d+\.\d+)V,
                | Temp=(\d+),
                | ((?:\d+(?:\.\d+)? ?mV,? ?)+)
                """.trimMargin().replace("\n", "")
            )
            val voltageRegex = Regex("""(\d+(?:\.\d+)?) mV""")
            return try {
                val matches = regex
                    .find(raw)
                    ?.groupValues
                    ?: throw IllegalArgumentException("Cannot parse Status: \"$raw\"")
                val date = matches[1].split("-").map { it.toInt() }
                val time = matches[2].split(":").map { it.toInt() }
                MikStatus(
                    // TODO normal date-time format
                    // localDateTime = LocalDateTime.parse("${matches[1]} ${matches[2]}"),
                    timestamp = LocalDateTime.of(
                        date[2], date[1], date[0],
                        time[0], time[1], time[2]
                    ),
                    isDoorOpen = matches[3] == "1",
                    battery = matches[4].toFloat(),
                    controllerTemperature = matches[5].toInt(),
                    voltage = voltageRegex.findAll(matches[6]).map {
                        it.groupValues[1].toFloat()
                    }.toList().toTypedArray()
                )
            } catch (e: Exception) {
                Log.e("ParseMikStatus", e.toString())
                null
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MikStatus
        if (timestamp != other.timestamp) return false
        if (isDoorOpen != other.isDoorOpen) return false
        if (battery != other.battery) return false
        if (controllerTemperature != other.controllerTemperature) return false
        if (!voltage.contentEquals(other.voltage)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + isDoorOpen.hashCode()
        result = 31 * result + battery.hashCode()
        result = 31 * result + controllerTemperature
        result = 31 * result + voltage.contentHashCode()
        return result
    }
}


data class MikJournalEntry(
    val timestamp: LocalDateTime,
    val battery: Float,
    val controllerTemperature: Int,
    val voltage: Array<Float>
) : Serializable {
    companion object {
        fun parse(raw: String): MikJournalEntry? {
            val regex = Regex(
                (if (raw.startsWith("jote"))
                    """
                    |jote (?:\d+-\d+-\d+)
                    | TDate (\d+-\d+-\d\d\d\d)
                    | Time (\d+:\d+:\d+),
                    | Bat (\d+\.\d+)V,
                    | Temp (\d+),
                    | ((?:\d+(?:\.\d+)? ?mV,? ?)+)
                    """
                else
                    """
                    |Date (\d+-\d+-\d\d\d\d),
                    | Time (\d+:\d+:\d+),
                    | Bat=(\d+\.\d+)V,
                    | Temp=(\d+),
                    | ((?:\d+(?:\.\d+)? ?mV,? ?)+)
                    """
                        ).trimMargin().replace("\n", "")
            )
            val voltageRegex = Regex("""(\d+(?:\.\d+)?) ?mV""")
            return try {
                val matches = regex
                    .find(raw)
                    ?.groupValues
                    ?: throw IllegalArgumentException("Cannot parse JournalEntry: \"$raw\"")
                val date = matches[1].split("-").map { it.toInt() }
                val time = matches[2].split(":").map { it.toInt() }
                MikJournalEntry(
                    // TODO normal date-time format
                    // localDateTime = LocalDateTime.parse("${matches[1]} ${matches[2]}"),
                    timestamp = LocalDateTime.of(
                        date[2], date[1], date[0],
                        time[0], time[1], time[2]
                    ),
                    battery = matches[3].toFloat(),
                    controllerTemperature = matches[4].toInt(),
                    voltage = voltageRegex.findAll(matches[5]).map {
                        it.groupValues[1].toFloat()
                    }.toList().toTypedArray()
                )
            } catch (e: Exception) {
                Log.e("ParseMikJournalEntry", e.toString())
                null
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MikJournalEntry
        if (timestamp != other.timestamp) return false
        if (battery != other.battery) return false
        if (controllerTemperature != other.controllerTemperature) return false
        if (!voltage.contentEquals(other.voltage)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + battery.hashCode()
        result = 31 * result + controllerTemperature
        result = 31 * result + voltage.contentHashCode()
        return result
    }
}