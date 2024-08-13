package space.diomentia.ptm_dct.data

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSnackbarHostState = staticCompositionLocalOf {
    SnackbarHostState()
}

val LocalStep = compositionLocalOf { mutableStateOf(Step.Password) }