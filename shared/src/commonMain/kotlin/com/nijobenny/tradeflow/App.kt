package com.nijobenny.tradeflow

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nijobenny.tradeflow.platform.PlatformBackHandler
import com.nijobenny.tradeflow.platform.SystemBarsEffect
import com.nijobenny.tradeflow.ui.LocalApi
import com.nijobenny.tradeflow.ui.LocalNavigator
import com.nijobenny.tradeflow.ui.LocalSession
import com.nijobenny.tradeflow.ui.LocalToast
import com.nijobenny.tradeflow.ui.Navigator
import com.nijobenny.tradeflow.ui.Route
import com.nijobenny.tradeflow.ui.SessionViewModel
import com.nijobenny.tradeflow.ui.ToastHost
import com.nijobenny.tradeflow.ui.ToastKind
import com.nijobenny.tradeflow.ui.components.RouteSurface
import com.nijobenny.tradeflow.ui.components.ToastBanner
import com.nijobenny.tradeflow.ui.components.collectAsStateLifecycle
import com.nijobenny.tradeflow.ui.customers.CustomerDetailScreen
import com.nijobenny.tradeflow.ui.customers.CustomerFormScreen
import com.nijobenny.tradeflow.ui.inventory.ProductDetailScreen
import com.nijobenny.tradeflow.ui.inventory.ProductFormScreen
import com.nijobenny.tradeflow.ui.invoices.InvoiceDetailScreen
import com.nijobenny.tradeflow.ui.invoices.InvoicesScreen
import com.nijobenny.tradeflow.ui.login.LoginScreen
import com.nijobenny.tradeflow.ui.main.MainScreen
import com.nijobenny.tradeflow.ui.more.ActivityScreen
import com.nijobenny.tradeflow.ui.more.SettingsScreen
import com.nijobenny.tradeflow.ui.more.TeamScreen
import com.nijobenny.tradeflow.ui.orders.NewOrderScreen
import com.nijobenny.tradeflow.ui.orders.OrderDetailScreen
import com.nijobenny.tradeflow.ui.search.SearchScreen
import com.nijobenny.tradeflow.ui.splash.SplashIntro
import com.nijobenny.tradeflow.ui.theme.ThemeMode
import com.nijobenny.tradeflow.ui.theme.TradeFlowTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val session: SessionViewModel = koinViewModel()
    val state by session.state.collectAsStateLifecycle()
    val navigator = remember { Navigator() }
    val toast = remember { ToastHost() }
    var showIntro by rememberSaveable { mutableStateOf(true) }

    // Live events from the WebSocket appear as banners.
    LaunchedEffect(session) {
        session.liveEvents.collect { toast.live(it) }
    }

    val darkTheme = when (state.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    SystemBarsEffect(useDarkIcons = !darkTheme && !showIntro)

    val signedIn = state.user != null
    LaunchedEffect(signedIn) { if (!signedIn) navigator.clear() }

    PlatformBackHandler(enabled = navigator.stack.isNotEmpty()) { navigator.pop() }

    CompositionLocalProvider(
        LocalSession provides session,
        LocalApi provides session.api,
        LocalNavigator provides navigator,
        LocalToast provides toast,
    ) {
        TradeFlowTheme(darkTheme = darkTheme, accent = state.accent) {
            Box(Modifier.fillMaxSize()) {
                if (state.ready) {
                    AnimatedContent(
                        targetState = signedIn,
                        transitionSpec = {
                            (fadeIn(tween(500)) + scaleIn(tween(500, easing = FastOutSlowInEasing), initialScale = 0.94f)) togetherWith
                                (fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 1.04f))
                        },
                        label = "root",
                    ) { loggedIn ->
                        if (loggedIn) MainScreen() else LoginScreen()
                    }
                }

                // Pushed screens slide over the tabs.
                AnimatedContent(
                    targetState = navigator.stack.toList(),
                    transitionSpec = {
                        val forward = targetState.size >= initialState.size
                        (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { if (forward) it else -it / 3 } + fadeIn(tween(250))) togetherWith
                            (slideOutHorizontally(tween(330, easing = FastOutSlowInEasing)) { if (forward) -it / 3 else it } + fadeOut(tween(250)))
                    },
                    label = "routes",
                ) { stack ->
                    val route = stack.lastOrNull()
                    if (route != null) {
                        RouteSurface { RouteContent(route) }
                    }
                }

                ToastBanner(
                    toast = toast.current,
                    onDismiss = { toast.dismiss() },
                    onOpen = { t ->
                        toast.dismiss()
                        val event = t.event
                        if (t.kind == ToastKind.Live && event != null) {
                            when (event.entity) {
                                "order" -> navigator.push(Route.OrderDetail(event.entityId))
                                "product" -> navigator.push(Route.ProductDetail(event.entityId))
                                "customer" -> navigator.push(Route.CustomerDetail(event.entityId))
                                "invoice" -> navigator.push(Route.InvoiceDetail(event.entityId))
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.TopCenter),
                )

                AnimatedVisibility(
                    visible = showIntro,
                    enter = fadeIn(tween(0)),
                    exit = fadeOut(tween(500)) + scaleOut(targetScale = 1.08f, animationSpec = tween(500)),
                ) {
                    SplashIntro(onFinished = { showIntro = false })
                }
            }
        }
    }
}

@Composable
private fun RouteContent(route: Route) {
    when (route) {
        is Route.OrderDetail -> OrderDetailScreen(route.id)
        is Route.NewOrder -> NewOrderScreen(route.customerId)
        is Route.ProductDetail -> ProductDetailScreen(route.id)
        is Route.ProductForm -> ProductFormScreen(route.product)
        is Route.CustomerDetail -> CustomerDetailScreen(route.id)
        is Route.CustomerForm -> CustomerFormScreen(route.customer)
        is Route.InvoiceDetail -> InvoiceDetailScreen(route.id)
        Route.Invoices -> InvoicesScreen()
        Route.Team -> TeamScreen()
        Route.Activity -> ActivityScreen()
        Route.Search -> SearchScreen()
        Route.Settings -> SettingsScreen()
    }
}
