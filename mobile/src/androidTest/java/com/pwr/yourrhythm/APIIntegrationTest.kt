package com.pwr.yourrhythm

import com.pwr.yourrhythm.fetchMusicService.findSongsBasedOnPreferences
import junit.framework.TestCase.assertTrue
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class APIIntegrationTest(private val bpm: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): List<Int> {
            return (40..220).toList()
        }
    }

    @Test
    fun givenPopGenreAndBpm_usingSongsFilter_shouldReturnOnlyMatchingSongs() {

        val start = System.nanoTime()

        val apiKey = BuildConfig.GETSONG_API_KEY

        val url = "https://api.getsong.co/tempo/?bpm=$bpm&api_key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = OkHttpClient().newCall(request).execute()
        assertTrue(response.isSuccessful)

        val json = JSONObject(response.body!!.string())
        val tempoArray = json.getJSONArray("tempo")
        val genres = listOf("pop")

        val songsFiltered = findSongsBasedOnPreferences(tempoArray, genres)

        val end = System.nanoTime()
        val durationMs = (end - start) / 1_000_000.0

        println("$durationMs")

        println("🔎 BPM=$bpm → Found: ${songsFiltered.size} songs")

        for (song in songsFiltered) {
            assertTrue(song.artist.genres.any { it in genres })
        }
    }
}
