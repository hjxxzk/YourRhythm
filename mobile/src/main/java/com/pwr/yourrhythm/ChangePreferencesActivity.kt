package com.pwr.yourrhythm

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ChangePreferencesActivity : AppCompatActivity() {

    private var selectedGenres = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_change_preferences)
        selectedGenres = Preferences.getGenres(this).toMutableSet()

        val backArrow = findViewById<ImageView>(R.id.arrow)

        backArrow.setOnClickListener {
            finish()
        }

        val save = findViewById<TextView>(R.id.save)

        save.setOnClickListener {
            Preferences.saveGenres(this, selectedGenres)
            finish()
        }

        val genres = listOf(
            "blues", "classical", "country", "electronic", "folk", "funk",
            "heavy metal", "hip hop", "jazz", "latin", "pop", "rap",
            "reggae", "rock", "soul", "world"
        )

        val rows = listOf(
            findViewById<ViewGroup>(R.id.firstRow),
            findViewById<ViewGroup>(R.id.secondRow),
            findViewById<ViewGroup>(R.id.thirdRow),
            findViewById<ViewGroup>(R.id.fourthRow),
            findViewById<ViewGroup>(R.id.fifthRow)
        )

        val itemsPerRow = listOf(3, 3, 3, 4, 3)

        var genreIndex = 0
        for ((rowIndex, row) in rows.withIndex()) {
            val count = itemsPerRow[rowIndex]
            for (i in 0 until count) {
                val includeView = row.getChildAt(i) as View
                val textView = includeView.findViewById<TextView>(R.id.textInside)
                val genreName = genres[genreIndex]
                textView.text = genreName

                includeView.isSelected = selectedGenres.contains(genreName)

                includeView.setOnClickListener {
                    if (selectedGenres.contains(genreName)) {
                        selectedGenres.remove(genreName)
                        includeView.isSelected = false
                    } else {
                        selectedGenres.add(genreName)
                        includeView.isSelected = true
                    }
                }
                genreIndex++
            }
        }
    }
}