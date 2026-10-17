package com.malism.linsia.dailylisten.data.parse

import com.malism.linsia.dailylisten.domain.ListingCandidate
import com.malism.linsia.dailylisten.domain.filterCandidatesNotAfterToday
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    }
}

class ArticleHtmlParserTest {
    @Test
    fun extractsParagraphsAndMp3() {
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
    fun returnsNullAudioWhenMissing() {
        val parsed = parseArticleHtml(
            """
            <div id="righter">
              <div class="content">
                <p>Only text.</p>
              </div>
            </div>
            """.trimIndent(),
        )
        assertEquals("Only text.", parsed.body)
        assertNull(parsed.audioUrl)
    }
}

class ResolveTodayTest {
    @Test
    fun filtersFutureAndOrdersNewestFirst() {
        val candidates = listOf(
            ListingCandidate("1", "Old", "Mar 17, 2025", parseFlexibleEpoch("25-3-17"), "u1"),
            ListingCandidate("2", "Today", "Mar 18, 2025", parseFlexibleEpoch("25-3-18"), "u2"),
            ListingCandidate("3", "Future", "Mar 19, 2025", parseFlexibleEpoch("25-3-19"), "u3"),
        )
        val filtered = filterCandidatesNotAfterToday(candidates, "2025-03-18", ::epochToDateIso)
        assertEquals(listOf("2", "1"), filtered.map { it.id })
    }
}

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
