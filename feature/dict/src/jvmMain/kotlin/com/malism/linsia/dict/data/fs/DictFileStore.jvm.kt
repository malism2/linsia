package com.malism.linsia.dict.data.fs

import com.malism.linsia.dict.domain.DictDefaults
import java.io.File

actual fun dictDatabasePath(): String {
    val dir = File(System.getProperty("user.home"), ".linsia")
    dir.mkdirs()
    return File(dir, DictDefaults.DbFileName).absolutePath
}
