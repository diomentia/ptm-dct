package space.diomentia.ptm_dct.data.bluetooth

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import space.diomentia.ptm_dct.data.mik.MikAuth
import space.diomentia.ptm_dct.data.mik.MikJournalEntry
import space.diomentia.ptm_dct.data.mik.MikStatus

@Stable
class MikState {
    var isConnected by mutableStateOf(false)
    var batteryLevel by mutableIntStateOf(0)
    var authInfo by mutableStateOf<MikAuth?>(null)
    var statusInfo by mutableStateOf<MikStatus?>(null)
    var setupInfo by mutableStateOf<String?>(null)
    val journal = mutableStateListOf<MikJournalEntry>()
}