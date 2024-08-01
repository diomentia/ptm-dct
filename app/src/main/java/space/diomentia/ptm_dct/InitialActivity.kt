package space.diomentia.ptm_dct

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import space.diomentia.ptm_dct.data.LocalSnackbarHostState
import space.diomentia.ptm_dct.ui.PtmTopBar
import space.diomentia.ptm_dct.ui.SideArrowContainer
import space.diomentia.ptm_dct.ui.SquareOutlinedButton
import space.diomentia.ptm_dct.ui.theme.PtmDctTheme
import space.diomentia.ptm_dct.ui.theme.blue_zodiac

class InitialActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(blue_zodiac.copy(alpha = .25f).toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
        )
        setContent {
            PtmDctTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            PtmTopBar(
                                title = {
                                    Text(
                                        resources.getString(R.string.app_name),
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            )
                        },
                        floatingActionButton = {
                            StartScanButton()
                        },
                        floatingActionButtonPosition = FabPosition.Center,
                        snackbarHost = { SnackbarHost(snackbarHostState) {
                            Snackbar(
                                it,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        } }
                    ) { innerPadding ->
                        Contents(Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StartScanButton(
    modifier: Modifier = Modifier
) {
    var enabled by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackbarHostState.current
    SquareOutlinedButton(
        roundedCorners = true,
        enabled = enabled,
        modifier = Modifier
            .combinedClickable(
                enabled = enabled,
                onClickLabel = stringResource(R.string.button_start_rfid_search)
            ) { /* TODO */
                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar("yippie")
                }
            }
            .then(modifier)
    ) {
        Icon(
            Icons.Default.Nfc,
            contentDescription = stringResource(R.string.button_start_rfid_search),
            modifier = Modifier
                .size(64.dp)
        )
    }
}

@Composable
fun Contents(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .then(modifier),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SideArrowContainer(
            toRight = false,
            slantFactor = 4
        ) {
            Image(
                painter = painterResource(R.drawable.logo_ptm),
                contentDescription = stringResource(R.string.logo_ptm_description),
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .heightIn(min = 75.dp, max = 175.dp)
                    .fillMaxSize()
            )
        }
        // TODO: segmented button USB/Bluetooth
    }
}

@Preview(showBackground = true)
@Composable
fun InitialPreview() {
    PtmDctTheme {
        Contents()
    }
}