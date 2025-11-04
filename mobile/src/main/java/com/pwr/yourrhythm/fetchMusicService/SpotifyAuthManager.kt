package com.pwr.yourrhythm.fetchMusicService

import android.util.Base64
import android.util.Log
import com.pwr.yourrhythm.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class SpotifyAuthManager() {

    private val clientId = BuildConfig.SPOTIFY_CLIENT_ID
    private val clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET

    fun refreshAccessToken(refreshToken: String?, callback: (String?) -> Unit) {
        val url = "https://accounts.spotify.com/api/token"
        val body = "grant_type=refresh_token&refresh_token=$refreshToken"
        val authHeader = "Basic " + Base64.encodeToString("$clientId:$clientSecret".toByteArray(), Base64.NO_WRAP)

        val request = Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull()))
            .addHeader("Authorization", authHeader)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Spotify", "Refresh token failed", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        Log.e("Spotify", "Unexpected response code: ${it.code}")
                        callback(null)
                        return
                    }

                    try {
                        val json = JSONObject(it.body?.string() ?: "")
                        val accessToken = json.getString("access_token")
                        Log.d("Spotify", "New access token: $accessToken")
                        callback(accessToken)
                    } catch (e: Exception) {
                        Log.e("Spotify", "JSON parse error", e)
                        callback(null)
                    }
                }
            }
        })
    }

}