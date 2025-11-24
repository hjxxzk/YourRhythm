package com.pwr.yourrhythm

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backArrow = findViewById<ImageView>(R.id.arrow)

        backArrow.setOnClickListener {
            finish()
        }

        val changeUsername = findViewById<TextView>(R.id.change_username)

        changeUsername.setOnClickListener {
            startActivity(Intent(this, ChangeUsernameActivity::class.java))
        }

        val changePreferences = findViewById<TextView>(R.id.change_preferences)

        changePreferences.setOnClickListener {
            startActivity(Intent(this, ChangePreferencesActivity::class.java))
        }

        val adjustAccuracy = findViewById<TextView>(R.id.adjust_accuracy)

        adjustAccuracy.setOnClickListener {
            startActivity(Intent(this, AdjustAccuracyActivity::class.java))
        }

        val clearData = findViewById<TextView>(R.id.clear_data)

        clearData.setOnClickListener {
            showAreYouSurePopup()
        }
    }

    private fun showAreYouSurePopup() {
        val view = layoutInflater.inflate(R.layout.are_you_sure_popup, null)

        val dialog = AlertDialog.Builder(this, R.style.MySmallDialog)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        view.findViewById<TextView>(R.id.cancel).setOnClickListener {
            dialog.dismiss()
        }

        view.findViewById<TextView>(R.id.delete).setOnClickListener {
            val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            activityManager.clearApplicationUserData()
        }

        dialog.show()
    }
}