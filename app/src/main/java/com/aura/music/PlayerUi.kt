package com.aura.music

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.delay

/** Compose-friendly mirror of the player's state. */
@Stable
class PlayerUi {
    var playing by mutableStateOf(false)          // user intent: play vs pause
    var buffering by mutableStateOf(false)
    var mediaId by mutableStateOf<String?>(null)
    var title by mutableStateOf("")
    var artist by mutableStateOf("")
    var artwork by mutableStateOf<Uri?>(null)
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)
    var shuffle by mutableStateOf(false)
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
    var errorTick by mutableIntStateOf(0)
    var errorText by mutableStateOf("")

    val hasItem: Boolean get() = mediaId != null

    fun sync(p: Player) {
        val item = p.currentMediaItem
        mediaId = item?.mediaId?.takeIf { it.isNotEmpty() }
        title = item?.mediaMetadata?.title?.toString() ?: ""
        artist = item?.mediaMetadata?.artist?.toString() ?: ""
        artwork = item?.mediaMetadata?.artworkUri
        val state = p.playbackState
        playing = p.playWhenReady && state != Player.STATE_IDLE && state != Player.STATE_ENDED
        buffering = p.playWhenReady && state == Player.STATE_BUFFERING
        val d = p.duration
        durationMs = if (d == C.TIME_UNSET || d < 0) 0L else d
        positionMs = p.currentPosition.coerceAtLeast(0L)
        shuffle = p.shuffleModeEnabled
        repeatMode = p.repeatMode
    }
}

fun togglePlay(p: Player) {
    if (p.playWhenReady && p.playbackState != Player.STATE_ENDED && p.playbackState != Player.STATE_IDLE) {
        p.pause()
    } else {
        if (p.playbackState == Player.STATE_IDLE) p.prepare()
        if (p.playbackState == Player.STATE_ENDED) p.seekToDefaultPosition()
        p.play()
    }
}

/** Replace the queue and start playing. */
fun MediaController.playTracks(vm: AuraViewModel, tracks: List<Track>, start: Int, shuffle: Boolean? = null) {
    if (tracks.isEmpty()) return
    vm.register(tracks)
    if (shuffle != null) shuffleModeEnabled = shuffle
    setMediaItems(tracks.map { it.toMediaItem() }, start.coerceIn(0, tracks.lastIndex), 0L)
    prepare()
    play()
}

/** Keeps [ui] in sync with the controller while this composable is on screen. */
@Composable
fun BindPlayer(player: MediaController?, ui: PlayerUi) {
    DisposableEffect(player) {
        if (player == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onEvents(p: Player, events: Player.Events) {
                    ui.sync(p)
                }

                override fun onPlayerError(error: PlaybackException) {
                    ui.errorText = "Couldn\u2019t play that track \u2014 skipping."
                    ui.errorTick++
                    if (player.hasNextMediaItem()) {
                        player.seekToNextMediaItem()
                        player.prepare()
                        player.play()
                    }
                }
            }
            player.addListener(listener)
            ui.sync(player)
            onDispose { player.removeListener(listener) }
        }
    }

    LaunchedEffect(player) {
        if (player != null) {
            while (true) {
                if (player.isPlaying) ui.sync(player)
                delay(500)
            }
        }
    }
}
