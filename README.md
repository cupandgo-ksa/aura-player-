# A S media-player

Personal, offline Android audio player built with Kotlin, Jetpack Compose, Media3 ExoPlayer/MediaSessionService, Room, DataStore and MediaStore.

Open this project in Android Studio or run `gradle :app:assembleDebug`. The debug APK uses the same private development keystore as the earlier personal build so it can be installed as an update. Keep that key private.

The app requests only local audio access and uses no internet permission. Media3 owns playback and its notification independently of the Compose activity. Home/back minimizes the UI and leaves active playback in the service. The player supports queues, local playlists, favorites, pinned songs, a sleep timer, offline LRC import, speed, EQ presets, themes and library filters.

Crossfade, loudness normalization and automated tempo matching are intentionally shown as unavailable until a compatible, tested audio implementation exists. The prompt asked to avoid fake premium switches.
