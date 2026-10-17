package com.malism.linsia.learnword.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class PhoneticFormatTest {
    @Test
    fun wrapsBareIpaWithSlashes() {
        assertEquals("/'ækses/", formatIpa("'ækses"))
    }

    @Test
    fun doesNotDoubleWrap() {
        assertEquals("/əˈbændən/", formatIpa("/əˈbændən/"))
        assertEquals("/əˈbændən/", formatIpa("[əˈbændən]"))
    }

    @Test
    fun blankStaysEmpty() {
        assertEquals("", formatIpa(null))
        assertEquals("", formatIpa("  "))
    }
}
