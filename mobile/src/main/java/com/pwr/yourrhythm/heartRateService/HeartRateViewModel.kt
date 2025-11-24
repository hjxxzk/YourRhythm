package com.pwr.yourrhythm.heartRateService

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

class HeartRateViewModel : ViewModel() {
    val heartRateEvent: LiveData<HeartRateEvent> = HeartRateRepository.heartRateLiveData
}
