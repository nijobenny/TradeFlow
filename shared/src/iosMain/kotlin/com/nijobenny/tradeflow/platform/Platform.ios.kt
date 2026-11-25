package com.nijobenny.tradeflow.platform

import androidx.compose.runtime.Composable
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSince1970

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

@OptIn(ExperimentalForeignApi::class)
actual fun dataStoreFilePath(): String {
    val documents = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documents?.path) + "/tradeflow.preferences_pb"
}

@Composable
actual fun SystemBarsEffect(useDarkIcons: Boolean) {
    // Compose Multiplatform on iOS follows the app's appearance automatically.
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS uses on-screen back buttons.
}

/** The iOS simulator shares the Mac's network, so localhost reaches the server. */
actual val defaultServerUrl: String = "http://localhost:8080"
