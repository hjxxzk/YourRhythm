package com.pwr.yourrhythm

import android.content.Context
import androidx.core.content.edit

object OnboardingManager {
    private const val PREFS = "app_prefs"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"

    fun isOnboardingCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false)
    }

    fun setOnboardingCompleted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_ONBOARDING_DONE, true)
        }
    }
}

