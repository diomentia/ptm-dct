package space.diomentia.ptm_dct.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import space.diomentia.ptm_dct.data.bluetooth.MikState
import space.diomentia.ptm_dct.data.bluetooth.PtmMikSerialPort

object Session {
    enum class Step {
        Password, UserLevel, RfidManager, RfidTag, BluetoothTurnOn, BluetoothPair;

        fun next(): Step = entries[this.ordinal + 1]
    }

    enum class AccessLevel(val code: String) {
        Guest("guest"),
        Admin("admin");
        companion object {
            fun checkPasswordLevel(password: String): AccessLevel = when (PasswordHash.encrypt(password)) {
                ApplicationSettings.passwordAdmin -> Admin
                else -> Guest
            }
        }
    }

    fun updateUserLevel(password: String) {
        userLevel = AccessLevel.checkPasswordLevel(password)
    }

    var mikState: MikState? = null

    var userLevel by mutableStateOf(AccessLevel.Guest)
        private set
    var rfidTag by mutableStateOf<RfidController.RfidTag?>(null)
}