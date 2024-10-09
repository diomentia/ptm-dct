package space.diomentia.ptm_dct

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import space.diomentia.ptm_dct.data.LocalMikState
import space.diomentia.ptm_dct.data.LocalSnackbarHostState
import space.diomentia.ptm_dct.data.Session
import space.diomentia.ptm_dct.data.mik.MikState
import space.diomentia.ptm_dct.data.mik.MikJournalEntry
import space.diomentia.ptm_dct.ui.PtmSnackbarHost
import space.diomentia.ptm_dct.ui.PtmTopBar
import space.diomentia.ptm_dct.ui.setupEdgeToEdge
import space.diomentia.ptm_dct.ui.theme.PtmTheme
import space.diomentia.ptm_dct.ui.theme.white
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class JournalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge(activity = this)
        val snackbarHostState = SnackbarHostState()
        setContent {
            PtmTheme {
                CompositionLocalProvider(
                    LocalSnackbarHostState provides snackbarHostState,
                    LocalMikState provides (Session.mikState ?: MikState())
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            PtmTopBar(
                                navigation = {
                                    IconButton(onClick = {
                                        setResult(RESULT_CANCELED)
                                        finish()
                                    }) {
                                        Icon(
                                            Icons.AutoMirrored.Default.ArrowBack,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp),
                                            contentDescription = stringResource(R.string.back)
                                        )
                                    }
                                },
                                title = {
                                    Text(stringResource(R.string.journal))
                                }
                            )
                        },
                        snackbarHost = { PtmSnackbarHost(snackbarHostState) }
                    ) { innerPadding ->
                        Contents(padding = innerPadding)
                    }
                }
            }
        }
    }
}

@Composable
private fun Contents(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(0.dp)
) {
    val mik = LocalMikState.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = padding.calculateStartPadding(LocalLayoutDirection.current) + 8.dp,
                end = padding.calculateEndPadding(LocalLayoutDirection.current) + 8.dp
            )
            .then(modifier)
    ) {
        item { Spacer(Modifier.height(padding.calculateTopPadding() + 16.dp)) }

        mik.journal.fastForEachIndexed { i, entry ->
            item {
                if (i > 0) {
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
                JournalListEntry(i + 1, entry, Modifier.fillMaxWidth())
            }
        }

        item { Spacer(Modifier.height(padding.calculateBottomPadding())) }
    }
}

@Composable
private fun JournalListEntry(
    index: Int,
    journalEntry: MikJournalEntry,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .then(modifier)
    ) {
        Text(
            "$index.",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(8.dp)
        )
        Column(
            modifier = Modifier
                .padding(8.dp)
        ) {
            Text(
                journalEntry.timestamp.format(
                    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                ),
                style = MaterialTheme.typography.labelLarge
            )
            Row {
                Text(
                    buildAnnotatedString {
                        append(
                            journalEntry
                                .voltage
                                .joinToString(separator = ", ") { "%.2fmV".format(it) }
                        )
                        withStyle(SpanStyle(color = white.copy(alpha = .5f))) {
                            append(
                                ", ${journalEntry.battery}V, ${journalEntry.controllerTemperature}°"
                            )
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}