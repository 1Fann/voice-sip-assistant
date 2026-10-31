#!/system/bin/sh
# Isolated regression test. Pass a health.sh source; no real app/service is touched.
set -eu
SOURCE=$1
BB=/data/adb/magisk/busybox
[ -x "$BB" ] || exit 2
TEST_ROOT=/data/local/tmp/voice-reliability-test-20261031
mkdir -p "$TEST_ROOT"
CASE=$("$BB" mktemp -d "$TEST_ROOT/case.XXXXXX")
case "$CASE" in /data/local/tmp/voice-reliability-test-20261031/case.*) ;; *) exit 2;; esac
cleanup() {
 rm -f "$CASE/health-enabled"
 sleep 6
 for file in health-loop.pid health-supervisor.pid; do
  pid=$(cat "$CASE/$file" 2>/dev/null || :)
  case "$pid" in ''|*[!0-9]*) continue;; esac
  if [ -r "/proc/$pid/cmdline" ] && tr '\000' ' ' < "/proc/$pid/cmdline" | "$BB" grep -qF "$CASE/health.sh"; then kill -TERM "$pid" 2>/dev/null || :; fi
 done
 rm -rf "$CASE"
}
trap cleanup EXIT
trap 'exit 1' TERM INT
mkdir "$CASE/bin"
printf '#!/system/bin/sh\necho 0\n' > "$CASE/bin/getprop"
printf '#!/system/bin/sh\necho isForeground=true\n' > "$CASE/bin/dumpsys"
chmod 700 "$CASE/bin/"*
PATH="$CASE/bin:$PATH"
export PATH
"$BB" sed -e "s|^BASE=.*|BASE=$CASE|" \
 -e "s|/data/adb/call-service-health.lock|$CASE/shared.lock|g" \
 -e "s|/sys/power/wake_unlock|$CASE/wake-unlock|g" \
 -e "s|/sys/power/wake_lock|$CASE/wake-lock|g" "$SOURCE" > "$CASE/health.sh"
touch "$CASE/.owned" "$CASE/.asterisk-app-owned" "$CASE/health-enabled"
echo 1440 > "$CASE/health-minutes"
# A contended common lock must return promptly, never hang until the other owner exits.
"$BB" flock "$CASE/shared.lock" /system/bin/sh -c "touch '$CASE/locked'; sleep 5" &
locker=$!
n=0
while [ ! -f "$CASE/locked" ] && [ "$n" -lt 20 ]; do sleep 0.1; n=$((n+1)); done
[ -f "$CASE/locked" ]
set +e
"$BB" timeout 2 /system/bin/sh "$CASE/health.sh" once
result=$?
set -e
[ "$result" = 1 ] || { echo "FAIL: contended check result $result"; exit 1; }
wait "$locker"
echo 'PASS: shared-lock contention returns without blocking'
/system/bin/sh "$CASE/health.sh" loop &
n=0
while [ ! -f "$CASE/health-loop.pid" ] && [ "$n" -lt 20 ]; do sleep 0.1; n=$((n+1)); done
old=$(cat "$CASE/health-loop.pid")
supervisor=$(cat "$CASE/health-supervisor.pid")
set +e
/system/bin/sh "$CASE/health.sh" loop
result=$?
set -e
[ "$result" = 1 ]
[ "$(cat "$CASE/health-supervisor.pid")" = "$supervisor" ]
echo 'PASS: duplicate start retains one supervisor'
kill -KILL "$old"
n=0
while [ "$n" -lt 15 ]; do
 sleep 1
new=$(cat "$CASE/health-loop.pid" 2>/dev/null || :)
 if [ -n "$new" ] && [ "$new" != "$old" ] && kill -0 "$new" 2>/dev/null; then break; fi
 n=$((n+1))
done
[ "$n" -lt 15 ] || { echo 'FAIL: worker did not recover'; exit 1; }
echo 'PASS: killed monitor worker automatically restarts'
rm "$CASE/health-enabled"
n=0
while [ "$n" -lt 15 ]; do
 [ ! -f "$CASE/health-loop.pid" ] && [ ! -f "$CASE/health-supervisor.pid" ] && break
 sleep 1; n=$((n+1))
done
[ "$n" -lt 15 ] || { echo 'FAIL: disabled monitor still running'; exit 1; }
echo 'PASS: disabling monitoring stops worker and supervisor'
