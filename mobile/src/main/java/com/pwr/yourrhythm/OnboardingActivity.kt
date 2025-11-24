package com.pwr.yourrhythm

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var backButton: Button
    private lateinit var saveButton: Button

    private val firstPage = 0
    private val secondPage = 1
    private val thirdPage = 2
    private val lastPage = 3
    private val pageCount = 4


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.onboardingViewPager)
        backButton = findViewById(R.id.backButton)
        saveButton = findViewById(R.id.saveButton)



        val pages = listOf(
            R.layout.onboarding_screen_1,
            R.layout.onboarding_screen_2,
            R.layout.onboarding_screen_3,
            R.layout.onboarding_screen_4
        )

        viewPager.adapter = OnboardingAdapter(pages)
        viewPager.isUserInputEnabled = false

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                if (position == secondPage || position == thirdPage) {
                    saveButton.visibility = View.VISIBLE
                } else {
                    saveButton.visibility = View.GONE
                }

                if (position == thirdPage) {
                    backButton.visibility = View.VISIBLE
                } else {
                    backButton.visibility = View.GONE
                }

                if (position == firstPage) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (position < pageCount - 1) {
                            viewPager.currentItem = position + 1
                        }
                    }, 4000)
                }

                if (position == lastPage) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        completeOnboarding()
                    }, 3000)
                }
            }
        })

        backButton.setOnClickListener {
            if (viewPager.currentItem > 0) {
                viewPager.currentItem -= 1
            }
        }

        saveButton.setOnClickListener {

            if (viewPager.currentItem == thirdPage) {
                val adapter = viewPager.adapter as OnboardingAdapter
                val genres = adapter.getSelectedGenres()
                Preferences.saveGenres(this, genres)

                if (viewPager.currentItem < pageCount) {
                    viewPager.currentItem += 1
                }
            }

            if (viewPager.currentItem == secondPage) {
                val username = findViewById<EditText>(R.id.inputText).text.toString()

                if(username == "") {
                    Toast.makeText(this, "Please enter nickname", Toast.LENGTH_SHORT).show()
                } else {
                    Preferences.saveUsername(this, username)

                    if (viewPager.currentItem < pageCount) {
                        viewPager.currentItem += 1
                    }
                }
            }
        }
    }

    private fun completeOnboarding() {
        OnboardingManager.setOnboardingCompleted(this)
        Preferences.saveIndex(this, 5.toString())
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}