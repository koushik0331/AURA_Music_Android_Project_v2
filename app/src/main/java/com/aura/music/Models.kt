package com.aura.music

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.json.JSONObject

/** One Internet Archive item (an album, a live show, a 78rpm record, ...). */
data class Album(
    val id: String,
    val title: String,
    val creator: String,
    val downloads: Int = 0
) {
    val artwork: String get() = "https://archive.org/services/img/$id"
}

/** One playable audio file, already resolved to the best quality available. */
data class Track(
    val id: String,          // "<albumId>/<fileName>" - also used as the player's mediaId
    val albumId: String,
    val title: String,
    val artist: String,
    val album: String,
    val streamUrl: String,
    val format: String,      // FLAC, WAV, MP3, OGG, OPUS, AAC
    val kbps: Int,           // 0 for lossless
    val lossless: Boolean,
    val durationSec: Int,    // 0 when unknown
    val sizeBytes: Long      // 0 when unknown
) {
    val artwork: String get() = "https://archive.org/services/img/$albumId"

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("albumId", albumId).put("title", title)
        .put("artist", artist).put("album", album).put("url", streamUrl)
        .put("format", format).put("kbps", kbps).put("lossless", lossless)
        .put("dur", durationSec).put("size", sizeBytes)

    companion object {
        fun fromJson(o: JSONObject) = Track(
            id = o.getString("id"),
            albumId = o.getString("albumId"),
            title = o.getString("title"),
            artist = o.optString("artist"),
            album = o.optString("album"),
            streamUrl = o.getString("url"),
            format = o.optString("format"),
            kbps = o.optInt("kbps"),
            lossless = o.optBoolean("lossless"),
            durationSec = o.optInt("dur"),
            sizeBytes = o.optLong("size")
        )
    }
}

fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id)
    .setUri(streamUrl)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(Uri.parse(artwork))
            .build()
    )
    .build()

fun formatSeconds(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "$m:${if (s < 10) "0$s" else "$s"}"
}

fun formatMillis(ms: Long): String = formatSeconds((ms / 1000).toInt().coerceAtLeast(0))

fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> "${n / 1_000_000}.${(n % 1_000_000) / 100_000}M"
    n >= 1_000 -> "${n / 1_000}.${(n % 1_000) / 100}K"
    else -> "$n"
}

fun formatSize(bytes: Long): String = if (bytes <= 0) "" else "${(bytes / 1_048_576).coerceAtLeast(1)} MB"
