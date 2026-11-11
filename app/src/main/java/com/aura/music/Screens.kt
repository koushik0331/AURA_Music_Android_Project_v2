package com.aura.music

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.session.MediaController

private val genres = listOf("Jazz", "Classical", "Blues", "Ambient", "Rock", "Folk", "Electronic", "Soul", "Live concert")

// ------------------------------------------------------------------ Discover

@Composable
fun DiscoverScreen(vm: AuraViewModel, ui: PlayerUi, player: MediaController?) {
    val album = vm.openAlbum
    if (album != null) {
        AlbumScreen(vm, ui, player, album)
        return
    }
    val focus = LocalFocusManager.current

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 22.dp, end = 20.dp, top = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("A U R A", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Text("Feel every frequency.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
            Pill(
                text = if (vm.hifi) "HI-FI" else "DATA SAVER",
                selected = vm.hifi,
                onClick = { vm.toggleHifi() }
            )
        }

        Row(
            Modifier.padding(horizontal = 20.dp).fillMaxWidth()
                .clip(RoundedCornerShape(18.dp)).background(Panel).padding(start = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = Muted)
            TextField(
                value = vm.query,
                onValueChange = { vm.query = it },
                placeholder = { Text("Search songs, artists, recordings\u2026", color = Muted) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focus.clearFocus()
                    vm.runSearch()
                }),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Accent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            if (vm.query.isNotEmpty()) {
                IconButton(onClick = { vm.query = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Muted)
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Pill("Trending", selected = vm.query.isBlank(), onClick = {
                    focus.clearFocus()
                    vm.loadTrending()
                })
            }
            items(genres) { g ->
                Pill(g, selected = vm.query.equals(g, ignoreCase = true), onClick = {
                    focus.clearFocus()
                    vm.runSearch(g)
                })
            }
        }

        Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(vm.heading, Modifier.weight(1f))
            Text(
                if (vm.hifi) "Best available quality" else "Smaller MP3/OGG files",
                color = Accent, fontSize = 10.sp
            )
        }

        when {
            vm.loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            vm.error != null -> Column(
                Modifier.fillMaxWidth().weight(1f).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(vm.error ?: "", color = Muted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                ActionButton("TRY AGAIN", filled = true, onClick = { vm.retry() }, icon = Icons.Default.Refresh)
            }
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                items(vm.albums, key = { it.id }) { a ->
                    AlbumRow(a, onClick = { vm.open(a) })
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Album

@Composable
fun AlbumScreen(vm: AuraViewModel, ui: PlayerUi, player: MediaController?, album: Album) {
    val tracks = vm.albumTracks
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
        item {
            Column(Modifier.fillMaxWidth()) {
                IconButton(onClick = { vm.closeAlbum() }, modifier = Modifier.padding(start = 8.dp, top = 4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Cover(album.artwork, Modifier.fillMaxWidth(0.62f).aspectRatio(1f), radius = 20.dp)
                    Spacer(Modifier.height(18.dp))
                    Text(
                        album.title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis
                    )
                    Text(album.creator, color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2)
                }
                Spacer(Modifier.height(18.dp))
                if (tracks.isNotEmpty() && player != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        ActionButton("PLAY", filled = true, onClick = { player.playTracks(vm, tracks, 0, shuffle = false) })
                        Spacer(Modifier.width(12.dp))
                        ActionButton("SHUFFLE", filled = false, onClick = {
                            player.playTracks(vm, tracks, tracks.indices.random(), shuffle = true)
                        })
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        if (vm.albumLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent)
                }
            }
        }
        vm.albumError?.let { msg ->
            item {
                Text(msg, color = Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp))
            }
        }

        itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
            TrackRow(
                track = track,
                number = index + 1,
                current = ui.mediaId == track.id,
                favourite = track.id in vm.favouriteIds,
                onClick = { player?.playTracks(vm, tracks, index) },
                onFavourite = { vm.toggleFavourite(track) }
            )
        }
    }
}

// ------------------------------------------------------------------ Library

@Composable
fun LibraryScreen(vm: AuraViewModel, ui: PlayerUi, player: MediaController?) {
    val favs = vm.favourites
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 22.dp, end = 22.dp, top = 16.dp, bottom = 12.dp)) {
            Text("L I B R A R Y", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Text("Your favourites", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        }
        if (favs.isEmpty()) {
            EmptyState(
                Icons.Default.FavoriteBorder,
                "Nothing here yet",
                "Tap the heart next to any track and it will be saved here, ready to play again."
            )
        } else {
            if (player != null) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    ActionButton("PLAY ALL", filled = true, onClick = { player.playTracks(vm, favs, 0, shuffle = false) })
                    Spacer(Modifier.width(12.dp))
                    ActionButton("SHUFFLE", filled = false, onClick = {
                        player.playTracks(vm, favs, favs.indices.random(), shuffle = true)
                    })
                }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                itemsIndexed(favs, key = { _, t -> t.id }) { index, track ->
                    TrackRow(
                        track = track,
                        number = index + 1,
                        current = ui.mediaId == track.id,
                        favourite = true,
                        onClick = { player?.playTracks(vm, favs, index) },
                        onFavourite = { vm.toggleFavourite(track) }
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Now playing

@Composable
fun NowPlayingScreen(vm: AuraViewModel, ui: PlayerUi, player: MediaController, onClose: () -> Unit) {
    val track = vm.trackFor(ui.mediaId)
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = ui.durationMs.coerceAtLeast(1L).toFloat()
    val position = dragging ?: ui.positionMs.toFloat().coerceIn(0f, duration)

    Column(
        Modifier.fillMaxSize().background(Ink).statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.weight(1f))
            Text("NOW PLAYING", color = Accent, letterSpacing = 3.sp, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(48.dp))
        }

        Spacer(Modifier.weight(1f))
        Cover(ui.artwork, Modifier.fillMaxWidth().aspectRatio(1f), radius = 24.dp)
        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    ui.title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(ui.artist, color = Muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (track != null) {
                    Spacer(Modifier.height(8.dp))
                    QualityBadge(track)
                }
            }
            if (track != null) {
                val fav = track.id in vm.favouriteIds
                IconButton(onClick = { vm.toggleFavourite(track) }) {
                    Icon(
                        if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (fav) "Remove from library" else "Add to library",
                        tint = if (fav) Accent else Muted,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Slider(
            value = position,
            onValueChange = { dragging = it },
            modifier = Modifier.fillMaxWidth(),
            onValueChangeFinished = {
                dragging?.let { player.seekTo(it.toLong()) }
                dragging = null
            },
            valueRange = 0f..duration,
            colors = SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = Line
            )
        )
        Row(Modifier.fillMaxWidth()) {
            Text(formatMillis(position.toLong()), color = Muted, fontSize = 11.sp)
            Spacer(Modifier.weight(1f))
            Text(if (ui.durationMs > 0) formatMillis(ui.durationMs) else "--:--", color = Muted, fontSize = 11.sp)
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { player.shuffleModeEnabled = !ui.shuffle }) {
                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = if (ui.shuffle) Accent else Muted)
            }
            IconButton(onClick = { player.seekToPrevious() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(34.dp))
            }
            Box(
                Modifier.size(68.dp).clip(CircleShape).background(Accent).clickable { togglePlay(player) },
                contentAlignment = Alignment.Center
            ) {
                if (ui.buffering) {
                    CircularProgressIndicator(color = Ink, strokeWidth = 3.dp, modifier = Modifier.size(30.dp))
                } else {
                    Icon(
                        if (ui.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (ui.playing) "Pause" else "Play",
                        tint = Ink,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
            IconButton(onClick = { player.seekToNext() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(34.dp))
            }
            IconButton(onClick = {
                player.repeatMode = when (ui.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
            }) {
                Icon(
                    if (ui.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = "Repeat",
                    tint = if (ui.repeatMode == Player.REPEAT_MODE_OFF) Muted else Accent
                )
            }
        }
        Spacer(Modifier.weight(1f))
    }
}
