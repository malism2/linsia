package com.malism.linsia.newsread.data

import com.malism.linsia.newsread.data.db.ArticleDao
import com.malism.linsia.newsread.data.db.ArticleEntity
import com.malism.linsia.newsread.data.parse.RssItem
import com.malism.linsia.newsread.data.parse.parseArticleHtml
import com.malism.linsia.newsread.data.parse.parseListingHtml
import com.malism.linsia.newsread.domain.NewsArticle
import com.malism.linsia.newsread.domain.NewsCategory
import com.malism.linsia.newsread.domain.NewsReadDefaults
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class NewsReadRepository(
    private val client: HttpClient,
    private val dao: ArticleDao,
) {
    fun observeArticles(categoryId: String): Flow<List<NewsArticle>> =
        if (NewsReadDefaults.isFavorites(categoryId)) {
            dao.observeFavorites().map { rows -> rows.map { it.toDomain() } }
        } else {
            dao.observeByCategory(categoryId).map { rows -> rows.map { it.toDomain() } }
        }

    fun observeArticle(id: String): Flow<NewsArticle?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun toggleFavorite(id: String) {
        val existing = dao.getById(id) ?: return
        val favorited = !existing.favorited
        val favoritedAt = if (favorited) Clock.System.now().toEpochMilliseconds() else 0L
        dao.setFavorite(id, favorited, favoritedAt)
    }

    suspend fun refresh(category: NewsCategory) {
        val items = fetchFeed(category)
        if (items.isEmpty()) error("没有获取到文章，请检查网络后重试")
        val now = Clock.System.now().toEpochMilliseconds()
        val merged = items.map { item ->
            val existing = dao.getById(item.id)
            ArticleEntity(
                id = item.id,
                categoryId = category.id,
                title = item.title,
                summary = item.summary,
                publishedAt = item.publishedAt,
                publishedEpoch = item.publishedEpoch,
                sourceUrl = item.sourceUrl,
                body = existing?.body.orEmpty(),
                audioUrl = item.audioUrl ?: existing?.audioUrl,
                cachedAt = existing?.cachedAt ?: now,
                favorited = existing?.favorited == true,
                favoritedAt = existing?.favoritedAt ?: 0L,
            )
        }
        dao.upsertAll(merged)
    }

    suspend fun ensureBody(id: String) {
        val existing = dao.getById(id) ?: return
        if (existing.body.isNotBlank()) return
        val html = fetchText(existing.sourceUrl)
        val parsed = parseArticleHtml(html)
        dao.upsertAll(
            listOf(
                existing.copy(
                    body = parsed.body.ifBlank { existing.body },
                    audioUrl = existing.audioUrl ?: parsed.audioUrl,
                    cachedAt = Clock.System.now().toEpochMilliseconds(),
                ),
            ),
        )
    }

    private suspend fun fetchFeed(category: NewsCategory): List<RssItem> {
        val html = fetchText(listingUrl(category.zoneId))
        val items = parseListingHtml(html)
        if (items.isEmpty()) error("没有获取到文章，请检查网络后重试")
        return items
    }

    private suspend fun fetchText(url: String): String {
        val response = client.get(url) {
            header(HttpHeaders.UserAgent, UserAgent)
            header(HttpHeaders.Accept, "text/html, application/rss+xml, application/xml, text/xml, */*")
        }
        if (!response.status.isSuccess()) {
            error("请求失败 HTTP ${response.status.value}")
        }
        return response.bodyAsText()
    }

    companion object {
        private const val UserAgent =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

        fun listingUrl(path: String): String =
            "https://www.21voa.com/${path.trim('/')}"

        fun categoryById(id: String): NewsCategory? =
            NewsReadDefaults.categories.firstOrNull { it.id == id }
    }
}

private fun ArticleEntity.toDomain() = NewsArticle(
    id = id,
    categoryId = categoryId,
    title = title,
    summary = summary,
    publishedAt = publishedAt,
    publishedEpoch = publishedEpoch,
    sourceUrl = sourceUrl,
    body = body,
    audioUrl = audioUrl,
    favorited = favorited,
)
