package com.aura.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Talks to the Internet Archive.
 *
 * Search is ONE request (fast). The file list of an item is only fetched when the
 * person opens it, and there we pick the best-quality playable file for every track.
 */
class ArchiveRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    private val trackCache = HashMap<String, List<Track>>()
    private val kbpsRegex = Regex("(\\d+)\\s*kbps", RegexOption.IGNORE_CASE)

    // ---------- Discovery ----------

    suspend fun trending(): List<Album> {
        val first = try {
            fetchAlbums("mediatype:audio AND collection:(audio_music OR netlabels OR 78rpm)")
        } catch (e: IOException) {
            emptyList()
        } catch (e: JSONException) {
            emptyList()
        }
        if (first.isNotEmpty()) return first
        return fetchAlbums("mediatype:audio AND subject:music")
    }

    suspend fun search(text: String): List<Album> {
        val clean = text.replace(Regex("[\\\\\"(){}\\[\\]^~:!]"), " ").trim()
        if (clean.isEmpty()) return emptyList()
        return fetchAlbums("($clean) AND mediatype:audio")
    }

    private suspend fun fetchAlbums(q: String): List<Album> = withContext(Dispatchers.IO) {
        val url = "https://archive.org/advancedsearch.php".toHttpUrl().newBuilder()
            .addQueryParameter("q", q)
            .addQueryParameter("fl[]", "identifier")
            .addQueryParameter("fl[]", "title")
            .addQueryParameter("fl[]", "creator")
            .addQueryParameter("fl[]", "downloads")
            .addQueryParameter("sort[]", "downloads desc")
            .addQueryParameter("rows", "40")
            .addQueryParameter("page", "1")
            .addQueryParameter("output", "json")
            .build()
        val docs = JSONObject(get(url.toString())).getJSONObject("response").getJSONArray("docs")
        val out = ArrayList<Album>()
        for (i in 0 until docs.length()) {
            val d = docs.optJSONObject(i) ?: continue
            val id = d.text("identifier")
            if (id.isBlank()) continue
            out.add(
                Album(
                    id = id,
                    title = d.text("title", id),
                    creator = d.text("creator", "Unknown artist"),
                    downloads = d.optInt("downloads", 0)
                )
            )
        }
        out
    }

    // ---------- Tracks of one item ----------

    suspend fun tracks(album: Album, hifi: Boolean): List<Track> {
        val key = "${album.id}|$hifi"
        trackCache[key]?.let { return it }
        val list = withContext(Dispatchers.IO) {
            parseTracks(album, hifi, JSONObject(get("https://archive.org/metadata/${album.id}")))
        }
        trackCache[key] = list
        return list
    }

    private class Candidate(
        val name: String,
        val key: String,
        val label: String,
        val lossless: Boolean,
        val kbps: Int,
        val size: Long,
        val title: String,
        val trackNo: Int,
        val lengthSec: Int,
        val creator: String
    )

    private fun parseTracks(album: Album, hifi: Boolean, meta: JSONObject): List<Track> {
        val files = meta.optJSONArray("files") ?: return emptyList()

        // Group every playable file by the recording it belongs to, so a FLAC original,
        // its VBR-MP3 derivative and its 64k derivative all end up in the same group.
        val groups = LinkedHashMap<String, MutableList<Candidate>>()
        for (i in 0 until files.length()) {
            val f = files.optJSONObject(i) ?: continue
            val c = candidate(f) ?: continue
            groups.getOrPut(c.key) { mutableListOf() }.add(c)
        }

        val best = ArrayList<Pair<Candidate, List<Candidate>>>()
        for ((_, group) in groups) {
            val chosen = pick(group, hifi) ?: continue
            if (chosen.size in 1L..40_000L) continue          // tiny clips / samples
            best.add(chosen to group)
        }
        best.sortWith(compareBy({ it.first.trackNo }, { it.first.name.lowercase() }))

        return best.take(300).map { (c, group) ->
            val title = c.title.ifBlank { group.firstOrNull { it.title.isNotBlank() }?.title ?: prettyName(c.name) }
            val length = if (c.lengthSec > 0) c.lengthSec else group.firstOrNull { it.lengthSec > 0 }?.lengthSec ?: 0
            Track(
                id = "${album.id}/${c.name}",
                albumId = album.id,
                title = title,
                artist = c.creator.ifBlank { album.creator },
                album = album.title,
                streamUrl = "https://archive.org/download/${album.id}/${encodePath(c.name)}",
                format = c.label,
                kbps = c.kbps,
                lossless = c.lossless,
                durationSec = length,
                sizeBytes = c.size
            )
        }
    }

    private fun candidate(f: JSONObject): Candidate? {
        val name = f.optString("name")
        if (name.isBlank() || name.contains("__ia_thumb")) return null
        val format = f.optString("format")
        val fmtLower = format.lowercase()
        val ext = name.substringAfterLast('.', "").lowercase()

        // Only formats Android's ExoPlayer can really decode.
        val (label, lossless) = when (ext) {
            "flac" -> "FLAC" to true
            "wav" -> "WAV" to true
            "mp3" -> "MP3" to false
            "ogg", "oga" -> "OGG" to false
            "opus" -> "OPUS" to false
            "m4a" -> if (fmtLower.contains("lossless")) return null else "AAC" to false
            else -> return null
        }

        val kbps = if (lossless) 0 else {
            f.optString("bitrate").toDoubleOrNull()?.toInt()?.takeIf { it > 0 }
                ?: kbpsRegex.find(format)?.groupValues?.get(1)?.toIntOrNull()
                ?: when {
                    fmtLower.contains("vbr") -> 220
                    label == "OGG" -> 160
                    label == "OPUS" -> 96
                    else -> 128
                }
        }

        val origin = f.optString("original").ifBlank { name }
        return Candidate(
            name = name,
            key = origin.substringBeforeLast('.').lowercase(),
            label = label,
            lossless = lossless,
            kbps = kbps,
            size = f.optString("size").toLongOrNull() ?: 0L,
            title = f.optString("title").trim(),
            trackNo = f.optString("track").substringBefore('/').trim().toIntOrNull() ?: Int.MAX_VALUE,
            lengthSec = parseLength(f.optString("length")),
            creator = f.text("creator").trim()
        )
    }

    private fun pick(group: List<Candidate>, hifi: Boolean): Candidate? {
        if (hifi) {
            // Lossless first (FLAC over WAV), otherwise the highest bitrate.
            return group.maxByOrNull { if (it.lossless) (if (it.label == "FLAC") 10_000 else 9_900) else it.kbps }
        }
        // Data saver: best lossy file up to 256 kbps, else the smallest thing we have.
        val lossy = group.filter { !it.lossless }
        return lossy.filter { it.kbps <= 256 }.maxByOrNull { it.kbps }
            ?: lossy.minByOrNull { it.kbps }
            ?: group.minByOrNull { it.size }
    }

    // ---------- helpers ----------

    private fun parseLength(s: String): Int {
        if (s.isBlank()) return 0
        s.toDoubleOrNull()?.let { return it.toInt() }
        val parts = s.split(":").mapNotNull { it.toDoubleOrNull() }
        if (parts.isEmpty()) return 0
        return parts.fold(0.0) { acc, p -> acc * 60 + p }.toInt()
    }

    private fun prettyName(path: String): String =
        path.substringAfterLast('/').substringBeforeLast('.').replace('_', ' ').trim().ifBlank { "Untitled" }

    /** Archive fields are sometimes a string and sometimes an array of strings. */
    private fun JSONObject.text(key: String, fallback: String = ""): String {
        val v = opt(key) ?: return fallback
        return when (v) {
            is JSONArray -> (0 until v.length()).joinToString(", ") { v.optString(it) }.ifBlank { fallback }
            JSONObject.NULL -> fallback
            else -> v.toString().ifBlank { fallback }
        }
    }

    private fun get(url: String): String {
        var last: IOException? = null
        repeat(2) {
            try {
                val request = Request.Builder().url(url).header("User-Agent", "AURAMusicAndroid/2.0").build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Archive.org returned ${response.code}")
                    return response.body?.string() ?: throw IOException("Empty response")
                }
            } catch (e: IOException) {
                last = e
            }
        }
        throw last ?: IOException("Network error")
    }

    private fun encodePath(path: String): String =
        path.split("/").joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
}
