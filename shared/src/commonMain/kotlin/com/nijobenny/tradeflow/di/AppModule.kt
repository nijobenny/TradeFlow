package com.nijobenny.tradeflow.di

import com.nijobenny.tradeflow.data.SessionStore
import com.nijobenny.tradeflow.data.TradeFlowApi
import com.nijobenny.tradeflow.data.createPreferencesDataStore
import com.nijobenny.tradeflow.platform.dataStoreFilePath
import com.nijobenny.tradeflow.ui.SessionViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val dataModule = module {
    single { createPreferencesDataStore(dataStoreFilePath()) }
    single { SessionStore(get()) }
    single { TradeFlowApi() }
}

val uiModule = module {
    viewModel { SessionViewModel(get(), get()) }
}

private var koinStarted = false

/** Starts Koin once per process (from Android's MainActivity and iOS's MainViewController). */
fun initKoin() {
    if (koinStarted) return
    startKoin { modules(dataModule, uiModule) }
    koinStarted = true
}
