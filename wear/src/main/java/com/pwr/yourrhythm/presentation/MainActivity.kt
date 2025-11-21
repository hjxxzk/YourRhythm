package com.pwr.yourrhythm.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.core.app.ActivityCompat
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pwr.yourrhythm.R
import com.pwr.yourrhythm.presentation.theme.YourRhythmTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Zapobieganie wygaszeniu ekranu
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Sprawdzenie uprawnień BODY_SENSORS
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), 1)
        } else {
            startHeartRateService()
        }

        setContent {
            WearApp()
        }
    }

    private fun startHeartRateService() {
        startService(Intent(this, HeartRateService::class.java))
        Log.d("Wear", "HeartRateService started")
    }
}

@Composable
fun WearApp() {
    val status by HeartRateState.sendStatus.collectAsState()

    YourRhythmTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {

            TimeText()

            when (status) {
                is SendStatus.Idle -> IdleScreen()
                is SendStatus.Failure -> FailureScreen()
                is SendStatus.Success -> LogoScreen((status as SendStatus.Success).bpm)
            }
        }
    }
}

@Composable
fun IdleScreen() {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = "Loading..."
    )
}

@Composable
fun FailureScreen() {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = "No device connected."
    )
}


@Composable
fun LogoScreen(bpm: Float) {
    val scale = remember { Animatable(1f) }

    val beatDuration = (60_000f / bpm).toInt()

    LaunchedEffect(bpm) {
        while (true) {
            scale.animateTo(
                targetValue = 1.2f,
                animationSpec = tween(
                    durationMillis = beatDuration / 2,
                    easing = FastOutSlowInEasing
                )
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = beatDuration / 2,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    Image(
        painter = painterResource(id = R.drawable.logo_icon),
        contentDescription = "Logo",
        modifier = Modifier
            .size(190.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
    )
}


