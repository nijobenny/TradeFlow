package com.nijobenny.tradeflow.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.nijobenny.tradeflow.data.ApiException
import com.nijobenny.tradeflow.ui.LocalSession
import com.nijobenny.tradeflow.ui.components.collectAsStateLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Failed(val message: String) : Load<Nothing>
    data class Ready<T>(val data: T) : Load<T>
}

/**
 * Loads one API resource for a screen and keeps it fresh. Once data is on screen,
 * later reloads keep showing it (with a small refresh indicator) instead of flashing a skeleton.
 */
class Loader<T>(private val scope: CoroutineScope) {
    var state by mutableStateOf<Load<T>>(Load.Loading)
        private set
    var refreshing by mutableStateOf(false)
        private set

    /** Latest fetch lambda, updated on every recomposition so it sees current filters. */
    var fetch: suspend () -> T = { error("No fetch set") }

    private var job: Job? = null

    val data: T?
        get() = (state as? Load.Ready<T>)?.data

    fun reload(showIndicator: Boolean = true) {
        job?.cancel()
        job = scope.launch { load(showIndicator) }
    }

    suspend fun load(showIndicator: Boolean = true) {
        val hadData = state is Load.Ready
        if (!hadData) state = Load.Loading else if (showIndicator) refreshing = true
        try {
            state = Load.Ready(fetch())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Keep stale data on screen if a background refresh fails.
            if (!hadData) state = Load.Failed(e.readable())
        } finally {
            refreshing = false
        }
    }

    /** Replaces the data immediately, e.g. with the response of a successful update. */
    fun set(value: T) {
        state = Load.Ready(value)
    }
}

fun Throwable.readable(): String = when (this) {
    is ApiException -> message
    else -> message ?: "Something went wrong"
}

/**
 * Remembers a [Loader] for this screen. It loads immediately, again whenever any of [inputs]
 * change (debounced, for search boxes), and silently whenever the live feed reports a change.
 */
@Composable
fun <T> rememberLoader(vararg inputs: Any?, fetch: suspend () -> T): Loader<T> {
    val scope = rememberCoroutineScope()
    val loader = remember { Loader<T>(scope) }
    loader.fetch = fetch
    LaunchedEffect(*inputs) {
        if (loader.data != null) delay(250)
        loader.load()
    }
    val session = LocalSession.current
    val tick by session.refreshTick.collectAsStateLifecycle()
    LaunchedEffect(tick) {
        if (tick > 0 && loader.data != null) loader.load(showIndicator = false)
    }
    return loader
}
