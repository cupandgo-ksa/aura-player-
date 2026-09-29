# AURA Player — personal offline Android player

An ad-free, local audio/video player. No account, analytics SDK, ad SDK, or `INTERNET` permission. Selected file references and playback positions are stored only on the device. Choose files saved on the device in the system picker; a cloud provider may require its own connection.

## Build

**Phone only:** See `ফোনে-APK-বানানোর-নির্দেশনা.md` for the Code on the Go steps.

**GitHub cloud build:** The included `.github/workflows/build-apk.yml` can compile an installable debug APK in a separate private repository using GitHub Actions. Place the *contents* of this folder at the repository root. On a successful workflow run, download the `AURA-Player-APK` artifact. Do not add this personal app to an unrelated public repository.

Open this folder in Android Studio. Install Android SDK 35 and JDK 17 if prompted; let Android Studio sync the project with Gradle 8.9 (Android Gradle Plugin 8.7.3). Build > Build Bundle(s) / APK(s) > Build APK(s), then install the debug APK on your phone. This source package has no Gradle wrapper or prebuilt APK. Initial build needs Gradle/plugin downloads; the installed player itself runs offline.

## Use

Tap **Add local files** and select several audio/video files. Tap any item to play. The list persists across launches. The player remembers the last position of each file; a file moved or deleted may need to be added again. Tap a theme chip to change the color scheme. Long-press a library item to remove it from the list without deleting the original file.

Included: audio/video playback using Android's built-in media decoders, local playlist, seek, 10-second jumps, next/previous, shuffle, repeat, four soft glass themes and video fullscreen. Supported codecs and containers depend on the Android device. Playback is foreground only; background audio, subtitles, speed and sleep timer are not in this first build.
