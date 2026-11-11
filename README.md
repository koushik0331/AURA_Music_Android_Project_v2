# AURA Music 2.0

A dark, premium-style Android music player built with Kotlin, Jetpack Compose and Media3.
It streams the Internet Archive's public audio catalogue and now picks the **best quality file** it can find.

## What's new in 2.0
- **Real quality selection.** For every track the app compares all the files an item offers and plays
  FLAC / WAV (lossless) first, otherwise the highest-bitrate MP3/OGG/Opus. A badge on each track shows what you get
  (`FLAC`, `MP3 245k`, ...), plus the file size.
- **HI-FI / DATA SAVER switch** (top right). Data saver picks smaller MP3/OGG files (up to 256 kbps).
- **Fast search.** One request instead of up to 25, and a Trending feed plus genre chips on the home screen.
- **Albums and full track lists.** Open any item to see all its tracks, then Play or Shuffle.
- **Proper player.** Seek bar, shuffle, repeat (off / all / one), previous/next, and a tidy mini-player with progress.
- **Library.** Tap the heart on any track to save it; favourites are kept on the phone.
- **Disk cache (up to 1 GB).** Anything you stream is cached, so replays are instant and use no data.
- **Background playback done right.** Notification and lock-screen controls (the app now asks for the notification
  permission), audio focus, pause on headphone unplug, keep-streaming with the screen off.
- **Skips broken files** instead of getting stuck, and shows a friendly message when the network is down.
- **Polish.** App icon, dark theme from the first frame, edge-to-edge layout.
- **Update-friendly APKs.** A fixed debug key is included, so a new build installs over the old one
  (no uninstall needed).

## Rights and quality note
The Internet Archive is a hosting platform, not a blanket music licence. Availability and reuse rights vary by item,
so check an item's rights information before sharing or reusing recordings. This app does not bypass subscriptions
or DRM. Quality is whatever the uploader provided: many items only have MP3, and the app can never be better than the
file the Archive has.

## Build an APK without installing Android Studio
1. Create a new GitHub repository (public or private).
2. Upload the **contents** of this folder to the repository root (make sure the hidden `.github` folder goes up too).
3. Open **Actions -> Build AURA Music APK -> Run workflow**.
4. When it finishes, open the run and download the `AURA-Music-debug` artifact, unzip it and install `app-debug.apk`.

## Phone tips (Xiaomi / POCO / HyperOS)
- Allow notifications when the app asks, otherwise there are no lock-screen controls.
- Settings -> Apps -> AURA Music -> Battery saver -> **No restrictions**, and enable **Autostart**,
  or HyperOS may stop the music when the screen is off.
- FLAC files are large (20-40 MB per track). Use Wi-Fi for HI-FI, or switch to DATA SAVER on mobile data.

## Local build
Requires JDK 17, Android SDK 35 and Gradle 8.9: `gradle assembleDebug`

## Ideas for next steps
- Playlists, listening history and a sleep timer.
- Equalizer and gapless/crossfade tuning.
- More catalogues through approved APIs, behind the same provider interface.
