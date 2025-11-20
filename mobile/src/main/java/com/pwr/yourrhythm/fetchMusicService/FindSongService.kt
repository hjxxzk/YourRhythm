package com.pwr.yourrhythm.fetchMusicService

import android.util.Log
import com.pwr.yourrhythm.MainActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

class FindSongService {
    fun getSongsByBpm(
        bpm: Float,
        apiKey: String,
        callback: (List<MainActivity.Song>) -> Unit
    ) {
        val url = "https://api.getsong.co/tempo/?bpm=$bpm&&api_key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("GetSongAPI", "Request failed: ${e.message}")
                callback(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("GetSongAPI", "HTTP error ${response.code}: ${response.message}")
                    callback(emptyList())
                    return
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    Log.e("GetSongAPI", "Empty response")
                    callback(emptyList())
                    return
                }

                try {
                    val json = JSONObject(responseBody)
                    val tempoArray = json.getJSONArray("tempo")
                    val genres = listOf("rock", "pop", "dance")
                    val songsFiltered = findSongsBasedOnPreferences(tempoArray, genres)

                    val result = songsFiltered.map { song ->
                        MainActivity.Song(
                            title = song.song_title,
                            artist = song.artist.name,
                            trackId = "",
                            img = ""
                        )
                    }

                    callback(result)
                } catch (e: Exception) {
                    Log.e("GetSongAPI", "Failed to parse JSON: ${e.message}")
                    callback(emptyList())
                }
            }
        })
    }

    data class SpotifyTrackInfo(
        val trackId: String,
        val imageUrl: String
    )

    fun searchTrackOnSpotify(
        title: String,
        artist: String,
        spotifyToken: String?,
        callback: (SpotifyTrackInfo?) -> Unit
    ) {
        val query = URLEncoder.encode("$title $artist", "UTF-8")
        val url = "https://api.spotify.com/v1/search?q=$query&type=track&limit=1"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $spotifyToken")
            .get()
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SpotifyAPI", "Request failed: ${e.message}")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e("SpotifyAPI", "Unexpected response: ${it.code}")
                        callback(null)
                        return
                    }

                    val json = it.body?.string()
                    try {
                        val root = JSONObject(json)
                        val items = root.getJSONObject("tracks").getJSONArray("items")

                        if (items.length() > 0) {
                            val track = items.getJSONObject(0)
                            val trackId = track.getString("id")
                            val images = track.getJSONObject("album").getJSONArray("images")
                            val imageUrl = images.getJSONObject(0).getString("url")

                            callback(
                                SpotifyTrackInfo(
                                    trackId = trackId,
                                    imageUrl = imageUrl
                                )
                            )
                        } else {
                            Log.w("SpotifyAPI", "⚠️ No match found for $title by $artist")
                            callback(null)
                        }
                    } catch (e: Exception) {
                        Log.e("SpotifyAPI", "JSON parse error: ${e.message}")
                        callback(null)
                    }
                }
            }
        })
    }
}