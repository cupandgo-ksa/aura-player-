set -euo pipefail
adb install -r videoapp/build/outputs/apk/debug/videoapp-debug.apk
for attempt in 1 2 3 4 5; do
  adb shell mkdir -p /sdcard/Movies/ASVideoSmoke || true
  if adb push videoapp/src/test/assets/smoke.mp4 /sdcard/Movies/ASVideoSmoke/AS_Video_Smoke.mp4; then break; fi
  if [ "$attempt" -eq 5 ]; then exit 1; fi
  sleep 4
done
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Movies/ASVideoSmoke/AS_Video_Smoke.mp4 >/dev/null
sleep 4
adb shell content query --uri content://media/external/video/media --projection _id:_display_name | head -5 || true
adb shell pm grant com.asvideo.offline android.permission.READ_MEDIA_VIDEO
adb shell pm grant com.asvideo.offline android.permission.POST_NOTIFICATIONS || true
adb logcat -c
adb shell am start -n com.asvideo.offline/.MainActivity --ez smokeTest true
sleep 25
adb logcat -d ASVideoTest:D ASVideo:E AndroidRuntime:E '*:S' > video-smoke.log
cat video-smoke.log
adb exec-out screencap -p > video-smoke.png
grep -q 'VIDEO_LIBRARY:1' video-smoke.log
grep -q 'VIDEO_PLAY_PASS' video-smoke.log
grep -q 'VIDEO_UI_PASS' video-smoke.log
for check in PAUSE SEEK SPEED FAVORITE RESUME VAULT TIMER FULLSCREEN SUBTITLE; do
  grep -q "VIDEO_${check}_PASS" video-smoke.log
done
adb logcat -d -s AndroidRuntime:E > video-crash.log
if grep -q 'FATAL EXCEPTION' video-crash.log; then cat video-crash.log; exit 1; fi
