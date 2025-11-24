package com.pwr.yourrhythm.heartRateService

import androidx.lifecycle.MutableLiveData

data class HeartRateEvent(
    val heartRate: Float,
    val senderId: String
)

object HeartRateRepository {
    val heartRateLiveData = MutableLiveData<HeartRateEvent>()
}