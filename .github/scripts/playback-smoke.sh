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
for attempt in 1 2 3 4 5; do
  adb shell mkdir -p /sdcard/Music/ASMusicSmoke || true
  if adb push /tmp/AS_Smoke_01.wav /sdcard/Music/ASMusicSmoke/AS_Smoke_01.wav &&
     adb push /tmp/AS_Smoke_02.wav /sdcard/Music/ASMusicSmoke/AS_Smoke_02.wav &&
     adb push /tmp/AS_Smoke_03.wav /sdcard/Music/ASMusicSmoke/AS_Smoke_03.wav; then break; fi
  if [ "$attempt" -eq 5 ]; then exit 1; fi
  sleep 4
done
for f in AS_Smoke_01.wav AS_Smoke_02.wav AS_Smoke_03.wav; do
  adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file:///sdcard/Music/ASMusicSmoke/$f" >/dev/null
done
sleep 4
adb shell pm grant com.auraplayer.offline android.permission.READ_MEDIA_AUDIO
adb shell pm grant com.auraplayer.offline android.permission.POST_NOTIFICATIONS || true
adb shell pm grant com.auraplayer.offline android.permission.RECORD_AUDIO
adb logcat -c
adb shell am force-stop com.auraplayer.offline
adb shell am start -n com.auraplayer.offline/.MainActivity --ez smokeTest true
for attempt in $(seq 1 40); do
  if adb logcat -d ASMusicTest:D '*:S' | grep -q 'SMOKE_CLIP_PASS'; then break; fi
  sleep 3
done
adb logcat -d ASMusicTest:D '*:S' > playback-smoke.log
cat playback-smoke.log
grep -q 'SMOKE_LIBRARY:' playback-smoke.log
grep -q 'SMOKE_FIRST_TAP_PASS' playback-smoke.log
grep -q 'SMOKE_TAP_SWITCH_PASS' playback-smoke.log
grep -q 'SMOKE_AUTO_NEXT_PASS' playback-smoke.log
grep -q 'SMOKE_QUEUE_PASS' playback-smoke.log
grep -q 'SMOKE_QUEUE_LAYOUT_PASS' playback-smoke.log
grep -q 'SMOKE_FAVORITE_PASS' playback-smoke.log
grep -q 'SMOKE_NO_BOX_PASS' playback-smoke.log
grep -q 'SMOKE_NEXT_BUTTON_PASS' playback-smoke.log
grep -q 'SMOKE_PREVIOUS_BUTTON_PASS' playback-smoke.log
grep -q 'SMOKE_SHUFFLE_BUTTON_PASS' playback-smoke.log
grep -q 'SMOKE_REPEAT_BUTTON_PASS' playback-smoke.log
grep -q 'SMOKE_BACKGROUND_PASS' playback-smoke.log
grep -q 'SMOKE_FFT_BANDS_PASS' playback-smoke.log
grep -q 'SMOKE_CLIP_PASS' playback-smoke.log
adb shell dumpsys media_session > media-session.txt
grep -q 'com.auraplayer.offline' media-session.txt
adb shell am start -n com.auraplayer.offline/.MainActivity
sleep 2
adb exec-out screencap -p > native-smoke.png
adb logcat -d -s AndroidRuntime:E > native-crash.log
if grep -q 'FATAL EXCEPTION' native-crash.log; then cat native-crash.log; exit 1; fi
