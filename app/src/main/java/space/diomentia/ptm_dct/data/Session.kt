package space.diomentia.ptm_dct.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Session {
    enum class AccessLevel(val code: String) {
        Guest("guest"),
        Admin("admin")
    }

    var userPassword by mutableStateOf("")
    val userLevel: AccessLevel
        // TODO! better password system
        get() = if (userPassword == "0000") AccessLevel.Admin else AccessLevel.Guest
    var rfidTag by mutableStateOf<RfidController.RfidTag?>(null)
}