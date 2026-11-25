package com.nijobenny.tradeflow

import androidx.compose.ui.window.ComposeUIViewController
import com.nijobenny.tradeflow.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}
