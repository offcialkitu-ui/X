
package iad1tya.echo.music.utils

import iad1tya.echo.music.models.MediaMetadata

enum class SongMood(val rotationDurationMs: Int, val bpm: Int) {
    PHONK(rotationDurationMs = 1_500, bpm = 140),
    LOFI(rotationDurationMs = 12_000, bpm = 72),
    POP(rotationDurationMs = 4_000, bpm = 110),
    DEFAULT(rotationDurationMs = 8_000, bpm = 100)
}

object SongMoodDetector {
    fun detect(metadata: MediaMetadata?): SongMood {
        if (metadata == null) return SongMood.DEFAULT
        
        val title = metadata.title.lowercase()
        val artists = metadata.artists.joinToString { it.name }.lowercase()
        val combined = "$title $artists"

        return when {
            // Expanded Phonk/Aggressive keywords
            combined.contains("phonk") || combined.contains("drift") || combined.contains("brazilian") || 
            combined.contains("bass boosted") || combined.contains("aggressive") || combined.contains("sigma") ||
            combined.contains("phonk remix") -> 
                SongMood.PHONK
            
            // Expanded Lo-fi/Chill keywords
            combined.contains("lofi") || combined.contains("lo-fi") || combined.contains("chill") || 
            combined.contains("study") || combined.contains("relax") || combined.contains("sleep") || 
            combined.contains("ambient") || combined.contains("slowed") || combined.contains("reverb") -> 
                SongMood.LOFI
            
            // Expanded Pop/Energetic keywords
            combined.contains("pop") || combined.contains("dance") || combined.contains("hit") || 
            combined.contains("club") || combined.contains("party") || combined.contains("upbeat") ||
            combined.contains("radio edit") || combined.contains("top 50") -> 
                SongMood.POP

            else -> SongMood.DEFAULT
        }
    }
}
