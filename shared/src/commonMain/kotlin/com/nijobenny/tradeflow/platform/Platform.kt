package com.nijobenny.tradeflow.platform

import androidx.compose.runtime.Composable

/** Current time in epoch milliseconds. */
expect fun currentTimeMillis(): Long

/** Absolute path of the DataStore preferences file for this platform. */
expect fun dataStoreFilePath(): String

/** Makes status/navigation bar icons dark on light themes and light on dark themes. */
@Composable
expect fun SystemBarsEffect(useDarkIcons: Boolean)

/** System back button / gesture (Android). No-op on iOS. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

/** Server address the app suggests on first launch. */
expect val defaultServerUrl: String
