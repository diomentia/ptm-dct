package space.diomentia.ptm_dct.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.security.MessageDigest


class PasswordHash(val hash: String) {
    companion object {
        fun checkPassword(password: String): Boolean = Regex(
            "^[\\w!@#\$%^&*()\\-=+/,.\"';:?`~\\[\\]{}]+\$"
        ).matches(password)

        fun encrypt(password: String): PasswordHash? {
            if (!checkPassword(password)) return null
            val md = MessageDigest.getInstance("SHA-512")
            val digest = md.digest(password.toByteArray())
            val sb = StringBuilder()
            for (b in digest) {
                sb.append(((b.toInt() and 0xff) + 0x100).toString(16).substring(1))
            }
            return PasswordHash(sb.toString())
        }
    }

    init {
        if (!hash.matches(Regex("^[0-9a-fA-F]+$"))) {
            throw IllegalArgumentException("password hash is not a hexadecimal")
        }
    }

    override fun hashCode(): Int {
        return hash.hashCode()
    }

    override operator fun equals(other: Any?): Boolean {
        return when (other) {
            is PasswordHash -> hash == other.hash
            else -> super.equals(other)
        }
    }
}


object ApplicationPreferences {
    class ApplicationPreferencesException : IllegalArgumentException() {}

    val keyPasswordAdmin = stringPreferencesKey("password_admin")
    val keyEnableRfid = booleanPreferencesKey("enable_rfid")
    val keyDemoPassport = stringPreferencesKey("demo_passport")
    val keyCommandTimeout = longPreferencesKey("command_timeout")

    private val Context.preferences: DataStore<Preferences> by preferencesDataStore(name = "preferences")

    @OptIn(DelicateCoroutinesApi::class)
    @Composable
    private fun <P, T> rememberPreference(
        key: Preferences.Key<P>,
        decoder: (P) -> T,
        encoder: (T) -> P,
        defaultValue: P
    ): MutableState<T> {
        val context = LocalContext.current
        val state by remember {
            context.preferences.data
                .map {
                    it[key] ?: defaultValue
                }
        }.collectAsState(initial = defaultValue)
        return remember {
            object : MutableState<T> {
                override var value: T
                    get() = decoder(state)
                    set(value) {
                        GlobalScope.launch {
                            context.preferences.edit {
                                try {
                                    it[key] = encoder(value)
                                } catch (_: ApplicationPreferencesException) {
                                }
                            }
                        }
                    }

                override fun component1() = value
                override fun component2(): (T) -> Unit = { value = it }
            }
        }
    }

    @Composable
    fun <T> rememberPreference(
        key: Preferences.Key<T>,
        defaultValue: T
    ) = rememberPreference(key, { it }, { it }, defaultValue)

    private val defaultPasswordAdmin = PasswordHash.encrypt("0000")!!.hash
    @Composable
    fun rememberPasswordAdmin() = rememberPreference(
        key = keyPasswordAdmin,
        defaultValue = defaultPasswordAdmin,
        decoder = { PasswordHash(it) },
        encoder = { if (Session.userLevel == Session.AccessLevel.Admin) it.hash else throw ApplicationPreferencesException() }
    )
    suspend fun getPasswordAdmin(context: Context) =
        PasswordHash(context.preferences.data.first()[keyPasswordAdmin] ?: defaultPasswordAdmin)

    @Composable
    fun rememberEnableRfid() = rememberPreference(
        key = keyEnableRfid,
        defaultValue = true
    )

    @Composable
    fun rememberDemoPassport() = rememberPreference(
        key = keyDemoPassport,
        defaultValue = "null",
        decoder = { if (it != "null") Uri.parse(it) else null },
        encoder = { it.toString() }
    )

    @Composable
    fun rememberCommandTimeout() = rememberPreference(
        key = keyCommandTimeout,
        defaultValue = 1000L
    )
    suspend fun getCommandTimeout(context: Context) =
        context.preferences.data.first()[keyCommandTimeout] ?: 1000L
}