package space.diomentia.ptm_dct.data

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import space.diomentia.ptm_dct.data.mik.MikState

@Stable
object Session {
    enum class Step {
        Password, UserLevel, RfidManager, RfidTag, BluetoothTurnOn, BluetoothPair;

        fun next(): Step = entries[this.ordinal + 1]
    }

    enum class AccessLevel(val code: String) {
        Guest("guest"),
        Admin("admin");
        companion object {
            suspend fun checkPasswordLevel(context: Context, password: String): AccessLevel {
                val passwordAdmin = ApplicationPreferences.getPasswordAdmin(context)
                return when (PasswordHash.encrypt(password)) {
                    passwordAdmin -> Admin
                    else -> Guest
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun updateUserLevel(context: Context, password: String) {
        GlobalScope.launch {
            userLevel = AccessLevel.checkPasswordLevel(context, password)
        }
    }
    fun resetUserLevel() {
        userLevel = AccessLevel.Guest
    }

    var mikState: MikState? = null

    var userLevel by mutableStateOf(AccessLevel.Guest)
        private set
    var rfidTag by mutableStateOf<RfidController.RfidTag?>(null)
}