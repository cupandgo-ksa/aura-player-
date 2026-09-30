set -euo pipefail
python3 - <<'PY'
import wave, math, struct
for n, hz in enumerate((440, 554, 659), start=1):
    path=f"/tmp/AS_Smoke_0{n}.wav"
    rate=8000
    seconds=3
    with wave.open(path,"w") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(rate)
        frames=[]
        for i in range(rate*seconds):
            v=int(9000*math.sin(2*math.pi*hz*i/rate))
            frames.append(struct.pack("<h",v))
        w.writeframes(b"".join(frames))
PY
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell mkdir -p /sdcard/Music/ASMusicSmoke
adb push /tmp/AS_Smoke_01.wav /sdcard/Music/ASMusicSmoke/AS_Smoke_01.wav
adb push /tmp/AS_Smoke_02.wav /sdcard/Music/ASMusicSmoke/AS_Smoke_02.wav
adb push /tmp/AS_Smoke_03.wav /sdcard/Music/ASMusicSmoke/AS_Smoke_03.wav
for f in AS_Smoke_01.wav AS_Smoke_02.wav AS_Smoke_03.wav; do
  adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file:///sdcard/Music/ASMusicSmoke/$f" >/dev/null
done
sleep 4
adb shell pm grant com.auraplayer.offline android.permission.READ_MEDIA_AUDIO
adb shell pm grant com.auraplayer.offline android.permission.POST_NOTIFICATIONS || true
adb logcat -c
adb shell am force-stop com.auraplayer.offline
adb shell am start -n com.auraplayer.offline/.MainActivity --ez smokeTest true
sleep 15
adb logcat -d ASMusicTest:D '*:S' > playback-smoke.log
cat playback-smoke.log
grep -q 'SMOKE_LIBRARY:' playback-smoke.log
grep -q 'SMOKE_FIRST_TAP_PASS' playback-smoke.log
grep -q 'SMOKE_TAP_SWITCH_PASS' playback-smoke.log
grep -q 'SMOKE_AUTO_NEXT_PASS' playback-smoke.log
grep -q 'SMOKE_NEXT_BUTTON_PASS' playback-smoke.log
grep -q 'SMOKE_PREVIOUS_BUTTON_PASS' playback-smoke.log
grep -q 'SMOKE_SHUFFLE_BUTTON_PASS' playback-smoke.log
grep -q 'SMOKE_REPEAT_BUTTON_PASS' playback-smoke.log
grep -q 'SMOKE_BACKGROUND_PASS' playback-smoke.log
adb shell dumpsys media_session > media-session.txt
grep -q 'com.auraplayer.offline' media-session.txt
adb shell am start -n com.auraplayer.offline/.MainActivity
sleep 2
adb exec-out screencap -p > native-smoke.png
adb logcat -d -s AndroidRuntime:E > native-crash.log
if grep -q 'FATAL EXCEPTION' native-crash.log; then cat native-crash.log; exit 1; fi
