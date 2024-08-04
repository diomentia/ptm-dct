package space.diomentia.ptm_dct.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Session {
    var userPassword by mutableStateOf("")
    var rfidTag by mutableStateOf<RfidController.RfidTag?>(null)
}