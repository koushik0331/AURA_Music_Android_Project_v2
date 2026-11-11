package com.aura.music

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

class MainActivity : ComponentActivity() {

    private var controller by mutableStateOf<MediaController?>(null)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* nothing to do */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        // Needed on Android 13+ for the lock-screen / notification player controls.
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener({
            try {
                controller = future.get()
            } catch (e: Exception) {
                Toast.makeText(this, "Couldn\u2019t start the player.", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))

        setContent { AuraApp(controller) }
    }

    override fun onDestroy() {
        controller?.release()
        controller = null
        super.onDestroy()
    }
}

@Composable
fun AuraApp(player: MediaController?) {
    val vm: AuraViewModel = viewModel()
    val ui = remember { PlayerUi() }
    BindPlayer(player, ui)

    var tab by rememberSaveable { mutableStateOf(0) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(ui.errorTick) {
        if (ui.errorTick > 0) Toast.makeText(context, ui.errorText, Toast.LENGTH_SHORT).show()
    }

    // Registered first = lowest priority: the full-screen player closes before the album does.
    BackHandler(enabled = tab == 0 && vm.openAlbum != null) { vm.closeAlbum() }
    BackHandler(enabled = expanded) { expanded = false }

    AuraTheme {
        Box(Modifier.fillMaxSize().background(Ink)) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().statusBarsPadding()) {
                    if (tab == 0) DiscoverScreen(vm, ui, player) else LibraryScreen(vm, ui, player)
                }
                if (ui.hasItem && player != null) {
                    MiniPlayer(ui, player, onOpen = { expanded = true })
                }
                NavigationBar(containerColor = Panel) {
                    val colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Ink,
                        selectedTextColor = Accent,
                        indicatorColor = Accent,
                        unselectedIconColor = Muted,
                        unselectedTextColor = Muted
                    )
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Default.Explore, contentDescription = null) },
                        label = { Text("Discover") },
                        colors = colors
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        label = { Text("Library") },
                        colors = colors
                    )
                }
            }

            if (player != null) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    NowPlayingScreen(vm, ui, player, onClose = { expanded = false })
                }
            }
        }
    }
}
