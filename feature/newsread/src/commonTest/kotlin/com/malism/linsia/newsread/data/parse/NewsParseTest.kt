package com.malism.linsia.newsread.data.parse

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RssParserTest {
    @Test
    fun parsesItemsAndDropsUnknownLinks() {
        val items = parseRssItems(SampleRss)
        assertEquals(2, items.size)
        val first = items[0]
        assertEquals("7801234", first.id)
        assertEquals("Methods for Protecting Earth against an Asteroid Strike", first.title)
        assertEquals("Scientists are studying ways to protect Earth from asteroids.", first.summary)
        assertEquals("Aug 13, 2026", first.publishedAt)
        assertTrue(first.publishedEpoch > 0)
        assertEquals(
            "https://learningenglish.voanews.com/a/methods-for-protecting-earth/7801234.html",
            first.sourceUrl,
        )
        assertEquals("https://av.voanews.com/clips/VLE/2026/08/13/asteroid.mp3", first.audioUrl)
        assertEquals("8008342", items[1].id)
        assertNull(items[1].audioUrl)
    }

    @Test
    fun articleIdSupportsTwentyOneVoaUrls() {
        assertEquals(
            "93397",
            articleIdFromUrl("https://www.21voa.com/special_english/wilbur-and-orville-wright-the-first-airplane-93397.html"),
        )
        assertEquals("7801234", articleIdFromUrl("https://learningenglish.voanews.com/a/7801234.html"))
        assertNull(articleIdFromUrl("https://www.21voa.com/as_it_is.html"))
    }
}

class ListingParserTest {
    @Test
    fun parsesTwentyOneVoaListItems() {
        val items = parseListingHtml(SampleListingHtml)
        assertEquals(2, items.size)
        assertEquals("93397", items[0].id)
        assertEquals("Wilbur and Orville Wright: The First Airplane", items[0].title)
        assertEquals("Mar 18, 2025", items[0].publishedAt)
        assertTrue(items[0].publishedEpoch > 0)
        assertEquals(
            "https://www.21voa.com/special_english/wilbur-and-orville-wright-the-first-airplane-93397.html",
            items[0].sourceUrl,
        )
        assertEquals("93396", items[1].id)
        assertEquals("Lincoln's Cottage: a Country House for the President", items[1].title)
    }
}

class ArticleHtmlParserTest {
    @Test
    fun extractsParagraphsAndMp3FromRighterContent() {
        val parsed = parseArticleHtml(SampleArticleHtml)
        assertEquals(
            "Wilbur and Orville Wright are the American inventors who made a small engine-powered flying machine.\n\n" +
                "On December 17th, 1903, they made the world's first flight.",
            parsed.body,
        )
        assertEquals(
            "https://files.21voa.com/audio/202503/wilbur-and-orville-wright-the-first-airplane.mp3",
            parsed.audioUrl,
        )
    }

    @Test
    fun stripsTagsAndDecodesEntities() {
        val parsed = parseArticleHtml(
            """
            <div id="righter">
              <div class="content">
                <p>It&rsquo;s a &quot;test&quot; &amp; more.</p>
              </div>
            </div>
            """.trimIndent(),
        )
        assertEquals("It’s a \"test\" & more.", parsed.body)
        assertNull(parsed.audioUrl)
    }
}

private const val SampleRss = """
<rss version="2.0">
  <channel>
    <title>Science &amp; Technology</title>
    <item>
      <title>Methods for Protecting Earth against an Asteroid Strike</title>
      <link>https://learningenglish.voanews.com/a/methods-for-protecting-earth/7801234.html</link>
      <pubDate>Wed, 13 Aug 2026 01:00:00 +0000</pubDate>
      <description><![CDATA[Scientists are studying ways to protect Earth from asteroids.]]></description>
      <enclosure url="https://av.voanews.com/clips/VLE/2026/08/13/asteroid.mp3" type="audio/mpeg" />
    </item>
    <item>
      <title>Everyday Grammar Video: Ramen</title>
      <guid>https://learningenglish.voanews.com/a/8008342.html</guid>
      <pubDate>Tue, 29 Apr 2025 22:05:00 +0000</pubDate>
      <description>A lesson about ramen.</description>
    </item>
    <item>
      <title>Not an article</title>
      <link>https://learningenglish.voanews.com/z/1579</link>
    </item>
  </channel>
</rss>
"""

private const val SampleListingHtml = """
<ul>
  <li>
    <a href="/special_english/wilbur-and-orville-wright-the-first-airplane-93397.html">Wilbur and Orville Wright: The First Airplane</a> (25-3-18)
  </li>
  <li>
    <a class="lrc" href="/static/lrc/lincoln-s-cottage-a-country-house-for-the-president.lrc"></a>
    <a href="/special_english/lincoln-s-cottage-a-country-house-for-the-president-93396.html">Lincoln's Cottage: a Country House for the President</a> (25-3-17)
  </li>
  <li><a href="/as_it_is_2.html">[2]</a></li>
</ul>
"""

private const val SampleArticleHtml = """
<html>
<body>
  <div id="righter">
    <h1>Wilbur and Orville Wright: The First Airplane</h1>
    <div id="jquery_jplayer_1">
      <audio src="https://files.21voa.com/audio/202503/wilbur-and-orville-wright-the-first-airplane.mp3"></audio>
    </div>
    <div class="content">
      <span class="datetime">17 March, 2025</span>
      <p>Wilbur and Orville Wright are the American inventors who made a small engine-powered flying machine.</p>
      <p>On December 17th, 1903, they made the world's first flight.</p>
    </div>
  </div>
</body>
</html>
"""
