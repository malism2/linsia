package com.malism.linsia.learnword.data.db

import android.content.Context

object AndroidAppHolder {
    @Volatile
    var application: Context? = null

    fun init(context: Context) {
        application = context.applicationContext
    }

    fun require(): Context =
        application ?: error("AndroidAppHolder not initialized")
}
