#!/system/bin/sh
# Run only transformed monitor copies; no production app, lock or wake file is used.
set -eu
SOURCE=$1
BB=/data/adb/magisk/busybox
TEST_ROOT=/data/local/tmp/voice-speed-test-20261031
mkdir -p "$TEST_ROOT"
CASE=$("$BB" mktemp -d "$TEST_ROOT/case.XXXXXX")
case "$CASE" in /data/local/tmp/voice-speed-test-20261031/case.*) ;; *) exit 2;; esac
export TEST_CASE="$CASE"
cleanup() {
 rm -f "$CASE/health-enabled"
 for file in blocked-check.pid health-supervisor.pid health-loop.pid; do
  pid=$(cat "$CASE/$file" 2>/dev/null || :)
  case "$pid" in ''|*[!0-9]*) continue;; esac
  if [ -r "/proc/$pid/cmdline" ] && tr '\000' ' ' < "/proc/$pid/cmdline" | "$BB" grep -qF "$CASE/"; then kill -TERM "$pid" 2>/dev/null || :; fi
 done
 sleep 2
 rm -rf "$CASE"
}
trap cleanup EXIT
trap 'exit 1' TERM INT
mkdir -p "$CASE/bin" "$CASE/rootfs/run/asterisk"
cat > "$CASE/bin/check" <<'EOF'
#!/system/bin/sh
n=$(cat "$TEST_CASE/count" 2>/dev/null || echo 0)
printf '%s|%s\n' "${1:-}" "${2:-}" > "$TEST_CASE/check-event"
echo $((n + 1)) > "$TEST_CASE/count"
if [ -f "$TEST_CASE/block" ]; then
 echo $$ > "$TEST_CASE/blocked-check.pid"
 sleep 10
fi
exit "$(cat "$TEST_CASE/result")"
EOF
cat > "$CASE/bin/pidof" <<'EOF'
#!/system/bin/sh
echo "$1" >> "$TEST_CASE/pidof-packages"
cat "$TEST_CASE/owner.pid" 2>/dev/null || :
EOF
cat > "$CASE/bin/dumpsys" <<'EOF'
#!/system/bin/sh
echo checked >> "$TEST_CASE/dumpsys-count"
echo isForeground=true
EOF
chmod 700 "$CASE/bin/"*
PATH="$CASE/bin:$PATH"; export PATH
"$BB" sed -e "s|^BASE=.*|BASE=$CASE|" \
 -e "s|/data/adb/call-service-health.lock|$CASE/shared.lock|g" \
 -e "s|/sys/power/wake_unlock|$CASE/wake-unlock|g" \
 -e "s|/sys/power/wake_lock|$CASE/wake-lock|g" \
 -e '/^if \[ "$1" = --check \]; then$/a\ exec "$TEST_CASE/bin/check" "${2:-}" "${3:-}"' "$SOURCE" > "$CASE/health.sh"
touch "$CASE/.owned" "$CASE/.asterisk-app-owned" "$CASE/health-enabled"
echo 1440 > "$CASE/health-minutes"
echo 0 > "$CASE/result"
count() { cat "$CASE/count" 2>/dev/null || echo 0; }
wait_count() {
 limit=$2; n=0
 while [ "$(count)" -lt "$1" ] && [ "$n" -lt "$limit" ]; do sleep 0.1; n=$((n + 1)); done
 [ "$n" -lt "$limit" ] || { echo "FAIL: check count did not reach $1"; exit 1; }
}
wait_new_worker() {
 n=0
 while [ "$n" -lt 30 ]; do
  new=$(cat "$CASE/health-loop.pid" 2>/dev/null || :)
  if [ -n "$new" ] && [ "$new" != "$1" ] && kill -0 "$new" 2>/dev/null; then return; fi
  sleep 0.1; n=$((n + 1))
 done
 echo 'FAIL: first worker recovery exceeded 3 seconds'; exit 1
}
/system/bin/sh "$CASE/health.sh" loop &
wait_count 1 50
sleep 2
[ ! -f "$CASE/dumpsys-count" ]
echo 'PASS: one-second path does not call dumpsys'
old=$(cat "$CASE/health-loop.pid")
kill -KILL "$old"
wait_new_worker "$old"
echo 'PASS: first killed worker rebuilt within 3 seconds'
# A long-running check must keep the shared repair lock, but not the worker lock.
touch "$CASE/block" "$CASE/health-now"
n=0
while [ ! -f "$CASE/blocked-check.pid" ] && [ "$n" -lt 50 ]; do sleep 0.1; n=$((n + 1)); done
[ -f "$CASE/blocked-check.pid" ]
sleep 1
old=$(cat "$CASE/health-loop.pid")
kill -KILL "$old"
wait_new_worker "$old"
set +e
"$BB" flock -n "$CASE/shared.lock" true
locked=$?
set -e
[ "$locked" -eq 1 ]
echo 'PASS: orphan check retains repair exclusion without delaying worker recreation'
rm "$CASE/block"
sleep 10
# Killing a supervisor must not leave its singleton lock held by its worker.
old_supervisor=$(cat "$CASE/health-supervisor.pid")
kill -KILL "$old_supervisor"
/system/bin/sh "$CASE/health.sh" loop &
n=0
while [ "$(cat "$CASE/health-supervisor.pid")" = "$old_supervisor" ] && [ "$n" -lt 30 ]; do sleep 0.1; n=$((n + 1)); done
[ "$n" -lt 30 ]
echo 'PASS: worker does not inherit the supervisor lock'
# Cache a disposable service PID, then kill only that mock process.
/system/bin/sh -c 'sleep 20' &
mock_owner=$!
echo "$mock_owner" > "$CASE/owner.pid"
before=$(count); touch "$CASE/health-now"; wait_count $((before + 1)) 50
sleep 1
before=$(count); kill -TERM "$mock_owner"; wait "$mock_owner" 2>/dev/null || :
wait_count $((before + 1)) 30
if "$BB" grep -q 'RECOVERY_PACKAGE=' "$CASE/health.sh"; then
 [ "$(cat "$CASE/check-event")" = "com.fann.voicesip.dualsim|$mock_owner" ]
 echo 'PASS: only observed gateway PID death forwards its selected package and old PID'
fi
rm "$CASE/owner.pid"
echo 'PASS: cached service PID loss requests check within 3 seconds'
if "$BB" grep -q 'OWNER_PACKAGE=' "$CASE/health.sh"; then
 echo io.github.sparkcyf.voicesipagent > "$CASE/health-agent"
 before=$(count); touch "$CASE/health-now"; wait_count $((before + 1)) 50
 sleep 1
 "$BB" grep -qx io.github.sparkcyf.voicesipagent "$CASE/pidof-packages"
 rm "$CASE/health-agent"
 echo 'PASS: cached PID probe follows the selected official gateway package'
fi
# Also cover Asterisk engine loss while its foreground owner is still alive.
/system/bin/sh -c 'sleep 20' &
mock_engine=$!
echo "$mock_engine" > "$CASE/rootfs/run/asterisk/asterisk.pid"
if "$BB" grep -q 'ENGINE_PID=' "$CASE/health.sh"; then
 before=$(count); touch "$CASE/health-now"; wait_count $((before + 1)) 50
 sleep 1
 before=$(count); kill -TERM "$mock_engine"; wait "$mock_engine" 2>/dev/null || :
 wait_count $((before + 1)) 30
 echo 'PASS: cached PBX engine loss requests check within 3 seconds'
else
 kill -TERM "$mock_engine"; wait "$mock_engine" 2>/dev/null || :
fi
echo 75 > "$CASE/result"
before=$(count); touch "$CASE/health-now"; wait_count $((before + 1)) 50
wait_count $((before + 2)) 45
echo 'PASS: transient dependency check retried within 4.5 seconds'
# Runtime handover waits for an ongoing check instead of terminating its work.
echo 0 > "$CASE/result"
rm -f "$CASE/blocked-check.pid"
touch "$CASE/block" "$CASE/health-now"
n=0
while [ ! -f "$CASE/blocked-check.pid" ] && [ "$n" -lt 50 ]; do sleep 0.1; n=$((n + 1)); done
[ -f "$CASE/blocked-check.pid" ]
old_supervisor=$(cat "$CASE/health-supervisor.pid")
/system/bin/sh "$CASE/health.sh" upgrade &
upgrade=$!
sleep 1
kill -0 "$(cat "$CASE/blocked-check.pid")"
[ "$(cat "$CASE/health-supervisor.pid")" = "$old_supervisor" ]
rm "$CASE/block"
wait "$upgrade"
n=0
while [ "$(cat "$CASE/health-supervisor.pid" 2>/dev/null || :)" = "$old_supervisor" ] && [ "$n" -lt 50 ]; do sleep 0.1; n=$((n + 1)); done
[ "$n" -lt 50 ]
sleep 1
[ -s "$CASE/health-supervisor.pid" ] && [ -s "$CASE/health-loop.pid" ]
echo 'PASS: upgrade preserves an ongoing check then replaces both monitor owners'
rm "$CASE/health-enabled"
n=0
while { [ -f "$CASE/health-loop.pid" ] || [ -f "$CASE/health-supervisor.pid" ]; } && [ "$n" -lt 50 ]; do sleep 0.1; n=$((n + 1)); done
[ "$n" -lt 50 ]
echo 'PASS: manual monitor disable stops both owners promptly'
