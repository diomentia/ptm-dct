package space.diomentia.ptm_dct.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Session {
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

    var userPassword by mutableStateOf("")
    val userLevel: AccessLevel
        get() = AccessLevel.checkPasswordLevel(userPassword)
    var rfidTag by mutableStateOf<RfidController.RfidTag?>(null)
}