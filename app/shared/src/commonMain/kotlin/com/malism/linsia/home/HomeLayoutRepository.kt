package com.malism.linsia.home

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

class HomeLayoutRepository(
    private val settings: Settings,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun load(catalog: List<AppFeature> = AppFeatureCatalog.all): HomeLayout {
        val raw = settings.getStringOrNull(KEY) ?: return resolveHomeLayout(catalog, null)
        val saved = runCatching { json.decodeFromString(HomeLayout.serializer(), raw) }.getOrNull()
        return resolveHomeLayout(catalog, saved)
    }

    fun save(layout: HomeLayout) {
        settings.putString(KEY, json.encodeToString(HomeLayout.serializer(), layout))
    }

    companion object {
        const val KEY = "app.home.layout"
    }
}
