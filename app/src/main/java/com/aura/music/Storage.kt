package com.aura.music

import android.content.Context
import org.json.JSONArray

/** Tiny SharedPreferences wrapper: favourites + the quality switch. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("aura", Context.MODE_PRIVATE)

    var hifi: Boolean
        get() = sp.getBoolean("hifi", true)
        set(value) { sp.edit().putBoolean("hifi", value).apply() }

    fun loadFavourites(): List<Track> {
        val raw = sp.getString("favs", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<Track>()
            for (i in 0 until arr.length()) {
                try { out.add(Track.fromJson(arr.getJSONObject(i))) } catch (e: Exception) { /* skip bad row */ }
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveFavourites(list: List<Track>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        sp.edit().putString("favs", arr.toString()).apply()
    }
}
