# AURA Player — personal offline Android player

An ad-free, local audio/video player. No account, analytics SDK, ad SDK, or `INTERNET` permission. Selected file references and playback positions are stored only on the device. Choose files saved on the device in the system picker; a cloud provider may require its own connection.

## Build

**Phone only:** See `ফোনে-APK-বানানোর-নির্দেশনা.md` for the Code on the Go steps.

**GitHub cloud build:** The included `.github/workflows/build-apk.yml` can compile an installable debug APK in a separate private repository using GitHub Actions. Place the *contents* of this folder at the repository root. On a successful workflow run, download the `AURA-Player-APK` artifact. Do not add this personal app to an unrelated public repository.

The private repository includes a stable *debug-only* keystore so future test APKs install over this version. Keep the repository private. Users of the very first APK (built before this key was added) need to uninstall that first version once before installing this one; uninstalling clears its app data and imported-file list.

Open this folder in Android Studio. Install Android SDK 35 and JDK 17 if prompted; let Android Studio sync the project with Gradle 8.9 (Android Gradle Plugin 8.7.3). Build > Build Bundle(s) / APK(s) > Build APK(s), then install the debug APK on your phone. This source package has no Gradle wrapper or prebuilt APK. Initial build needs Gradle/plugin downloads; the installed player itself runs offline.

## Use

Grant audio/video library permission to scan media stored on the phone. You can also tap **Add files** to choose specific local files without that permission. Use Videos/Music tabs, folder filtering, search and sorting. The player remembers positions; moved or deleted files may need to be selected again. Long-press a library item to share it or remove it from the list without deleting the original file.

Included: a dark cinematic library, video thumbnails where supported, local playlist, seek, 10-second jumps, next/previous, shuffle/repeat, four soft glass themes, playback speed, local `.srt` subtitle picker, swipe volume/brightness/seek, screen lock, video fit/zoom, fullscreen and Android picture-in-picture on supported devices. Codec support depends on the phone. Playback outside picture-in-picture is foreground only. Casting, subtitle downloading, encrypted private folder and background audio are not included; they require separate implementations and some require internet access.
