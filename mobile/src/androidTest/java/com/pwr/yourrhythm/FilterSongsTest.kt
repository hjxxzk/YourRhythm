package com.pwr.yourrhythm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pwr.yourrhythm.fetchMusicService.Tempo
import com.pwr.yourrhythm.fetchMusicService.findSongsBasedOnPreferences
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(AndroidJUnit4::class)
class FilterSongsTest {

    @Test
    fun fetchData() {
        val bpm = 120
        val apiKey = BuildConfig.GETSONG_API_KEY

        val url = "https://api.getsong.co/tempo/?bpm=$bpm&api_key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val client = OkHttpClient()

        val response = client.newCall(request).execute()

        assertTrue(response.isSuccessful)

        val body = response.body?.string()
        assertNotNull(body)

        val json = JSONObject(body!!)
        assertTrue(json.has("tempo"))

        val tempoArray = json.getJSONArray("tempo")

        assertTrue(tempoArray.length() > 0)

        val first = tempoArray.getJSONObject(0)
        assertTrue(first.has("song_title"))
        assertTrue(first.has("tempo"))

        println("API returned: $first")
    }

    @Test
    fun filter4ForPop() {
        val bpm = 120
        val apiKey = BuildConfig.GETSONG_API_KEY

        val url = "https://api.getsong.co/tempo/?bpm=$bpm&api_key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val client = OkHttpClient()
        val response = client.newCall(request).execute()

        assertTrue(response.isSuccessful)

        val body = response.body?.string()
        assertNotNull(body)

        val json = JSONObject(body!!)
        val tempoArray = json.getJSONArray("tempo")

        val genres = listOf("pop")
        val songsFiltered: List<Tempo> = findSongsBasedOnPreferences(tempoArray, genres)

        for (song in songsFiltered) {
            println("API returned: ${song.song_title} / ${song.artist.genres}")
            assertTrue(song.artist.genres.any { it in genres })
        }
    }

    @Test
    fun filter4ForPopRock() {
        val bpm = 120
        val apiKey = BuildConfig.GETSONG_API_KEY

        val url = "https://api.getsong.co/tempo/?bpm=$bpm&api_key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val client = OkHttpClient()
        val response = client.newCall(request).execute()

        assertTrue(response.isSuccessful)

        val body = response.body?.string()
        assertNotNull(body)

        val json = JSONObject(body!!)
        val tempoArray = json.getJSONArray("tempo")

        val genres = listOf("pop", "rock")
        val songsFiltered: List<Tempo> = findSongsBasedOnPreferences(tempoArray, genres)

        for (song in songsFiltered) {
            println("API returned: ${song.song_title} / ${song.artist.genres}")
            assertTrue(song.artist.genres.any { it in genres })
        }
    }

    @Test
    fun filter4ForPopRockBlues() {
        val bpm = 120
        val apiKey = BuildConfig.GETSONG_API_KEY

        val url = "https://api.getsong.co/tempo/?bpm=$bpm&api_key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val client = OkHttpClient()
        val response = client.newCall(request).execute()

        assertTrue(response.isSuccessful)

        val body = response.body?.string()
        assertNotNull(body)

        val json = JSONObject(body!!)
        val tempoArray = json.getJSONArray("tempo")

        val genres = listOf("pop", "rock", "blues")
        val songsFiltered: List<Tempo> = findSongsBasedOnPreferences(tempoArray, genres)

        for (song in songsFiltered) {
            println("API returned: ${song.song_title} / ${song.artist.genres}")
            assertTrue(song.artist.genres.any { it in genres })
        }
    }

}