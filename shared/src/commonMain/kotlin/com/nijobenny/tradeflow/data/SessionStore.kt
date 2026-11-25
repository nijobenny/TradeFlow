package com.nijobenny.tradeflow.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nijobenny.tradeflow.core.CompanyDto
import com.nijobenny.tradeflow.core.UserDto
import com.nijobenny.tradeflow.ui.theme.AccentTheme
import com.nijobenny.tradeflow.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath

fun createPreferencesDataStore(path: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { path.toPath() })

data class StoredSession(
    val serverUrl: String?,
    val token: String?,
    val user: UserDto?,
    val company: CompanyDto?,
    val themeMode: ThemeMode,
    val accent: AccentTheme,
)

/** Persists the session (server, token, signed-in user) and appearance settings with DataStore. */
class SessionStore(private val dataStore: DataStore<Preferences>) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val server = stringPreferencesKey("server_url")
        val token = stringPreferencesKey("token")
        val user = stringPreferencesKey("user_json")
        val company = stringPreferencesKey("company_json")
        val theme = stringPreferencesKey("theme_mode")
        val accent = stringPreferencesKey("accent")
    }

    suspend fun load(): StoredSession {
        val p = dataStore.data.first()
        return StoredSession(
            serverUrl = p[Keys.server],
            token = p[Keys.token],
            user = p[Keys.user]?.let { runCatching { json.decodeFromString(UserDto.serializer(), it) }.getOrNull() },
            company = p[Keys.company]?.let { runCatching { json.decodeFromString(CompanyDto.serializer(), it) }.getOrNull() },
            themeMode = ThemeMode.entries.firstOrNull { it.name == p[Keys.theme] } ?: ThemeMode.Dark,
            accent = AccentTheme.entries.firstOrNull { it.name == p[Keys.accent] } ?: AccentTheme.Amber,
        )
    }

    suspend fun saveLogin(serverUrl: String, token: String, user: UserDto, company: CompanyDto) {
        dataStore.edit {
            it[Keys.server] = serverUrl
            it[Keys.token] = token
            it[Keys.user] = json.encodeToString(UserDto.serializer(), user)
            it[Keys.company] = json.encodeToString(CompanyDto.serializer(), company)
        }
    }

    suspend fun saveUser(user: UserDto) {
        dataStore.edit { it[Keys.user] = json.encodeToString(UserDto.serializer(), user) }
    }

    suspend fun saveServer(serverUrl: String) {
        dataStore.edit { it[Keys.server] = serverUrl }
    }

    suspend fun clearLogin() {
        dataStore.edit {
            it.remove(Keys.token)
            it.remove(Keys.user)
            it.remove(Keys.company)
        }
    }

    suspend fun saveTheme(mode: ThemeMode) {
        dataStore.edit { it[Keys.theme] = mode.name }
    }

    suspend fun saveAccent(accent: AccentTheme) {
        dataStore.edit { it[Keys.accent] = accent.name }
    }
}
