package com.pwr.yourrhythm.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HeartRateViewModel : ViewModel() {

    private val _sendStatus = MutableStateFlow<SendStatus>(SendStatus.Idle)
    val sendStatus = _sendStatus.asStateFlow()

    fun updateStatus(status: SendStatus) {
        _sendStatus.value = status
    }
}

sealed class SendStatus {
    object Idle : SendStatus()
    data class Success(val bpm: Float) : SendStatus()
    object Failure : SendStatus()
}