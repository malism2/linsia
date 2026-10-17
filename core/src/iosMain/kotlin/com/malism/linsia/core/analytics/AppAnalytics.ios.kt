package com.malism.linsia.core.analytics

/**
 * iOS Firebase wiring can be completed via CocoaPods / SPM.
 * No-op placeholders keep the common API usable until native SDKs are linked.
 */
actual object AppAnalytics {
    actual fun logEvent(name: String, params: Map<String, String>) = Unit

    actual fun setUserId(userId: String?) = Unit
}

actual object AppCrashReporter {
    actual fun log(message: String) = Unit

    actual fun recordException(throwable: Throwable) = Unit

    actual fun setCustomKey(key: String, value: String) = Unit
}
