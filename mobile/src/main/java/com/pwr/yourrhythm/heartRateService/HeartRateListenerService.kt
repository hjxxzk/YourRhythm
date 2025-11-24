package com.pwr.yourrhythm.heartRateService

import android.widget.Toast
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class HeartRateListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/heartrate") {

            val heartRate = String(messageEvent.data).toFloatOrNull()
            val senderId = messageEvent.sourceNodeId

            if (heartRate != null) {
                HeartRateRepository.heartRateLiveData.postValue(
                    HeartRateEvent(heartRate, senderId)
                )
            }

        } else {
            Toast.makeText(applicationContext, "Unknown path: ${messageEvent.path}", Toast.LENGTH_SHORT).show()
        }
    }
}