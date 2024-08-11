package space.diomentia.ptm_dct

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import space.diomentia.ptm_dct.ui.theme.blue_zodiac

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(blue_zodiac.copy(alpha = .25f).toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
        )
        throw NotImplementedError()
        /*
        setContent {
            PtmDctTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        PtmTopBar(
                            title = {
                                Text(
                                    resources.getString(R.string.settings),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        )
                    }
                ) { innerPadding ->
                }
            }
        }
        */
    }
}