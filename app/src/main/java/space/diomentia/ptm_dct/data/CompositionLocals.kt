package space.diomentia.ptm_dct.data

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf
import com.ubx.usdk.rfid.RfidManager

val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> {
    SnackbarHostState()
}

val LocalRfidManager = compositionLocalOf<RfidManager?> {
    null
}