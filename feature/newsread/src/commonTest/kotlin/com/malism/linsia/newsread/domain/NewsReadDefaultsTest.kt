package com.malism.linsia.newsread.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NewsReadDefaultsTest {
    @Test
    fun tabsLeadWithFavorites() {
        assertEquals(NewsReadDefaults.FavoritesId, NewsReadDefaults.tabs.first().id)
        assertEquals("收藏", NewsReadDefaults.favorites.title)
        assertTrue(NewsReadDefaults.isFavorites(NewsReadDefaults.FavoritesId))
        assertFalse(NewsReadDefaults.isFavorites("asit"))
    }
}
