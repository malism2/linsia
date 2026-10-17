package com.malism.linsia.core.platform

import android.content.Context

object AndroidAppContext {
    @Volatile
    private var application: Context? = null

    fun init(context: Context) {
        application = context.applicationContext
    }

    fun require(): Context =
        application ?: error("AndroidAppContext not initialized")
}
