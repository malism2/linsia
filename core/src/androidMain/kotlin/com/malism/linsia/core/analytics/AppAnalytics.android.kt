package com.malism.linsia.core.analytics

/**
 * Android Firebase Crashlytics / Analytics are on the classpath
 * (see core androidMain deps). Wire FirebaseApp in Application before
 * replacing these no-ops with real SDK calls.
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
