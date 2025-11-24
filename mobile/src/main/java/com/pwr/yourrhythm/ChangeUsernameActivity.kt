package com.pwr.yourrhythm

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class ChangeUsernameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_username)

        val backArrow = findViewById<ImageView>(R.id.arrow)

        backArrow.setOnClickListener {
            finish()
        }

        val usernameChange = findViewById<EditText>(R.id.username_change)
        val username = Preferences.getUsername(this)
        usernameChange.setText(username)

        val save = findViewById<TextView>(R.id.save)

        save.setOnClickListener {
            if(usernameChange.text.toString() == "") {
                Toast.makeText(this, "Please enter valid username", Toast.LENGTH_SHORT).show()
            } else {
                Preferences.saveUsername(this, usernameChange.text.toString())
                finish()
            }
        }
    }
}