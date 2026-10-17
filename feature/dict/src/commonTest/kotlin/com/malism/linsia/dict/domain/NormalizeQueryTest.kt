package com.malism.linsia.dict.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class NormalizeQueryTest {
    @Test
    fun trimsAndLowercases() {
        assertEquals("abandon", normalizeQuery("  Abandon  "))
    }

    @Test
    fun stripsEdgePunctuation() {
        assertEquals("hello", normalizeQuery("\"Hello\""))
        assertEquals("world", normalizeQuery("(World)."))
    }

    @Test
    fun blankBecomesEmpty() {
        assertEquals("", normalizeQuery("   "))
        assertEquals("", normalizeQuery("..."))
    }
}
