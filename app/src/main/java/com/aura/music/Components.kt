package com.aura.music

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController
import coil.compose.AsyncImage

@Composable
fun Cover(model: Any?, modifier: Modifier = Modifier, radius: Dp = 12.dp) {
    Box(
        modifier.clip(RoundedCornerShape(radius)).background(Raised),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.MusicNote, null, tint = Muted.copy(alpha = 0.5f), modifier = Modifier.fillMaxSize(0.4f))
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun QualityBadge(track: Track, modifier: Modifier = Modifier) {
    val text = when {
        track.lossless -> track.format
        track.kbps > 0 -> "${track.format} ${track.kbps}k"
        else -> track.format
    }
    val shape = RoundedCornerShape(5.dp)
    Text(
        text,
        color = if (track.lossless) Ink else Muted,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = modifier
            .clip(shape)
            .background(if (track.lossless) Accent else Color.Transparent)
            .border(1.dp, if (track.lossless) Accent else Line, shape)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Text(
        text,
        color = if (selected) Ink else Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(shape)
            .background(if (selected) Accent else Panel)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = modifier)
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, color = Muted, fontSize = 13.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun ActionButton(
    text: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = if (filled) Icons.Default.PlayArrow else Icons.Default.Shuffle
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .clip(shape)
            .background(if (filled) Accent else Color.Transparent)
            .border(1.dp, if (filled) Accent else Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            null,
            tint = if (filled) Ink else Accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, color = if (filled) Ink else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun AlbumRow(album: Album, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Cover(album.artwork, Modifier.size(58.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                album.title, color = Color.White, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                album.creator, color = Muted, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (album.downloads > 0) {
                Text("${formatCount(album.downloads)} plays", color = Muted.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun TrackRow(
    track: Track,
    number: Int,
    current: Boolean,
    favourite: Boolean,
    onClick: () -> Unit,
    onFavourite: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 20.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.CenterStart) {
            if (current) {
                Icon(Icons.Default.GraphicEq, null, tint = Accent, modifier = Modifier.size(18.dp))
            } else {
                Text("$number", color = Muted, fontSize = 12.sp)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                track.title,
                color = if (current) Accent else Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val bits = listOf(
                track.artist,
                if (track.durationSec > 0) formatSeconds(track.durationSec) else "",
                formatSize(track.sizeBytes)
            ).filter { it.isNotBlank() }
            Text(bits.joinToString(" \u00B7 "), color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        QualityBadge(track)
        IconButton(onClick = onFavourite) {
            Icon(
                if (favourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (favourite) "Remove from library" else "Add to library",
                tint = if (favourite) Accent else Muted
            )
        }
    }
}

@Composable
fun MiniPlayer(ui: PlayerUi, player: MediaController, onOpen: () -> Unit) {
    val progress = if (ui.durationMs > 0) (ui.positionMs.toFloat() / ui.durationMs).coerceIn(0f, 1f) else 0f
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp)).background(Raised).clickable(onClick = onOpen)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(ui.artwork, Modifier.size(46.dp), radius = 10.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(ui.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ui.artist, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { togglePlay(player) }) {
                if (ui.buffering) {
                    CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Icon(
                        if (ui.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (ui.playing) "Pause" else "Play",
                        tint = Accent
                    )
                }
            }
            IconButton(onClick = { player.seekToNext() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White)
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = Accent,
            trackColor = Line
        )
    }
}
