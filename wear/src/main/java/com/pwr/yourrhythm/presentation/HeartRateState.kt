package com.pwr.yourrhythm.presentation

import kotlinx.coroutines.flow.MutableStateFlow

object HeartRateState {
    val sendStatus = MutableStateFlow<SendStatus>(SendStatus.Idle)
}
