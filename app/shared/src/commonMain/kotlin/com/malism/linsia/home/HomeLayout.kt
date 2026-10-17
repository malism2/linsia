package com.malism.linsia.home

import kotlinx.serialization.Serializable

@Serializable
data class HomeLayout(
    val orderedIds: List<String> = emptyList(),
    val hiddenIds: List<String> = emptyList(),
)

fun resolveHomeLayout(
    catalog: List<AppFeature>,
    saved: HomeLayout?,
): HomeLayout {
    val catalogIds = catalog.map { it.id }
    val known = catalogIds.toSet()
    if (saved == null) {
        return HomeLayout(
            orderedIds = catalogIds,
            hiddenIds = catalog.filterNot { it.defaultVisible }.map { it.id },
        )
    }
    val savedOrder = saved.orderedIds.filter { it in known }
    val newIds = catalogIds.filter { it !in saved.orderedIds.toSet() }
    val hidden = saved.hiddenIds.filter { it in known }.toMutableSet()
    catalog.filter { it.id in newIds.toSet() && !it.defaultVisible }.forEach { hidden += it.id }
    return HomeLayout(
        orderedIds = savedOrder + newIds,
        hiddenIds = catalogIds.filter { it in hidden },
    )
}

fun HomeLayout.visibleFeatures(catalog: List<AppFeature>): List<AppFeature> {
    val hidden = hiddenIds.toSet()
    val byId = catalog.associateBy { it.id }
    return orderedIds.mapNotNull { byId[it] }.filter { it.id !in hidden }
}

fun HomeLayout.managedFeatures(catalog: List<AppFeature>): List<ManagedFeature> {
    val hidden = hiddenIds.toSet()
    val byId = catalog.associateBy { it.id }
    val features = orderedIds.mapNotNull { byId[it] }
    return features.mapIndexed { index, feature ->
        ManagedFeature(
            feature = feature,
            visible = feature.id !in hidden,
            canMoveUp = index > 0,
            canMoveDown = index < features.lastIndex,
        )
    }
}

data class ManagedFeature(
    val feature: AppFeature,
    val visible: Boolean,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean,
)
