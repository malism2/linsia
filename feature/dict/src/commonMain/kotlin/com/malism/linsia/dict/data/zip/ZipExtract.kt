package com.malism.linsia.dict.data.zip

/**
 * Extract one zip entry to [destPath] without loading the whole archive into memory.
 */
expect fun extractZipEntryToFile(
    zipPath: String,
    entryName: String,
    destPath: String,
)
