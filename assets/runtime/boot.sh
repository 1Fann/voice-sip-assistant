#!/system/bin/sh
# voice-sip-assistant-owned
# Start explicitly enabled monitoring / Shizuku preparation. Never place calls.
BASE=/data/adb/voice-sip-assistant
BB=
for candidate in /data/adb/magisk/busybox /data/adb/ksu/bin/busybox /data/adb/ap/bin/busybox; do
  if [ -x "$candidate" ]; then BB=$candidate; break; fi
done
[ -n "$BB" ] || { echo busybox_missing; exit 20; }
[ -f "$BASE/.owned" ] || exit 0
[ -x "$BB" ] || exit 0
[ -f "$BASE/autostart" ] || [ -f "$BASE/health-enabled" ] || exit 0
if [ "${1:-}" != --worker ]; then
 "$BB" setsid "$BB" flock -n "$BASE/boot.lock" /system/bin/sh "$0" --worker </dev/null >"$BASE/boot.log" 2>&1 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&- &
 exit 0
fi
# RUNNING_UNLOCKED refers to decrypted app storage, not dismissing the lock screen.
# With no screen credential Android normally reaches this state automatically.
# Keep waiting for credential-protected devices, including unlocks after 15 minutes.
while :; do
 [ -f "$BASE/autostart" ] || [ -f "$BASE/health-enabled" ] || exit 0
 [ "$(getprop sys.boot_completed)" = 1 ] && [ "$("$BB" timeout 10 cmd activity get-started-user-state 0 2>/dev/null | tr -d '\r')" = RUNNING_UNLOCKED ] && break
 sleep 1
done
echo app_storage_ready
if [ -f "$BASE/autostart" ] && [ -z "$(pidof shizuku_server)" ]; then
 APK=$("$BB" timeout 10 pm path moe.shizuku.privileged.api 2>/dev/null | sed -n 's/^package://p' | head -n 1)
 EXPECTED=$(cat "$BASE/health-shizuku.sha256" 2>/dev/null)
 if [ -n "$APK" ] && [ -f "$APK" ] && [ -x "$BASE/shizuku_starter" ] && [ -n "$EXPECTED" ] && [ "$("$BB" sha256sum "$APK" | "$BB" awk '{print $1}')" = "$EXPECTED" ]; then
  "$BB" timeout 45 "$BASE/shizuku_starter" --apk="$APK" </dev/null >/dev/null 2>&1 || echo shizuku_start_deferred
 else
  echo shizuku_start_unavailable
 fi
fi
# Launch even if initial Shizuku preparation failed: the monitor can retry safely.
if [ -f "$BASE/health-enabled" ]; then
 "$BB" setsid /system/bin/sh "$BASE/health.sh" loop </dev/null >/dev/null 2>&1 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&- &
 echo health_monitor_requested
fi
