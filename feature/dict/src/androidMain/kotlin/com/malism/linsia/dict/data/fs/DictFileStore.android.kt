package com.malism.linsia.dict.data.fs

import com.malism.linsia.core.platform.AndroidAppContext
import com.malism.linsia.dict.domain.DictDefaults
import java.io.File

actual fun dictDatabasePath(): String {
    val context = AndroidAppContext.require()
    return File(context.filesDir, DictDefaults.DbFileName).absolutePath
}
