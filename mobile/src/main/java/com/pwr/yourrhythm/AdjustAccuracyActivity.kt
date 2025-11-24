package com.pwr.yourrhythm

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AdjustAccuracyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_adjust_accuracy)

        val backArrow = findViewById<ImageView>(R.id.arrow)

        backArrow.setOnClickListener {
            finish()
        }

        val index = findViewById<TextView>(R.id.number)
        index.text = Preferences.getIndex(this)

        var currentValue = index.text.toString().toInt()

        val plusButton = findViewById<TextView>(R.id.add_button)
        val minusButton = findViewById<TextView>(R.id.subtract_button)

        plusButton.setOnClickListener {
            if (currentValue < 15) {
                currentValue += 1
                index.text = currentValue.toString()
            } else {
                Toast.makeText(this, "Accuracy should not be bigger than 15.", Toast.LENGTH_SHORT).show()
            }
        }

        minusButton.setOnClickListener {
            if (currentValue > 0) {
                currentValue -= 1
                index.text = currentValue.toString()
            } else {
                Toast.makeText(this, "Accuracy cannot be smaller than 0.", Toast.LENGTH_SHORT).show()
            }
        }

        val save = findViewById<TextView>(R.id.save)

        save.setOnClickListener {
            Preferences.saveIndex(this, currentValue.toString())
            finish()
        }
    }
}