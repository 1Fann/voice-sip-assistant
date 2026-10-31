#!/system/bin/sh
# voice-sip-assistant-owned: independent root monitor; no credentials or call audio logged.
BASE=/data/adb/voice-sip-assistant
BB=
for candidate in /data/adb/magisk/busybox /data/adb/ksu/bin/busybox /data/adb/ap/bin/busybox; do
  if [ -x "$candidate" ]; then BB=$candidate; break; fi
done
[ -n "$BB" ] || { echo busybox_missing; exit 20; }
umask 077
[ -f "$BASE/.owned" ] && [ -x "$BB" ] || exit 1
# Android mksh marks numbered descriptors close-on-exec. Use flock's command form.
case "${1:-loop}" in
 loop) exec "$BB" flock -n "$BASE/health-supervisor.lock" /system/bin/sh "$BASE/health.sh" --supervise;;
 # BusyBox timeout can leave a timer child alive after its command exits. It must
 # start before any lock is acquired, otherwise the timer inherits that lock.
 once) exec "$BB" timeout 180 "$BB" flock -n "$BASE/health-check.lock" "$BB" flock -n /data/adb/call-service-health.lock /system/bin/sh "$BASE/health.sh" --check "${2:-}" "${3:-}";;
 upgrade) exec "$BB" timeout 200 "$BB" flock -n "$BASE/health-upgrade.lock" /system/bin/sh "$BASE/health.sh" --handoff;;
 --supervise|--loop|--check|--handoff|--retire) ;;
 *) exit 2;;
esac
monitor_identity() {
 # Check both the exact owned script/mode arguments and process start ticks.
 # A stale PID file or a replacement reusing its PID cannot authorize TERM.
 local role=$1 mode=$2 pid fields
 read -r pid < "$BASE/$role.pid" 2>/dev/null || return 1
 case "$pid" in ''|*[!0-9]*|0|1) return 1;; esac
 [ -r "/proc/$pid/cmdline" ] && [ -r "/proc/$pid/stat" ] || return 1
 tr '\000' '\n' < "/proc/$pid/cmdline" | "$BB" awk -v script="$BASE/health.sh" -v mode="$mode" 'NR==1 {ok=($0=="/system/bin/sh")} NR==2 {ok=ok && $0==script} NR==3 {ok=ok && $0==mode} END {exit !(NR==3 && ok)}' || return 1
 read -r fields < "/proc/$pid/stat" || return 1
 fields=${fields##*) }; set -- $fields
 case "${20:-}" in ''|*[!0-9]*) return 1;; esac
 printf '%s:%s\n' "$pid" "${20}"
}
if [ "$1" = --retire ]; then
 # Only this short phase owns the per-app check lock. Do not wait for a
 # monitor to exit here and never signal its active check or descendants.
 [ -f "$BASE/health-enabled" ] || exit 0
 if [ -n "${2:-}" ] && [ "$(monitor_identity health-supervisor --supervise)" = "$2" ]; then kill -TERM "${2%:*}" 2>/dev/null || :; fi
 if [ -n "${3:-}" ] && [ "$(monitor_identity health-loop --loop)" = "$3" ]; then kill -TERM "${3%:*}" 2>/dev/null || :; fi
 exit 0
fi
if [ "$1" = --handoff ]; then
 [ -f "$BASE/health-enabled" ] || exit 0
 OLD_SUP=$(monitor_identity health-supervisor --supervise)
 OLD_LOOP=$(monitor_identity health-loop --loop)
 "$BB" flock "$BASE/health-check.lock" /system/bin/sh "$BASE/health.sh" --retire "$OLD_SUP" "$OLD_LOOP" || exit 1
 # The repair lock is already free. Another entry can start the replacement
 # first; do not wait for two slots to be empty at the same instant.
 attempts=0
 while [ -f "$BASE/health-enabled" ] && [ "$attempts" -lt 5 ]; do
  SUP=$(monitor_identity health-supervisor --supervise)
  [ -z "$SUP" ] || [ "$SUP" = "$OLD_SUP" ] || exit 0
  if "$BB" flock -n "$BASE/health-supervisor.lock" true; then
   [ -f "$BASE/health-enabled" ] || exit 0
   "$BB" setsid /system/bin/sh "$BASE/health.sh" loop </dev/null >/dev/null 2>&1 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&- &
   exit 0
  fi
  LOOP=$(monitor_identity health-loop --loop)
  [ -z "$LOOP" ] || [ "$LOOP" = "$OLD_LOOP" ] || exit 0
  attempts=$((attempts+1)); [ "$attempts" -ge 5 ] || sleep 1 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&-
 done
 [ ! -f "$BASE/health-enabled" ] && exit 0
 exit 75
fi
if [ "$1" = --supervise ]; then
 printf '%s\n' "$$" > "$BASE/health-supervisor.pid"
 trap 'rm -f "$BASE/health-supervisor.pid"' EXIT
 trap 'exit 0' TERM INT
 # Restart the first failed worker immediately; back off only repeated short failures.
 FAILURES=0
 while [ -f "$BASE/health-enabled" ]; do
  read STARTED _ < /proc/uptime; STARTED=${STARTED%%.*}
  # A descendant must not keep its parent's singleton lock alive after that parent dies.
  "$BB" flock -n "$BASE/health-loop.lock" /system/bin/sh "$BASE/health.sh" --loop 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&-
  [ -f "$BASE/health-enabled" ] || break
  read ENDED _ < /proc/uptime; ENDED=${ENDED%%.*}
  [ $((ENDED - STARTED)) -lt 30 ] || FAILURES=0
  case "$FAILURES" in 0) DELAY=0;; 1) DELAY=1;; 2) DELAY=2;; 3) DELAY=5;; *) DELAY=10;; esac
  [ "$FAILURES" -ge 4 ] || FAILURES=$((FAILURES + 1))
  while [ "$DELAY" -gt 0 ] && [ -f "$BASE/health-enabled" ]; do
   sleep 1 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&-; DELAY=$((DELAY - 1))
  done
 done
 exit 0
fi
if [ "$1" = --check ]; then
 [ -f "$BASE/health-enabled" ] || exit 0
 [ "$(getprop sys.boot_completed)" = 1 ] || exit 0
 [ "$("$BB" timeout 10 cmd activity get-started-user-state 0 2>/dev/null 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&- | tr -d '\r')" = RUNNING_UNLOCKED ] || exit 0
 HELPER=$("$BB" timeout 10 pm path com.fann.voicesiphelper 2>/dev/null 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&- | sed -n 's/^package://p' | head -n 1)
 SHIZUKU=$("$BB" timeout 10 pm path moe.shizuku.privileged.api 2>/dev/null 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&- | sed -n 's/^package://p' | head -n 1)
 [ -n "$HELPER" ] && [ -f "$HELPER" ] && [ -n "$SHIZUKU" ] && [ -f "$SHIZUKU" ] || exit 0
 # Validate protected hashes BEFORE loading APK code as root, including after reinstall/update.
 EXPECTED=$(cat "$BASE/health-helper.sha256" 2>/dev/null)
 [ -n "$EXPECTED" ] && [ "$("$BB" sha256sum "$HELPER" | "$BB" awk '{print $1}')" = "$EXPECTED" ] || exit 0
 EXPECTED=$(cat "$BASE/health-shizuku.sha256" 2>/dev/null)
 [ -n "$EXPECTED" ] && [ "$("$BB" sha256sum "$SHIZUKU" | "$BB" awk '{print $1}')" = "$EXPECTED" ] || exit 0
 # The check itself retains both locks through exec; only disposable query
 # children close them. The outer entry already supplies the total deadline.
 exec env CLASSPATH="$HELPER:$SHIZUKU" app_process /system/bin --nice-name=voice_sip_health_check com.fann.voicesiphelper.HealthMain "${2:-}" "${3:-}" </dev/null >/dev/null 2>&1
fi
printf '%s\n' "$$" > "$BASE/health-loop.pid"
trap 'rm -f "$BASE/health-loop.pid"' EXIT
trap 'exit 0' TERM INT
TRANSIENT_RETRIES=0
RECOVERY_PACKAGE=
RECOVERY_PID=
OWNER_PACKAGE=
OWNER_PID=
BUSY_RETRIES=0
EMPTY_RECHECKED=0
clear_recovery() { RECOVERY_PACKAGE=; RECOVERY_PID=; BUSY_RETRIES=0; }
observe_owner() {
 # Remember a positively observed live PID across the blocking full check. An
 # empty first lookup is never proof of death and cannot authorize fast start.
 PREVIOUS_PACKAGE=$OWNER_PACKAGE; PREVIOUS_PID=$OWNER_PID
 OWNER_PACKAGE=$(cat "$BASE/health-agent" 2>/dev/null)
 case "$OWNER_PACKAGE" in com.fann.voicesip.dualsim|io.github.sparkcyf.voicesipagent) ;; *) OWNER_PACKAGE=com.fann.voicesip.dualsim;; esac
 OWNER_PID=$(pidof "$OWNER_PACKAGE" 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&-); OWNER_PID=${OWNER_PID%% *}
 case "$OWNER_PID" in ''|*[!0-9]*|0|1) OWNER_PID=;; *) kill -0 "$OWNER_PID" 2>/dev/null || OWNER_PID=;; esac
 NEW_DEATH=0
 if [ -n "$RECOVERY_PID" ] && { [ "$RECOVERY_PACKAGE" != "$OWNER_PACKAGE" ] || [ -n "$OWNER_PID" ] || kill -0 "$RECOVERY_PID" 2>/dev/null; }; then clear_recovery; fi
 if [ -n "$PREVIOUS_PID" ] && [ "$PREVIOUS_PACKAGE" = "$OWNER_PACKAGE" ] && [ -z "$OWNER_PID" ] && ! kill -0 "$PREVIOUS_PID" 2>/dev/null; then
  RECOVERY_PACKAGE=$OWNER_PACKAGE; RECOVERY_PID=$PREVIOUS_PID; NEW_DEATH=1; BUSY_RETRIES=0
 fi
 [ -z "$OWNER_PID" ] || EMPTY_RECHECKED=0
}
while [ -f "$BASE/health-enabled" ]; do
 observe_owner
 /system/bin/sh "$BASE/health.sh" once "$RECOVERY_PACKAGE" "$RECOVERY_PID" 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&-
 CHECK_RESULT=$?
 # Nonblocking flock exits 1 without entering the checker. Keep verified death
 # evidence for a retry, and validate selection/replacement/PID reuse again.
 [ "$CHECK_RESULT" -eq 1 ] || clear_recovery
 observe_owner
 [ "$NEW_DEATH" -eq 0 ] || continue
 MINUTES=$(cat "$BASE/health-minutes" 2>/dev/null)
 case "$MINUTES" in ''|*[!0-9]*) MINUTES=5;; esac
 [ "$MINUTES" -ge 1 ] && [ "$MINUTES" -le 1440 ] || MINUTES=5
 LEFT=$((MINUTES * 60))
 # Contention is expected while the other assistant repairs. Never block forever
 # on its lock, and do not wait a user-selected 24h before trying again.
 if [ "$CHECK_RESULT" -eq 75 ]; then
  TRANSIENT_RETRIES=$((TRANSIENT_RETRIES + 1))
  if [ "$TRANSIENT_RETRIES" -le 6 ]; then LEFT=3; elif [ "$LEFT" -gt 60 ]; then LEFT=60; fi
 else
  TRANSIENT_RETRIES=0
  [ "$CHECK_RESULT" -eq 0 ] || LEFT=3
 fi
 if [ "$CHECK_RESULT" -eq 1 ] && [ -n "$RECOVERY_PID" ]; then
  if [ "$BUSY_RETRIES" -lt 3 ]; then LEFT=1; BUSY_RETRIES=$((BUSY_RETRIES+1)); fi
 elif [ "$CHECK_RESULT" -eq 0 ] && [ -z "$OWNER_PID" ] && [ "$EMPTY_RECHECKED" -eq 0 ]; then
  # One ordinary follow-up per continuous absence, not a permanent fast loop
  # while the user has stopped the gateway. Never invent an old PID.
  EMPTY_RECHECKED=1; LEFT=1
 fi
 SHIZUKU_PID=$(pidof shizuku_server 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&-)
 SHIZUKU_PID=${SHIZUKU_PID%% *}
 TICKS=0
 while [ "$LEFT" -gt 0 ] && [ -f "$BASE/health-enabled" ]; do
  [ -f "$BASE/health-now" ] && { rm -f "$BASE/health-now"; break; }
  sleep 1 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&-
  LEFT=$((LEFT - 1)); TICKS=$((TICKS + 1))
  # kill -0 is a shell builtin; no Binder/dumpsys work on the one-second path.
  case "$OWNER_PID" in ''|*[!0-9]*) ;; *) if ! kill -0 "$OWNER_PID" 2>/dev/null; then RECOVERY_PACKAGE=$OWNER_PACKAGE; RECOVERY_PID=$OWNER_PID; OWNER_PID=; BUSY_RETRIES=0; break; fi;; esac
  case "$SHIZUKU_PID" in ''|*[!0-9]*) ;; *) kill -0 "$SHIZUKU_PID" 2>/dev/null || break;; esac
  # Retain a bounded foreground-service check for a live process with a lost service.
  if [ $((TICKS % 10)) -eq 0 ] && ! "$BB" timeout 8 dumpsys activity services "$OWNER_PACKAGE" 2>/dev/null 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&- | "$BB" grep -q 'isForeground=true' 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&-; then
   break
  fi
 done
done
