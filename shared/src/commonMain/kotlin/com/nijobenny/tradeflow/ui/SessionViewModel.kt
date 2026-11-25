package com.nijobenny.tradeflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nijobenny.tradeflow.core.CompanyDto
import com.nijobenny.tradeflow.core.HealthDto
import com.nijobenny.tradeflow.core.LiveEventDto
import com.nijobenny.tradeflow.core.UserDto
import com.nijobenny.tradeflow.data.ApiException
import com.nijobenny.tradeflow.data.SessionStore
import com.nijobenny.tradeflow.data.TradeFlowApi
import com.nijobenny.tradeflow.platform.defaultServerUrl
import com.nijobenny.tradeflow.ui.theme.AccentTheme
import com.nijobenny.tradeflow.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionState(
    val ready: Boolean = false,
    val serverUrl: String = "",
    val user: UserDto? = null,
    val company: CompanyDto? = null,
    val themeMode: ThemeMode = ThemeMode.Dark,
    val accent: AccentTheme = AccentTheme.Amber,
    val liveConnected: Boolean = false,
)

/**
 * App-wide state: who is signed in, which server, appearance, and the live WebSocket feed.
 * Screens observe [refreshTick] to reload when something changes anywhere in the company.
 */
class SessionViewModel(
    val api: TradeFlowApi,
    private val store: SessionStore,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _refreshTick = MutableStateFlow(0)
    val refreshTick: StateFlow<Int> = _refreshTick.asStateFlow()

    private val _liveEvents = MutableSharedFlow<LiveEventDto>(extraBufferCapacity = 16)
    val liveEvents: SharedFlow<LiveEventDto> = _liveEvents.asSharedFlow()

    private var liveJob: Job? = null

    init {
        api.onUnauthorized = { logout() }
        viewModelScope.launch {
            val saved = store.load()
            val server = saved.serverUrl ?: defaultServerUrl
            api.baseUrl = server
            api.token = saved.token
            _state.value = SessionState(
                ready = true,
                serverUrl = server,
                user = if (saved.token != null) saved.user else null,
                company = saved.company,
                themeMode = saved.themeMode,
                accent = saved.accent,
            )
            if (saved.token != null && saved.user != null) {
                startLive()
                // Refresh the profile in the background (role may have changed).
                runCatching { api.me() }.onSuccess { user ->
                    _state.update { it.copy(user = user) }
                    store.saveUser(user)
                }
            }
        }
    }

    /** Pings `/api/health` on [server] to show the database version before signing in. */
    suspend fun testServer(server: String): HealthDto = api.health(normalize(server))

    /** Signs in; throws [ApiException] with a readable message on failure. */
    suspend fun login(server: String, email: String, password: String) {
        val url = normalize(server)
        api.baseUrl = url
        store.saveServer(url)
        val auth = api.login(email.trim(), password)
        api.token = auth.token
        store.saveLogin(url, auth.token, auth.user, auth.company)
        _state.update { it.copy(serverUrl = url, user = auth.user, company = auth.company) }
        startLive()
    }

    fun logout() {
        liveJob?.cancel()
        api.token = null
        _state.update { it.copy(user = null, liveConnected = false) }
        viewModelScope.launch { store.clearLogin() }
    }

    fun setThemeMode(mode: ThemeMode) {
        _state.update { it.copy(themeMode = mode) }
        viewModelScope.launch { store.saveTheme(mode) }
    }

    fun setAccent(accent: AccentTheme) {
        _state.update { it.copy(accent = accent) }
        viewModelScope.launch { store.saveAccent(accent) }
    }

    /** Asks every visible screen to refresh (after a local change). */
    fun refreshAll() {
        _refreshTick.update { it + 1 }
    }

    private fun startLive() {
        liveJob?.cancel()
        liveJob = viewModelScope.launch {
            api.liveEvents { connected -> _state.update { it.copy(liveConnected = connected) } }
                .collect { event ->
                    _liveEvents.tryEmit(event)
                    refreshAll()
                }
        }
    }

    private fun normalize(server: String): String {
        val trimmed = server.trim().trimEnd('/')
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
    }

    override fun onCleared() {
        liveJob?.cancel()
    }
}
