package com.pwr.yourrhythm

import android.content.Context
import androidx.core.content.edit

object Preferences {
    private const val PREF_NAME = "app-prefs"
    private const val KEY_GENRES = "selected_genres"
    private const val KEY_USERNAME = "username"
    private const val KEY_INDEX = "index"

    fun saveGenres(context: Context, genres: Set<String>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putStringSet(KEY_GENRES, genres) }
    }

    fun getGenres(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_GENRES, emptySet()) ?: emptySet()
    }

    fun saveUsername(context: Context, username: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_USERNAME, username) }
    }

    fun getUsername(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USERNAME, "") ?: ""
    }

    fun saveIndex(context: Context, index: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_INDEX, index) }
    }

    fun getIndex(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_INDEX, "") ?: ""
    }
}