package com.pwr.yourrhythm

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.pwr.yourrhythm.fetchMusicService.SpotifyAuthManager
import com.pwr.yourrhythm.fetchMusicService.FindSongService
import com.pwr.yourrhythm.heartRateService.HeartRateViewModel
import com.pwr.yourrhythm.security.TokenEncryptionHelper.getAccessToken
import com.pwr.yourrhythm.security.TokenEncryptionHelper.getRefreshToken
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.pwr.yourrhythm.security.TokenEncryptionHelper.saveTokens
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlinx.coroutines.*
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.pwr.yourrhythm.security.TokenEncryptionHelper.saveAccessToken
import com.pwr.yourrhythm.theme.CurveProgressView
import com.pwr.yourrhythm.theme.SongAdapter
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET
    private val apiKey = BuildConfig.GETSONG_API_KEY
    private val redirectUri = "com.pwr.yourrhythm://callback"
    private val AUTH_CODE_REQUEST_CODE = 0x11
    private var spotifyAppRemote: SpotifyAppRemote? = null
    private lateinit var spotifyAuthManager : SpotifyAuthManager
    private lateinit var findSongService: FindSongService
    private lateinit var bpmText: TextView
    private val heartRateViewModel: HeartRateViewModel by viewModels()
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var songsList = mutableListOf<Song>()
    private lateinit var adapter: SongAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private var heartRate : Float = 0F
    private var isSpotifyConnected = false
    private var isPlaying = true
    private var currentlyPlayingTrackId: String? = null


    data class Song(
        val title: String,
        val artist: String,
        var trackId: String,
        var img: String
    )


    override fun onCreate(savedInstanceState: Bundle?) {

        if (!OnboardingManager.isOnboardingCompleted(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        adapter = SongAdapter(songsList) { song ->
            playTrack(song)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        swipeRefresh = findViewById(R.id.swipeRefresh)

        swipeRefresh.setOnRefreshListener {
            refreshSongsManually()
        }

        val logo = findViewById<ImageView>(R.id.logo)

        logo.setOnClickListener {
            if(isPlaying && isSpotifyConnected) {
                onPause()
            } else {
                onResume()
            }
        }

        val curveProgress = findViewById<CurveProgressView>(R.id.curveProgress)
        spotifyAuthManager = SpotifyAuthManager()
        findSongService = FindSongService()

        // 1. Autoryzacja Spotify
        authenticateSpotify()
        //3. Połaczenie się z zgearkiem
        bpmText = findViewById(R.id.bpm)
        // 2. Odebranie danych z zegarka
        heartRateViewModel.heartRate.observe(this) { value ->
            if (value != null && value > 0f) {
                curveProgress.setProgress(value / 200F)
                curveProgress.setBpm(value.toInt())

                if(shouldSongChange(value) && isSpotifyConnected) {

                    findSongService.getSongsByBpm(this, value, apiKey) { songs ->
                        songsList.clear()
                        songsList.addAll(songs)
                        fetchAlbumImages(songsList)

                        // 4. Zagranie piosenki
                        val firstSong = songs.firstOrNull()
                        val accessToken = getAccessToken(this@MainActivity)
                        if (firstSong != null && getAccessToken(this@MainActivity) != null) {
                            findSongService.searchTrackOnSpotify(firstSong.title, firstSong.artist, accessToken) { trackData ->
                                if (trackData != null) {
                                    firstSong.trackId = trackData.trackId
                                    updateUIOnSongChange(value.toInt(), songs)
                                    playTrack(firstSong)
                                    isPlaying = true

                                } else {
                                    Log.w("MainActivity", "Nie znaleziono trackId dla ${firstSong.title}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fetchAlbumImages(songs: List<Song>) {
        val accessToken = getAccessToken(this@MainActivity) ?: return
        for (i in 0 until songs.size) {
            val song = songs[i]
            findSongService.searchTrackOnSpotify(song.title, song.artist, accessToken) { trackData ->
                if (trackData != null) {
                    song.trackId = trackData.trackId
                    song.img = trackData.imageUrl

                    runOnUiThread {
                        adapter.notifyItemChanged(i)
                    }
                }
            }
        }
    }


    private fun refreshSongsManually() {
        swipeRefresh.isRefreshing = true

        val apiKey = BuildConfig.GETSONG_API_KEY
        findSongService.getSongsByBpm(this, heartRate, apiKey) { songs ->
            songsList.clear()
            songsList.addAll(songs)
            runOnUiThread {
                adapter.updateSongs(songs)
                fetchAlbumImages(songs)
            }
            swipeRefresh.isRefreshing = false
        }
    }


    private fun updateUIOnSongChange(bpm: Int, songs: List<Song>) {
        runOnUiThread {
            bpmText.text = "$bpm BPM"
            adapter.updateSongs(songs)
        }
    }

    private fun authenticateSpotify() {
        val request = AuthorizationRequest.Builder(clientId, AuthorizationResponse.Type.CODE, redirectUri)
            .setScopes(arrayOf("streaming", "user-modify-playback-state"))
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
                saveTokens(this@MainActivity,
                    json.getString("access_token"),
                    json.getString("refresh_token"))
                startRefreshingToken()
                runOnUiThread { connectToSpotifyAppRemote() }
            }
        })
    }

    private fun startRefreshingToken() {
        scope.launch {
            while (isActive) {
                spotifyAuthManager.refreshAccessToken(getRefreshToken(this@MainActivity)) { newToken ->
                    if (newToken != null) {
                        saveAccessToken(this@MainActivity, newToken)
                    }
                }
                delay(25 * 60 * 1000)
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
        if(isPlaying && song != null && song.trackId.isNotEmpty()) {
            spotifyAppRemote?.playerApi?.play("spotify:track:${song.trackId}")
            currentlyPlayingTrackId = song.trackId
            adapter.setCurrentlyPlayingTrack(song.trackId)
        } else {
            Toast.makeText(this, "Couldn't play: ${song?.title}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        isPlaying = true
        spotifyAppRemote?.playerApi?.resume()
    }

    override fun onPause() {
        super.onPause()
        isPlaying = false
        spotifyAppRemote?.playerApi?.pause()
    }

    override fun onStop() {
        super.onStop()
        isPlaying = false
        spotifyAppRemote?.let { SpotifyAppRemote.disconnect(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        // Clear Access Tokens
        val prefs = getSharedPreferences("secure_prefs", MODE_PRIVATE)
        prefs.edit { clear() }
        Toast.makeText(this, "Tokens cleared", Toast.LENGTH_SHORT).show()
    }

    private var volatilityIndex : Float = 0F
    private val VOLATILITY_THRESHOLD = 5F

    fun shouldSongChange(newHeartRate : Float) : Boolean {
        if(isFirstHeartRateMeasurement()) {
            heartRate = newHeartRate
            return true
        }
        return checkVolatility(newHeartRate)
    }

    fun isFirstHeartRateMeasurement(): Boolean {
        return heartRate == 0F
    }

    fun isDifferenceHigherThanThreshold(): Boolean {
        return abs(volatilityIndex) >= VOLATILITY_THRESHOLD
    }

    fun checkVolatility(newHeartRate : Float): Boolean {
        val heartRateDifference = heartRate - newHeartRate
        volatilityIndex += heartRateDifference
        heartRate = newHeartRate

        if(isDifferenceHigherThanThreshold()) {
            volatilityIndex = 0F
            return true
        }
        return false
    }
}
