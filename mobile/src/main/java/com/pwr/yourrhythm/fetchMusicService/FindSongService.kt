package com.pwr.yourrhythm.fetchMusicService

import android.util.Log
import com.pwr.yourrhythm.MainActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

class FindSongService {

    data class Artist(
        val id: String,
        val name: String,
        val uri: String,
        val genres: List<String>,
        val from: String?,
        val mbid: String?
    )

    data class Album(
        val title: String,
        val uri: String,
        val year: Int
    )

    fun getSongsByBpm(
        bpm: Float,
        apiKey: String,
        limit: Int = 5,
        callback: (List<MainActivity.Song>) -> Unit
    ) {
        val url = "https://api.getsong.co/tempo/?bpm=$bpm&limit=$limit&api_key=$apiKey"

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
                    val songs = mutableListOf<MainActivity.Song>()

                    for (i in 0 until tempoArray.length()) {
                        val songObj = tempoArray.getJSONObject(i)
                        val title = songObj.getString("song_title")
                        val artistObj = songObj.getJSONObject("artist")
                        val artistName = artistObj.getString("name")

                        songs.add(
                            MainActivity.Song(
                                title = title,
                                artist = artistName,
                                trackId = "",
                                img = ""
                            )
                        )
                    }

                    callback(songs)
                } catch (e: Exception) {
                    Log.e("GetSongAPI", "Failed to parse JSON: ${e.message}")
                    callback(emptyList())
                }
            }
        })
    }

    fun searchTrackOnSpotify(
        title: String,
        artist: String,
        spotifyToken: String?,
        callback: (String?) -> Unit
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
                            Log.d("SpotifyAPI", "✅ Found Spotify ID: $trackId for $title by $artist")
                            callback(trackId)
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