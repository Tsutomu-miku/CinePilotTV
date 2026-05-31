package tv.cinepilot.tv.runtime

import android.content.Context
import android.content.SharedPreferences
import tv.cinepilot.core.protocol.AuthenticatedServer

class RecentAccountStore(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun accounts(): List<RecentAccount> {
        val recentAccounts = preferences.getString(RECENT_ACCOUNTS_KEY, null)
            ?.lineSequence()
            ?.mapNotNull(RecentAccount::deserialize)
            ?.toList()
            .orEmpty()
        if (recentAccounts.isNotEmpty()) {
            return recentAccounts
        }
        return legacyAccount()?.let(::listOf).orEmpty()
    }

    fun remember(authenticated: AuthenticatedServer) {
        val account = RecentAccount(
            authenticated.server().address().value(),
            authenticated.server().serverName(),
            authenticated.session().userId(),
        )
        val accounts = (listOf(account) + accounts().filterNot {
            it.serverAddress == account.serverAddress && it.userId == account.userId
        }).take(MAX_RECENT_ACCOUNTS)
        preferences.edit()
            .putString(RECENT_ACCOUNTS_KEY, accounts.joinToString("\n") { it.serialize() })
            .putString(LEGACY_SERVER_ADDRESS_KEY, account.serverAddress)
            .putString(LEGACY_SERVER_NAME_KEY, account.serverName)
            .putString(LEGACY_USER_ID_KEY, account.userId)
            .apply()
    }

    fun forget(authenticated: AuthenticatedServer) {
        val account = RecentAccount(
            authenticated.server().address().value(),
            authenticated.server().serverName(),
            authenticated.session().userId(),
        )
        val accounts = accounts().filterNot {
            it.serverAddress == account.serverAddress && it.userId == account.userId
        }
        preferences.edit()
            .putString(RECENT_ACCOUNTS_KEY, accounts.joinToString("\n") { it.serialize() })
            .remove(LEGACY_SERVER_ADDRESS_KEY)
            .remove(LEGACY_SERVER_NAME_KEY)
            .remove(LEGACY_USER_ID_KEY)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun legacyAccount(): RecentAccount? {
        val serverAddress = preferences.getString(LEGACY_SERVER_ADDRESS_KEY, null)?.takeIf { it.isNotBlank() }
        val userId = preferences.getString(LEGACY_USER_ID_KEY, null)?.takeIf { it.isNotBlank() }
        if (serverAddress == null || userId == null) {
            return null
        }
        return RecentAccount(
            serverAddress,
            preferences.getString(LEGACY_SERVER_NAME_KEY, null).orEmpty(),
            userId,
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "cinepilot_last_account"
        const val RECENT_ACCOUNTS_KEY = "recent_accounts"
        const val LEGACY_SERVER_ADDRESS_KEY = "server_address"
        const val LEGACY_SERVER_NAME_KEY = "server_name"
        const val LEGACY_USER_ID_KEY = "user_id"
        const val MAX_RECENT_ACCOUNTS = 5
    }
}

data class RecentAccount(
    val serverAddress: String,
    val serverName: String,
    val userId: String,
) {
    fun displayName(): String {
        val serverLabel = serverName.ifBlank { serverAddress }
        return "$serverLabel / $userId"
    }

    fun serialize(): String {
        return listOf(serverAddress, serverName, userId).joinToString("\t", transform = ::safeField)
    }

    companion object {
        fun deserialize(value: String): RecentAccount? {
            val fields = value.split('\t')
            if (fields.size != 3 || fields[0].isBlank() || fields[2].isBlank()) {
                return null
            }
            return RecentAccount(fields[0], fields[1], fields[2])
        }

        private fun safeField(value: String): String {
            return value.replace('\t', ' ').replace('\n', ' ').trim()
        }
    }
}
