package space.diomentia.ptm_dct.data

import android.bluetooth.BluetoothAdapter
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import space.diomentia.ptm_dct.data.bluetooth.PtmMikGatt

val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }
val LocalStep = compositionLocalOf { mutableStateOf(Session.Step.Password) }
val LocalBtAdapter = staticCompositionLocalOf<BluetoothAdapter?> { null }
val LocalGattConnection = staticCompositionLocalOf<PtmMikGatt?> { null }