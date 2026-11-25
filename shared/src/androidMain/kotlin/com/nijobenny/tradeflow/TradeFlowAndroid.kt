package com.nijobenny.tradeflow

import android.content.Context
import com.nijobenny.tradeflow.di.initKoin
import com.nijobenny.tradeflow.platform.AndroidContextHolder

/** Call once from MainActivity before showing [App]. */
fun initTradeFlow(context: Context) {
    AndroidContextHolder.init(context)
    initKoin()
}
