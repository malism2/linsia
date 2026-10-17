package com.malism.linsia.core.analytics

/** Desktop: reserved no-op implementations. */
actual object AppAnalytics {
    actual fun logEvent(name: String, params: Map<String, String>) = Unit

    actual fun setUserId(userId: String?) = Unit
}

actual object AppCrashReporter {
    actual fun log(message: String) = Unit

    actual fun recordException(throwable: Throwable) = Unit

    actual fun setCustomKey(key: String, value: String) = Unit
}
