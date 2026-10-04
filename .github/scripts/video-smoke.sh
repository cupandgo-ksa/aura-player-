set -euo pipefail
trap 'adb logcat -d > video-device.log 2>/dev/null || true; adb exec-out screencap -p > video-device.png 2>/dev/null || true' EXIT
adb shell setprop debug.hwui.renderer skiagl || true
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
adb shell pm set-permission-flags com.asvideo.offline android.permission.READ_MEDIA_VIDEO user-set user-fixed || true
adb shell pm grant com.asvideo.offline android.permission.POST_NOTIFICATIONS || true
adb logcat -c
adb shell am start -n com.asvideo.offline/.MainActivity --ez smokeTest true
for attempt in $(seq 1 40); do
  if adb logcat -d ASVideoTest:D '*:S' | grep -q 'VIDEO_FULLSCREEN_PASS'; then break; fi
  sleep 3
done
adb shell am start -n com.asvideo.offline/.MainActivity --activity-reorder-to-front --activity-single-top
sleep 3
adb logcat -d ASVideoTest:D ASVideo:E AndroidRuntime:E '*:S' > video-smoke.log
cat video-smoke.log
if [ "${VISUAL_CHECK:-0}" = "1" ]; then
  adb shell uiautomator dump /sdcard/as-video-ui.xml
  adb pull /sdcard/as-video-ui.xml video-ui.xml
  read -r tap_x tap_y < <(python3 - <<'PY'
import xml.etree.ElementTree as ET, re
root=ET.parse("video-ui.xml").getroot()
node=next(n for n in root.iter("node") if n.get("content-desc")=="Fullscreen")
x1,y1,x2,y2=map(int,re.findall(r"-?\d+",node.get("bounds","")))
print((x1+x2)//2,(y1+y2)//2)
PY
)
  adb shell input tap "$tap_x" "$tap_y"
  sleep 3
  adb exec-out screencap -p > video-fullscreen.png
  python3 - <<'PY'
import struct
data=open("video-fullscreen.png","rb").read()
width,height=struct.unpack(">II",data[16:24])
assert width>height,(width,height)
print("VIDEO_VISUAL_FULLSCREEN_PASS")
PY
  adb shell input keyevent 4
  sleep 3
fi
adb exec-out screencap -p > video-smoke.png
grep -q 'VIDEO_LIBRARY:1' video-smoke.log
grep -q 'VIDEO_PLAY_PASS' video-smoke.log
grep -q 'VIDEO_UI_PASS' video-smoke.log
for check in PAUSE SEEK SPEED FAVORITE RESUME VAULT TIMER FULLSCREEN SUBTITLE VOLUME BACKGROUND; do
  grep -q "VIDEO_${check}_PASS" video-smoke.log
done
adb logcat -d -s AndroidRuntime:E > video-crash.log
if grep -q 'FATAL EXCEPTION' video-crash.log; then cat video-crash.log; exit 1; fi
