package com.malism.linsia.core.settings

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual fun createSettings(): Settings =
    NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
