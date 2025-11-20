package com.pwr.yourrhythm.fetchMusicService

import org.json.JSONArray
    data class Album(
        val title: String,
        val uri: String,
        val year: Int
    )

    data class Tempo(
        val song_id: String,
        val song_title: String,
        val song_uri: String,
        val tempo: Int,
        val artist: Artist,
        val album: Album
    )

    data class Artist(
        val id: String,
        val name: String,
        val uri: String,
        val genres: List<String>,
        val from: String?,
        val mbid: String?
    )

    const val NUMBER_OF_RECOMMENDATIONS = 4

    fun findSongsBasedOnPreferences(apiResponse: JSONArray, genres: List<String>): List<Tempo> {
        val songs = makeSongsList(apiResponse)
        return filterByGenres(songs, genres)
    }

    fun filterByGenres(songs: MutableList<Tempo>, genres: List<String>): List<Tempo> {
        val songsFiltered = songs
            .filter { song -> song.artist.genres.any(genres::contains) }
            .toMutableList()

        return if (songsFiltered.size >= 4) {
            drawSongs(songsFiltered, NUMBER_OF_RECOMMENDATIONS)
        } else {
            recommendAvailable(songsFiltered, songs)
        }
    }

    fun drawSongs(songsFiltered: MutableList<Tempo>, songsNumber : Int): List<Tempo> {
        return songsFiltered.shuffled().take(songsNumber)
    }

    fun recommendAvailable(songsFiltered: MutableList<Tempo>, allSongs: MutableList<Tempo>
    ): List<Tempo> {
        val recommendationsList = songsFiltered.toMutableList()
        val missing = 4 - recommendationsList.size

        val available = allSongs.toMutableList().apply {
            removeAll(songsFiltered)
        }
        val missingSongs = drawSongs(available, missing)
        recommendationsList.addAll(missingSongs)
        return recommendationsList
    }


fun makeSongsList(songs: JSONArray): MutableList<Tempo> {
        val list = mutableListOf<Tempo>()
        for(song in 0 until songs.length()) {
            val obj = songs.getJSONObject(song)

            val artistJson = obj.getJSONObject("artist")

            val genresJson = artistJson.optJSONArray("genres")
            val genres = mutableListOf<String>()

            if (genresJson != null) {
                for (i in 0 until genresJson.length()) {
                    genres.add(genresJson.getString(i))
                }
            }

            val song = Tempo(
                song_id = obj.getString("song_id"),
                song_title = obj.getString("song_title"),
                song_uri = obj.getString("song_uri"),
                tempo = obj.optInt("tempo"),

                artist = Artist(
                    id = obj.getJSONObject("artist").getString("id"),
                    name = obj.getJSONObject("artist").getString("name"),
                    uri = obj.getJSONObject("artist").getString("uri"),
                    genres = genres,
                    from = obj.getJSONObject("artist").getString("from"),
                    mbid = obj.getJSONObject("artist").getString("mbid")
                ),
                album = Album(
                    title = obj.getJSONObject("album").getString("title"),
                    uri = obj.getJSONObject("album").getString("uri"),
                    year = obj.getJSONObject("album").optInt("year")
                )
            )
            list.add(song)
        }
        return list
    }
