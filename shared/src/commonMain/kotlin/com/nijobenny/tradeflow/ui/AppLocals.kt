package com.nijobenny.tradeflow.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.nijobenny.tradeflow.core.CustomerDto
import com.nijobenny.tradeflow.core.LiveEventDto
import com.nijobenny.tradeflow.core.ProductDto
import com.nijobenny.tradeflow.data.TradeFlowApi
import com.nijobenny.tradeflow.platform.currentTimeMillis

/** Screens pushed above the tabs. */
sealed interface Route {
    data class OrderDetail(val id: Int) : Route
    data class NewOrder(val customerId: Int? = null) : Route
    data class ProductDetail(val id: Int) : Route
    data class ProductForm(val product: ProductDto? = null) : Route
    data class CustomerDetail(val id: Int) : Route
    data class CustomerForm(val customer: CustomerDto? = null) : Route
    data class InvoiceDetail(val id: Int) : Route
    data object Invoices : Route
    data object Team : Route
    data object Activity : Route
    data object Search : Route
    data object Settings : Route
}

/** A tiny back stack: no navigation library needed, and it works the same on Android and iOS. */
class Navigator {
    val stack = mutableStateListOf<Route>()

    val top: Route? get() = stack.lastOrNull()

    fun push(route: Route) {
        stack.add(route)
    }

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    /** Replaces the top screen, e.g. "New order" → the order it created. */
    fun replace(route: Route) {
        pop()
        push(route)
    }

    fun clear() {
        stack.clear()
    }
}

enum class ToastKind { Success, Error, Info, Live }

data class Toast(
    val id: Long,
    val message: String,
    val kind: ToastKind,
    val event: LiveEventDto? = null,
)

/** One banner at a time at the top of the screen. */
class ToastHost {
    var current by mutableStateOf<Toast?>(null)
        private set

    fun success(message: String) = show(message, ToastKind.Success)
    fun error(message: String) = show(message, ToastKind.Error)
    fun info(message: String) = show(message, ToastKind.Info)
    fun live(event: LiveEventDto) {
        current = Toast(currentTimeMillis(), event.message, ToastKind.Live, event)
    }

    fun dismiss() {
        current = null
    }

    private fun show(message: String, kind: ToastKind) {
        current = Toast(currentTimeMillis(), message, kind)
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("Navigator not provided") }
val LocalSession = staticCompositionLocalOf<SessionViewModel> { error("Session not provided") }
val LocalApi = staticCompositionLocalOf<TradeFlowApi> { error("Api not provided") }
val LocalToast = staticCompositionLocalOf<ToastHost> { error("Toast host not provided") }
