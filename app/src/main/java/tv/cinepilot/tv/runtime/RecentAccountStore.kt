package tv.cinepilot.tv.runtime

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import tv.cinepilot.core.protocol.AuthenticatedServer

private val Context.recentAccountDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cinepilot_last_account_datastore",
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, RecentAccountStore.PREFERENCES_NAME))
    },
)

data class RecentAccountsState(
    val accounts: List<RecentAccount>,
    val servers: List<RecentServer>,
)

class RecentAccountStore(context: Context) {
    private val dataStore = context.applicationContext.recentAccountDataStore
    private val storeScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun accounts(): List<RecentAccount> {
        return read { preferences ->
            val recentAccounts = preferences[RECENT_ACCOUNTS_KEY]
                ?.lineSequence()
                ?.mapNotNull(RecentAccount::deserialize)
                ?.toList()
                .orEmpty()
            if (recentAccounts.isNotEmpty()) {
                recentAccounts
            } else {
                legacyAccount(preferences)?.let(::listOf).orEmpty()
            }
        }
    }

    fun servers(): List<RecentServer> {
        return read { preferences ->
            val recentServers = preferences[RECENT_SERVERS_KEY]
                ?.lineSequence()
                ?.mapNotNull(RecentServer::deserialize)
                ?.toList()
                .orEmpty()
            if (recentServers.isNotEmpty()) {
                recentServers
            } else {
                legacyAccount(preferences)?.let {
                    listOf(RecentServer(it.serverAddress, it.serverName))
                }.orEmpty()
            }
        }
    }

    fun current(): RecentAccountsState = RecentAccountsState(accounts = accounts(), servers = servers())

    val flow: Flow<RecentAccountsState> = dataStore.data.map { prefs ->
        val accts = prefs[RECENT_ACCOUNTS_KEY]
            ?.lineSequence()
            ?.mapNotNull(RecentAccount::deserialize)
            ?.toList()
            .orEmpty()
        val svrs = prefs[RECENT_SERVERS_KEY]
            ?.lineSequence()
            ?.mapNotNull(RecentServer::deserialize)
            ?.toList()
            .orEmpty()
        val finalAccounts = accts.ifEmpty {
            legacyAccount(prefs)?.let(::listOf).orEmpty()
        }
        val finalServers = svrs.ifEmpty {
            legacyAccount(prefs)?.let { listOf(RecentServer(it.serverAddress, it.serverName)) }.orEmpty()
        }
        RecentAccountsState(accounts = finalAccounts, servers = finalServers)
    }

    val stateFlow: StateFlow<RecentAccountsState> by lazy {
        flow.stateIn(storeScope, SharingStarted.Eagerly, current())
    }

    fun remember(authenticated: AuthenticatedServer) {
        val account = RecentAccount(
            authenticated.server().address().value(),
            authenticated.server().serverName(),
            authenticated.session().userId(),
        )
        rememberServer(account.serverAddress, account.serverName)
        val accounts = (listOf(account) + accounts().filterNot {
            it.serverAddress == account.serverAddress && it.userId == account.userId
        }).take(MAX_RECENT_ACCOUNTS)
        write { preferences ->
            preferences[RECENT_ACCOUNTS_KEY] = accounts.joinToString("\n") { it.serialize() }
            preferences[LEGACY_SERVER_ADDRESS_KEY] = account.serverAddress
            preferences[LEGACY_SERVER_NAME_KEY] = account.serverName
            preferences[LEGACY_USER_ID_KEY] = account.userId
        }
    }

    fun rememberServer(serverAddress: String, serverName: String) {
        if (serverAddress.isBlank()) {
            return
        }
        val server = RecentServer(serverAddress, serverName)
        val servers = (listOf(server) + servers().filterNot {
            it.serverAddress == server.serverAddress
        }).take(MAX_RECENT_SERVERS)
        write { preferences ->
            preferences[RECENT_SERVERS_KEY] = servers.joinToString("\n") { it.serialize() }
        }
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
        write { preferences ->
            preferences[RECENT_ACCOUNTS_KEY] = accounts.joinToString("\n") { it.serialize() }
            preferences.remove(LEGACY_SERVER_ADDRESS_KEY)
            preferences.remove(LEGACY_SERVER_NAME_KEY)
            preferences.remove(LEGACY_USER_ID_KEY)
        }
    }

    fun clear() {
        write { preferences -> preferences.clear() }
    }

    private fun legacyAccount(preferences: Preferences): RecentAccount? {
        val serverAddress = preferences[LEGACY_SERVER_ADDRESS_KEY]?.takeIf { it.isNotBlank() }
        val userId = preferences[LEGACY_USER_ID_KEY]?.takeIf { it.isNotBlank() }
        if (serverAddress == null || userId == null) {
            return null
        }
        return RecentAccount(
            serverAddress,
            preferences[LEGACY_SERVER_NAME_KEY].orEmpty(),
            userId,
        )
    }

    private fun <T> read(block: (Preferences) -> T): T = runBlocking(Dispatchers.IO) {
        dataStore.data.map(block).first()
    }

    private fun write(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        runBlocking(Dispatchers.IO) {
            dataStore.edit { preferences -> block(preferences) }
        }
    }

    companion object {
        const val PREFERENCES_NAME = "cinepilot_last_account"
        private val RECENT_ACCOUNTS_KEY = stringPreferencesKey("recent_accounts")
        private val RECENT_SERVERS_KEY = stringPreferencesKey("recent_servers")
        private val LEGACY_SERVER_ADDRESS_KEY = stringPreferencesKey("server_address")
        private val LEGACY_SERVER_NAME_KEY = stringPreferencesKey("server_name")
        private val LEGACY_USER_ID_KEY = stringPreferencesKey("user_id")
        const val MAX_RECENT_ACCOUNTS = 5
        const val MAX_RECENT_SERVERS = 5
    }
}

data class RecentServer(
    val serverAddress: String,
    val serverName: String,
) {
    fun displayName(): String {
        return serverName.ifBlank { serverAddress }
    }

    fun serialize(): String {
        return listOf(serverAddress, serverName).joinToString("\t", transform = ::safeField)
    }

    companion object {
        fun deserialize(value: String): RecentServer? {
            val fields = value.split('\t')
            if (fields.size != 2 || fields[0].isBlank()) {
                return null
            }
            return RecentServer(fields[0], fields[1])
        }
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

    }
}

private fun safeField(value: String): String {
    return value.replace('\t', ' ').replace('\n', ' ').trim()
}
