package com.pwr.yourrhythm.theme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pwr.yourrhythm.MainActivity
import com.pwr.yourrhythm.R

class SongAdapter(
    private var songs: MutableList<MainActivity.Song>,
    private val onItemClick: (MainActivity.Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val songName: TextView = itemView.findViewById(R.id.songTitle)
        val artist: TextView = itemView.findViewById(R.id.artist)
        val position: TextView = itemView.findViewById(R.id.songId)
        val itemView: ImageView = itemView.findViewById(R.id.songImg)

        val equalizer: ImageView = itemView.findViewById(R.id.equilizer)

        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(songs[pos])
                }
            }
        }
    }

    var playingTrackId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.song_item, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.songName.text = songs[position].title
        holder.artist.text = songs[position].artist
        holder.position.text = "#${position + 1}"

        if (songs[position].trackId == playingTrackId) {
            holder.equalizer.visibility = View.VISIBLE
        } else {
            holder.equalizer.visibility = View.INVISIBLE
        }

        Glide.with(holder.itemView.context)
            .load(songs[position].img)
            .placeholder(R.drawable.logo_icon)
            .into(holder.itemView)
    }

    fun setCurrentlyPlayingTrack(trackId: String) {
        playingTrackId = trackId
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = songs.size

    fun updateSongs(newSongs: List<MainActivity.Song>) {
        songs.clear()
        songs.addAll(newSongs)
        notifyDataSetChanged()
    }
}
