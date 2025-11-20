package com.pwr.yourrhythm

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OnboardingAdapter(private val layouts: List<Int>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    private val selectedGenres = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun getItemViewType(position: Int) = layouts[position]

    override fun getItemCount() = layouts.size

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
            val view = holder.itemView

            if (position == 2) {
                val genres = listOf(
                    "blues", "classical", "country", "electronic", "folk", "funk",
                    "heavy metal", "hip hop", "jazz", "latin", "pop", "rap",
                    "reggae", "rock", "soul", "world"
                )

                val rows = listOf(
                    view.findViewById<ViewGroup>(R.id.firstRow),
                    view.findViewById<ViewGroup>(R.id.secondRow),
                    view.findViewById<ViewGroup>(R.id.thirdRow),
                    view.findViewById<ViewGroup>(R.id.fourthRow),
                    view.findViewById<ViewGroup>(R.id.fifthRow)
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

    fun getSelectedGenres(): Set<String> {
        return selectedGenres.toSet()
    }

    class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
