package com.malism.linsia.newsread.domain

data class NewsCategory(
    val id: String,
    val title: String,
    val zoneId: String,
)

data class NewsArticle(
    val id: String,
    val categoryId: String,
    val title: String,
    val summary: String,
    val publishedAt: String,
    val publishedEpoch: Long,
    val sourceUrl: String,
    val body: String,
    val audioUrl: String?,
    val favorited: Boolean = false,
)

object NewsReadDefaults {
    const val SourceName = "VOA Learning English"
    const val SourceHost = "www.21voa.com"
    const val Attribution = "VOA Learning English / 21voa.com"
    const val FavoritesId = "favorites"

    val favorites = NewsCategory(id = FavoritesId, title = "收藏", zoneId = "")

    val categories: List<NewsCategory> = listOf(
        NewsCategory(id = "asit", title = "AS IT IS", zoneId = "as_it_is.html"),
        NewsCategory(id = "tech", title = "科技报道", zoneId = "technology_report.html"),
        NewsCategory(id = "health", title = "健康报道", zoneId = "health_report.html"),
        NewsCategory(id = "arts", title = "文化艺术", zoneId = "arts_culture.html"),
        NewsCategory(id = "edu", title = "教育报道", zoneId = "education_report.html"),
    )

    val tabs: List<NewsCategory> = listOf(favorites) + categories

    fun isFavorites(id: String): Boolean = id == FavoritesId
}
