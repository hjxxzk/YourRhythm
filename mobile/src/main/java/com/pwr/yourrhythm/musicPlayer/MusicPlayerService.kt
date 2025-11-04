package com.pwr.yourrhythm.musicPlayer

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import com.pwr.yourrhythm.R

class MusicPlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mediaPlayer = MediaPlayer.create(this, R.raw.song)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
        return START_STICKY
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    fun resume() {
        mediaPlayer?.start()
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}