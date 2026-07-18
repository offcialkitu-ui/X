package iad1tya.echo.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YTPlayerUtilsTest {

    @Test
    fun testQobuzSearchTerms() {
        val artist = "Lana Del Rey"
        val title = "Summertime Sadness"
        val terms = qobuzSearchTerms(artist, title)
        
        // Assert terms contains the basic combined term
        assertTrue(terms.contains("Lana Del Rey Summertime Sadness"))
    }

    @Test
    fun testQobuzSearchTermsWithMultipleArtists() {
        val artist = "Coldplay, Beyonce"
        val title = "Hymn for the Weekend"
        val terms = qobuzSearchTerms(artist, title)
        
        // Assert terms contains primary artist only query
        assertTrue(terms.contains("Coldplay Hymn for the Weekend"))
    }
}
