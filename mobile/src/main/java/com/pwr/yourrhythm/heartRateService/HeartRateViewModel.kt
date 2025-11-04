package com.pwr.yourrhythm.heartRateService

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel

class HeartRateViewModel : ViewModel() {
    val heartRate: LiveData<Float> = HeartRateRepository.heartRateLiveData
}