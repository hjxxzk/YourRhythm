package com.pwr.yourrhythm

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.pwr.yourrhythm.fetchMusicService.SpotifyAuthManager
import com.pwr.yourrhythm.fetchMusicService.FindSongService
import com.pwr.yourrhythm.heartRateService.HeartRateViewModel
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET
    private val apiKey = BuildConfig.GETSONG_API_KEY
    private val redirectUri = "com.pwr.yourrhythm://callback"
    private val AUTH_CODE_REQUEST_CODE = 0x11
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var spotifyAppRemote: SpotifyAppRemote? = null
    private lateinit var spotifyAuthManager : SpotifyAuthManager
    private lateinit var findSongService: FindSongService
    private lateinit var heartRateText: TextView
    private val heartRateViewModel: HeartRateViewModel by viewModels()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val songsList = mutableListOf<Song>()
    private var isSpotifyConnected = false
    data class Song(
        val title: String,
        val artist: String,
        var trackId: String,
        var img: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spotifyAuthManager = SpotifyAuthManager()
        findSongService = FindSongService()

        // 1. Autoryzacja Spotify
        authenticateSpotify()

        //3. Połaczenie się z zgearkiem
        heartRateText = findViewById(R.id.heartRateTextView)

        // 2. Odebranie danych z zegarka
        heartRateViewModel.heartRate.observe(this) { value ->
            if (value != null && value > 0f) {
                heartRateText.text = "❤️ $value bpm"

                if(isSpotifyConnected) {
                    findSongService.getSongsByBpm(value, apiKey, limit = 5) { songs ->
                        songsList.clear()
                        songsList.addAll(songs)
                        songsList.forEach { song ->
                            Log.d("MainActivity", "🎵 ${song.title} by ${song.artist}")
                        }
                        // 4. Zagranie piosenki
                        val firstSong = songs.firstOrNull()
                        if (firstSong != null && accessToken != null) {
                            findSongService.searchTrackOnSpotify(firstSong.title, firstSong.artist, accessToken) { trackId ->
                                if (trackId != null) {
                                    firstSong.trackId = trackId
                                    playTrack(firstSong)
                                } else {
                                    Log.w("MainActivity", "Nie znaleziono trackId dla ${firstSong.title}")
                                }
                            }
                        }
                    }
                }
            } else {
                heartRateText.text = "Czekam na dane z zegarka..."
            }
        }
    }

    private fun authenticateSpotify() {
        val request = AuthorizationRequest.Builder(clientId, AuthorizationResponse.Type.CODE, redirectUri)
            .setScopes(arrayOf("user-read-email", "streaming", "user-modify-playback-state"))
            .setShowDialog(true)
            .build()
        AuthorizationClient.openLoginActivity(this, AUTH_CODE_REQUEST_CODE, request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTH_CODE_REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, data)
            when (response.type) {
                AuthorizationResponse.Type.CODE -> {
                    Log.d("Spotify", "Auth code: ${response.code}")
                    //2. Pobranie access tokena
                    exchangeCodeForToken(response.code)
                }
                AuthorizationResponse.Type.ERROR -> {
                    Log.e("Spotify", "Auth error: ${response.error}")
                }
                else -> {}
            }
        }
    }

    private fun exchangeCodeForToken(code: String) {
        val url = "https://accounts.spotify.com/api/token"
        val body = "grant_type=authorization_code&code=$code&redirect_uri=$redirectUri"
        val authHeader = "Basic " + Base64.encodeToString("$clientId:$clientSecret".toByteArray(), Base64.NO_WRAP)

        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull()))
            .addHeader("Authorization", authHeader)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Spotify", "Token exchange failed", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body?.string() ?: "")
                accessToken = json.getString("access_token")
                refreshToken = json.getString("refresh_token")
                Log.d("Spotify", "Access token: $accessToken")
                Log.d("Spotify", "Refresh token: $refreshToken")
                startRefreshingToken()
                runOnUiThread { connectToSpotifyAppRemote() }
            }
        })
    }

    private fun startRefreshingToken() {
        scope.launch {
            while (isActive) {
                spotifyAuthManager.refreshAccessToken(refreshToken) { newToken ->
                    if (newToken != null) {
                        accessToken = newToken
                    }
                }
                delay(20_000)
            }
        }
    }

    private fun connectToSpotifyAppRemote() {
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(this, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                Log.d("Spotify", "Connected to Spotify App Remote!")
                isSpotifyConnected = true
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("Spotify", "Failed to connect to Spotify App Remote", throwable)
            }
        })
    }

    private fun playTrack(song: Song?) {
        Log.d("SpotifyAPI", "🎵 Spotify Track ID: ${song?.trackId} (${song?.title})")
        spotifyAppRemote?.playerApi?.play("spotify:track:${song?.trackId}")
    }


    override fun onStop() {
        super.onStop()
        spotifyAppRemote?.let { SpotifyAppRemote.disconnect(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

}
