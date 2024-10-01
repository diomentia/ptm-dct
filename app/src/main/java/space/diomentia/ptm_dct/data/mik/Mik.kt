package space.diomentia.ptm_dct.data.mik

import java.time.LocalDateTime

data class MikStatus(
    val localDateTime: LocalDateTime,
    val isDoorOpen: Boolean,
    val battery: Float,
    val controllerTemperature: Int,
    val voltage: Array<Float>
) {
    companion object {
        fun parse(raw: String): MikStatus {
            val regex = Regex("""
                |Date (\d+-\d+-\d\d\d\d),
                | Time (\d+:\d+:\d+),
                | door ([01]),
                | Bat=(\d+\.\d+)V,
                | Temp=(\d+),
                | (?:(\d+\.\d+) mV,?)+
            """.trimMargin().replace("\n", ""))
            println(regex.pattern)
            val matches = regex
                .find(raw)
                ?.groupValues
                ?: throw IllegalArgumentException()
            return MikStatus(
                localDateTime = LocalDateTime.parse("${matches[1]} ${matches[2]}"),
                isDoorOpen = matches[3] == "1",
                battery = matches[4].toFloat(),
                controllerTemperature = matches[5].toInt(),
                voltage = matches.subList(6, matches.size).map { it.toFloat() }.toTypedArray()
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MikStatus
        if (localDateTime != other.localDateTime) return false
        if (isDoorOpen != other.isDoorOpen) return false
        if (battery != other.battery) return false
        if (controllerTemperature != other.controllerTemperature) return false
        if (!voltage.contentEquals(other.voltage)) return false
        return true
    }
    override fun hashCode(): Int {
        var result = localDateTime.hashCode()
        result = 31 * result + isDoorOpen.hashCode()
        result = 31 * result + battery.hashCode()
        result = 31 * result + controllerTemperature
        result = 31 * result + voltage.contentHashCode()
        return result
    }
}