package com.aura.music

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import java.io.File

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var cache: SimpleCache? = null

    override fun onCreate() {
        super.onCreate()

        // Everything you stream is cached on disk (up to 1 GB, oldest evicted first),
        // so replaying a track costs no mobile data and starts instantly.
        val diskCache = SimpleCache(
            File(cacheDir, "audio"),
            LeastRecentlyUsedCacheEvictor(1024L * 1024L * 1024L),
            StandaloneDatabaseProvider(this)
        )
        cache = diskCache

        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("AURAMusicAndroid/2.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)

        val dataSource = CacheDataSource.Factory()
            .setCache(diskCache)
            .setUpstreamDataSourceFactory(http)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSource))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)          // pause when headphones are unplugged
            .setWakeMode(C.WAKE_MODE_NETWORK)           // keep streaming with the screen off
            .setLoadControl(
                // Bigger buffer so lossless files ride out patchy mobile data.
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(30_000, 120_000, 2_000, 5_000)
                    .build()
            )
            .build()

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openApp)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Swiping the app away keeps music going, but stops the service if nothing is playing.
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        cache?.release()
        cache = null
        super.onDestroy()
    }
}
