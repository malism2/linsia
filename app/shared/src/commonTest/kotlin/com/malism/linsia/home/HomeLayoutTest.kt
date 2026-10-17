package com.malism.linsia.home

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeLayoutTest {
    private val alpha = feature("alpha")
    private val beta = feature("beta")
    private val gamma = feature("gamma", defaultVisible = false)
    private val catalog = listOf(alpha, beta, gamma)

    @Test
    fun defaultLayoutKeepsCatalogOrderAndDefaultVisibility() {
        val layout = resolveHomeLayout(catalog, saved = null)
        assertEquals(listOf("alpha", "beta", "gamma"), layout.orderedIds)
        assertEquals(listOf("gamma"), layout.hiddenIds)
        assertEquals(listOf(alpha, beta), layout.visibleFeatures(catalog))
    }

    @Test
    fun unknownIdsAreDropped() {
        val saved = HomeLayout(
            orderedIds = listOf("gone", "beta", "alpha"),
            hiddenIds = listOf("gone", "beta"),
        )
        val layout = resolveHomeLayout(catalog, saved)
        assertEquals(listOf("beta", "alpha", "gamma"), layout.orderedIds)
        assertEquals(listOf("beta", "gamma"), layout.hiddenIds)
    }

    @Test
    fun newFeaturesAreAppendedAndHonorDefaultVisibility() {
        val saved = HomeLayout(
            orderedIds = listOf("alpha"),
            hiddenIds = emptyList(),
        )
        val layout = resolveHomeLayout(catalog, saved)
        assertEquals(listOf("alpha", "beta", "gamma"), layout.orderedIds)
        assertEquals(listOf("gamma"), layout.hiddenIds)
    }

    @Test
    fun hiddenSetOnlyKeepsKnownIds() {
        val saved = HomeLayout(
            orderedIds = listOf("alpha", "beta", "gamma"),
            hiddenIds = listOf("missing", "alpha"),
        )
        val layout = resolveHomeLayout(catalog, saved)
        assertEquals(listOf("alpha"), layout.hiddenIds)
        val managed = layout.managedFeatures(catalog)
        assertEquals(listOf(false, true, true), managed.map { it.visible })
        assertEquals(listOf(false, true, true), managed.map { it.canMoveUp })
        assertEquals(listOf(true, true, false), managed.map { it.canMoveDown })
    }

    private fun feature(id: String, defaultVisible: Boolean = true) = AppFeature(
        id = id,
        title = id,
        subtitle = id,
        icon = AppFeatureIcon.MenuBook,
        defaultVisible = defaultVisible,
    )
}
