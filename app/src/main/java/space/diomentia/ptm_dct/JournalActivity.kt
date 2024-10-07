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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import space.diomentia.ptm_dct.data.LocalSnackbarHostState
import space.diomentia.ptm_dct.data.mik.MikJournalEntry
import space.diomentia.ptm_dct.ui.PtmSnackbarHost
import space.diomentia.ptm_dct.ui.PtmTopBar
import space.diomentia.ptm_dct.ui.setupEdgeToEdge
import space.diomentia.ptm_dct.ui.theme.PtmTheme
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class JournalActivity : ComponentActivity() {
    companion object {
        const val EXTRA_JOURNAL = "journal"

        val LocalJournal = staticCompositionLocalOf<Array<MikJournalEntry>> { arrayOf() }
    }

    private lateinit var mJournal: Array<MikJournalEntry>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mJournal = IntentCompat
            .getSerializableExtra(
                intent,
                EXTRA_JOURNAL,
                Array<MikJournalEntry>::class.java
            )!!
        setupEdgeToEdge(activity = this)
        val snackbarHostState = SnackbarHostState()
        setContent {
            PtmTheme {
                CompositionLocalProvider(
                    LocalSnackbarHostState provides snackbarHostState,
                    LocalJournal provides mJournal
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
    val journal = JournalActivity.LocalJournal.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = padding.calculateStartPadding(LocalLayoutDirection.current),
                end = padding.calculateEndPadding(LocalLayoutDirection.current)
            )
            .then(modifier)
    ) {
        item { Spacer(Modifier.height(padding.calculateTopPadding())) }

        journal.forEachIndexed { i, entry ->
            item {
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
            .padding(16.dp)
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
            Text(
                journalEntry.voltage.joinToString { "%.2f".format(it) },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "${journalEntry.battery}V, ${journalEntry.controllerTemperature}°",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = .5f)
            )
        }
    }
}