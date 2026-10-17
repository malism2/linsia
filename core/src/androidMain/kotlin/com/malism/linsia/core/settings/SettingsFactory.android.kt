package com.malism.linsia.core.settings

import android.content.Context
import com.malism.linsia.core.platform.AndroidAppContext
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

actual fun createSettings(): Settings {
    val context = AndroidAppContext.require()
    val prefs = context.getSharedPreferences("linsia", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(prefs)
}
