package com.aura.music

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

class AuraViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ArchiveRepository()
    private val prefs = Prefs(app)

    // Discover
    var query by mutableStateOf("")
    var albums by mutableStateOf<List<Album>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var heading by mutableStateOf("TRENDING")
        private set

    // Quality
    var hifi by mutableStateOf(prefs.hifi)
        private set

    // Opened album
    var openAlbum by mutableStateOf<Album?>(null)
        private set
    var albumTracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var albumLoading by mutableStateOf(false)
        private set
    var albumError by mutableStateOf<String?>(null)
        private set

    // Library
    var favourites by mutableStateOf(prefs.loadFavourites())
        private set
    var favouriteIds by mutableStateOf<Set<String>>(favourites.mapTo(HashSet()) { it.id })
        private set

    private val known = HashMap<String, Track>()
    private var listJob: Job? = null
    private var albumJob: Job? = null

    init {
        favourites.forEach { known[it.id] = it }
        loadTrending()
    }

    // ---------- Discover ----------

    fun loadTrending() {
        query = ""
        load("TRENDING") { repo.trending() }
    }

    fun runSearch(text: String = query) {
        val q = text.trim()
        if (q.isEmpty()) {
            loadTrending()
            return
        }
        query = q
        load("RESULTS FOR \u201C${q.uppercase()}\u201D") { repo.search(q) }
    }

    fun retry() {
        if (query.isBlank()) loadTrending() else runSearch(query)
    }

    private fun load(title: String, block: suspend () -> List<Album>) {
        listJob?.cancel()
        listJob = viewModelScope.launch {
            loading = true
            error = null
            heading = title
            try {
                val result = block()
                albums = result
                if (result.isEmpty()) error = "Nothing found. Try another search."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                albums = emptyList()
                error = friendly(e)
            }
            loading = false
        }
    }

    // ---------- Quality ----------

    fun toggleHifi() {
        hifi = !hifi
        prefs.hifi = hifi
        openAlbum?.let { open(it) }   // re-resolve files with the new setting
    }

    // ---------- Album ----------

    fun open(album: Album) {
        openAlbum = album
        albumTracks = emptyList()
        albumError = null
        albumLoading = true
        albumJob?.cancel()
        albumJob = viewModelScope.launch {
            try {
                val tracks = repo.tracks(album, hifi)
                register(tracks)
                albumTracks = tracks
                if (tracks.isEmpty()) albumError = "No playable audio files in this item."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                albumError = friendly(e)
            }
            albumLoading = false
        }
    }

    fun closeAlbum() {
        albumJob?.cancel()
        openAlbum = null
    }

    // ---------- Library ----------

    fun toggleFavourite(track: Track) {
        favourites = if (track.id in favouriteIds) {
            favourites.filter { it.id != track.id }
        } else {
            listOf(track) + favourites
        }
        favouriteIds = favourites.mapTo(HashSet()) { it.id }
        known[track.id] = track
        prefs.saveFavourites(favourites)
    }

    fun register(tracks: List<Track>) {
        tracks.forEach { known[it.id] = it }
    }

    fun trackFor(mediaId: String?): Track? = if (mediaId == null) null else known[mediaId]

    private fun friendly(e: Exception): String =
        if (e is IOException) "Can\u2019t reach the Internet Archive. Check your connection and try again."
        else e.message ?: "Something went wrong."
}
