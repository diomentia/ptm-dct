package space.diomentia.ptm_dct.data.mik

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class MikState {
    var isConnected by mutableStateOf(false)
    var endJournalReading by mutableStateOf(false)
    var batteryLevel by mutableIntStateOf(0)
    var authInfo by mutableStateOf<MikAuth?>(null)
    var statusInfo by mutableStateOf<MikStatus?>(null)
    var setupInfo by mutableStateOf<String?>(null)
    val journal = mutableStateListOf<MikJournalEntry>()
}