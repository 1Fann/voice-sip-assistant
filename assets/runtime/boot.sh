#!/system/bin/sh
# voice-sip-assistant-owned
# Start explicitly enabled monitoring / Shizuku preparation. Never place calls.
BASE=/data/adb/voice-sip-assistant
[ -f "$BASE/.owned" ] || exit 0
[ -f "$BASE/autostart" ] || [ -f "$BASE/health-enabled" ] || exit 0
(
 for attempt in $(seq 1 180); do
  [ "$(getprop sys.boot_completed)" = 1 ] && [ "$(cmd activity get-started-user-state 0 2>/dev/null)" = RUNNING_UNLOCKED ] && break
  sleep 5
 done
 [ "$(getprop sys.boot_completed)" = 1 ] || exit 0
 [ "$(cmd activity get-started-user-state 0 2>/dev/null)" = RUNNING_UNLOCKED ] || exit 0
 if [ -f "$BASE/health-enabled" ]; then
  /data/adb/ksu/bin/busybox setsid /system/bin/sh "$BASE/health.sh" loop </dev/null >/dev/null 2>&1 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&- &
 fi
 [ -f "$BASE/autostart" ] && [ -z "$(pidof shizuku_server)" ] || exit 0
 APK=$(pm path moe.shizuku.privileged.api 2>/dev/null | sed -n 's/^package://p' | head -n 1)
 [ -n "$APK" ] && [ -f "$APK" ] && [ -x "$BASE/shizuku_starter" ] || exit 0
 "$BASE/shizuku_starter" --apk="$APK" </dev/null >/dev/null 2>&1
) </dev/null >/dev/null 2>&1 &
