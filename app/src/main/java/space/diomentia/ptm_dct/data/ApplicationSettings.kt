package space.diomentia.ptm_dct.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
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

object ApplicationSettings {
    const val keyPasswordAdmin = "password_admin"

    lateinit var sharedPreferences: SharedPreferences

    var passwordAdmin: PasswordHash
        get() = PasswordHash(sharedPreferences.getString(keyPasswordAdmin, "")!!)
        set(value) {
            if (Session.userLevel != Session.AccessLevel.Admin) {
                return
            }
            with(sharedPreferences.edit()) {
                putString(keyPasswordAdmin, value.hash)
                apply()
            }
        }

    fun init(context: Context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (sharedPreferences.getString(keyPasswordAdmin, "") == "") {
            with(sharedPreferences.edit()) {
                putString(keyPasswordAdmin, PasswordHash.encrypt("0000")!!.hash)
                apply()
            }
        }
    }
}