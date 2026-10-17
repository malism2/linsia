package com.malism.linsia.core.analytics

/**
 * Crash reporting and analytics facade.
 * Real Firebase wiring on Android/iOS; no-op on Desktop.
 */
expect object AppAnalytics {
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
    fun setUserId(userId: String?)
}

expect object AppCrashReporter {
    fun log(message: String)
    fun recordException(throwable: Throwable)
    fun setCustomKey(key: String, value: String)
}
