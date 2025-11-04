package com.pwr.yourrhythm.heartRateService

import android.widget.Toast
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class HeartRateListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/heartrate") {
            val heartRate = String(messageEvent.data).toFloatOrNull()


            if (heartRate != null) {
                HeartRateRepository.heartRateLiveData.postValue(heartRate)
            }

        } else {
            Toast.makeText(applicationContext, "Unknown path: ${messageEvent.path}", Toast.LENGTH_SHORT).show()
        }
    }
}