package com.malism.linsia.dict.data

import kotlin.test.Test
import kotlin.test.assertEquals

class SanitizeDownloadUrlTest {
    @Test
    fun stripsDoubleHttps() {
        assertEquals(
            "https://objectstorage.example.com/n/x/b/y/o/z.zip",
            sanitizeDownloadUrl("https://https://objectstorage.example.com/n/x/b/y/o/z.zip"),
        )
    }

    @Test
    fun leavesNormalUrl() {
        assertEquals(
            "https://example.com/a.zip",
            sanitizeDownloadUrl("https://example.com/a.zip"),
        )
    }
}
