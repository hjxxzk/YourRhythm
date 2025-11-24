package com.pwr.yourrhythm

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
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
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.pwr.yourrhythm.Preferences.getUsername
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
    private lateinit var rotationAnimator: ObjectAnimator
    private var isPopupVisible = false
    private var connectedDevices = mutableListOf<Node>()
    private var watchIndex = 0


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

        VOLATILITY_THRESHOLD = Preferences.getIndex(this).toFloat()

        swipeRefresh = findViewById(R.id.swipeRefresh)

        swipeRefresh.setOnRefreshListener {
            refreshSongsManually()
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val logo = findViewById<ImageView>(R.id.logo)

        logo.setOnClickListener {
            if(isPlaying && isSpotifyConnected) {
                pause()
            } else {
                resume()
            }
        }

        val leftDrawer = findViewById<View>(R.id.leftDrawer)
        val leftParams = leftDrawer.layoutParams
        leftParams.width = (resources.displayMetrics.widthPixels * 0.75).toInt()
        leftDrawer.layoutParams = leftParams

        findViewById<ImageView>(R.id.icon_left).setOnClickListener {
            val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val drawerRight = findViewById<View>(R.id.rightDrawer)
        val paramsRight = drawerRight.layoutParams
        paramsRight.width = (resources.displayMetrics.widthPixels * 0.75).toInt()
        drawerRight.layoutParams = paramsRight

        findViewById<ImageView>(R.id.icon_right).setOnClickListener {
            val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
            drawerLayout.openDrawer(GravityCompat.END)
        }


        val usernameText = findViewById<TextView>(R.id.username)
        usernameText.text = getUsername(this)

        val settingsButton = findViewById<TextView>(R.id.settings_text)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)

        settingsButton.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        val curveProgress = findViewById<CurveProgressView>(R.id.curveProgress)
        spotifyAuthManager = SpotifyAuthManager()
        findSongService = FindSongService()

        android.os.Handler(Looper.getMainLooper()).postDelayed({
            if (heartRate == 0F && !isPopupVisible) {
                isPopupVisible = true
                showWatchPopup()
            }
        }, 3000)

        // 1. Autoryzacja Spotify
        authenticateSpotify()
        //3. Połaczenie się z zgearkiem
        bpmText = findViewById(R.id.bpm)
        // 2. Odebranie danych z zegarka
        heartRateViewModel.heartRateEvent.observe(this) { value ->
            if (value != null && value.heartRate > 0f &&
                connectedDevices.isNotEmpty() &&
                value.senderId == connectedDevices[watchIndex].id) {

                curveProgress.setProgress(value.heartRate / 200F)
                curveProgress.setBpm(value.heartRate.toInt())

                if(shouldSongChange(value.heartRate) && isSpotifyConnected) {

                    findSongService.getSongsByBpm(this, value.heartRate, apiKey) { songs ->
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
                                    updateUIOnSongChange(value.heartRate.toInt(), songs)
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

    override fun onResume() {
        super.onResume()
        val usernameText = findViewById<TextView>(R.id.username)
        usernameText.text = getUsername(this)
        VOLATILITY_THRESHOLD = Preferences.getIndex(this).toFloat()

        checkWatchConnection(this) { isConnected ->
            if (!isConnected && !isPopupVisible) {
                isPopupVisible = true
                val watch = findViewById<TextView>(R.id.watch)
                watch.text = "-"
                findViewById<View>(R.id.status_icon).visibility = View.GONE
                findViewById<ImageView>(R.id.watchImg).visibility = View.GONE
                val status = findViewById<TextView>(R.id.status)
                status.text = "-"
                showWatchPopup()
            } else {
                getConnectedWatches(this) { nodes ->
                    if (nodes != null) {
                        connectedDevices = nodes as MutableList<Node>
                        val watch = findViewById<TextView>(R.id.watch)
                        watch.text = nodes[0].displayName

                        if(connectedDevices.size > 1) {
                            allowScrolling()
                        } else {
                            disableScrolling()
                        }
                    }
                }
            }
        }

        checkWifiConnection(this) { isWifiConnected ->
            if (!isWifiConnected && !isPopupVisible) {
                isPopupVisible = true
                showWifiPopup()
            }
        }
    }

    private fun disableScrolling() {
        findViewById<ImageView>(R.id.arrowLeft).visibility = View.GONE
        findViewById<ImageView>(R.id.arrowRight).visibility = View.GONE
        findViewById<ImageView>(R.id.leftWatch).visibility = View.GONE
        findViewById<ImageView>(R.id.rightWatch).visibility = View.GONE
    }

    private fun allowScrolling() {
        findViewById<ImageView>(R.id.arrowLeft).setOnClickListener { scrollLeft() }
        findViewById<ImageView>(R.id.arrowRight).setOnClickListener { scrollRight() }

        updateWatchUi()
    }

    private fun scrollRight() {
        if (watchIndex < connectedDevices.size - 1) {
            watchIndex++
            updateWatchUi()
        }
    }

    private fun scrollLeft() {
        if (watchIndex > 0) {
            watchIndex--
            updateWatchUi()
        }
    }

    private fun updateWatchUi() {
        val watchText = findViewById<TextView>(R.id.watch)
        val left = findViewById<ImageView>(R.id.leftWatch)
        val right = findViewById<ImageView>(R.id.rightWatch)
        val leftArrow = findViewById<ImageView>(R.id.arrowLeft)
        val rightArrow = findViewById<ImageView>(R.id.arrowRight)

        watchText.text = connectedDevices[watchIndex].displayName

        left.visibility = if (watchIndex == 0) View.GONE else View.VISIBLE
        right.visibility = if (watchIndex == connectedDevices.size - 1) View.GONE else View.VISIBLE

        leftArrow.visibility = if (watchIndex == 0) View.GONE else View.VISIBLE
        rightArrow.visibility = if (watchIndex == connectedDevices.size - 1) View.GONE else View.VISIBLE
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
                observeSpotifyPlayback()
                setupLogoAnimation()
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

    fun resume() {
        super.onResume()
        isPlaying = true
        spotifyAppRemote?.playerApi?.resume()
    }

    fun pause() {
        super.onPause()
        isPlaying = false
        spotifyAppRemote?.playerApi?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        spotifyAppRemote?.let { SpotifyAppRemote.disconnect(it) }
        // Clear Access Tokens
        val prefs = getSharedPreferences("secure_prefs", MODE_PRIVATE)
        prefs.edit { clear() }
        Toast.makeText(this, "Tokens cleared", Toast.LENGTH_SHORT).show()
    }

    // LOGO ANIMATION

    private fun setupLogoAnimation() {
        val logo = findViewById<ImageView>(R.id.logo)

        rotationAnimator = ObjectAnimator.ofFloat(logo, View.ROTATION, 0f, 360f).apply {
            duration = 4000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
    }

    private fun observeSpotifyPlayback() {
        spotifyAppRemote?.playerApi?.subscribeToPlayerState()?.setEventCallback { playerState ->
            if (playerState.isPaused) {
                stopLogoAnimation()
            } else {
                startLogoAnimation()
            }
        }
    }

    private fun startLogoAnimation() {
        if (!rotationAnimator.isStarted) {
            rotationAnimator.start()
        } else {
            rotationAnimator.resume()
        }
    }

    private fun stopLogoAnimation() {
        if (rotationAnimator.isRunning) {
            rotationAnimator.pause()
        }
    }

    // SHOULD STOP ALGORITHM
    private var volatilityIndex : Float = 0F
    private var VOLATILITY_THRESHOLD : Float = 0F

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

    // POP UPS

    fun checkWatchConnection(context: Context, callback: (Boolean) -> Unit) {
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                callback(nodes.isNotEmpty())
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    private fun showWatchPopup() {
        val view = layoutInflater.inflate(R.layout.no_device_popup, null)

        val dialog = AlertDialog.Builder(this, R.style.MySmallDialog)
            .setView(view)
            .create()


        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        view.findViewById<TextView>(R.id.ok).setOnClickListener {
            isPopupVisible = false
            dialog.dismiss()
        }

        dialog.show()
    }

    fun checkWifiConnection(context: Context, callback: (Boolean) -> Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: run { callback(false); return }
        val capabilities = cm.getNetworkCapabilities(network) ?: run { callback(false); return }

        val online = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        callback(online)
    }

    private fun showWifiPopup() {
        val view = layoutInflater.inflate(R.layout.no_wifi_popup, null)

        val dialog = AlertDialog.Builder(this, R.style.MySmallDialog)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        view.findViewById<TextView>(R.id.ok).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // CONNECTED DEVICES
    fun getConnectedWatches(context: Context, callback: (List<Node>?) -> Unit) {
        Wearable.getNodeClient(context).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isNotEmpty()) {
                    callback(nodes)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }
}
