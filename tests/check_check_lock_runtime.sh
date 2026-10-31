#!/system/bin/sh
# Isolated regression for Magisk BusyBox timeout children retaining flock files.
# SOURCE is transformed into a private temp directory; no real APK/service is used.
set -eu
SOURCE=$1
BB=/data/adb/magisk/busybox
[ -x "$BB" ] || exit 2
TEST_ROOT=/data/local/tmp/voice-check-lock-test-20261031
mkdir -p "$TEST_ROOT"
CASE=$("$BB" mktemp -d "$TEST_ROOT/case.XXXXXX")
case "$CASE" in /data/local/tmp/voice-check-lock-test-20261031/case.*) ;; *) exit 2;; esac
export TEST_CASE="$CASE"
cleanup() {
 rm -f "$CASE/health-enabled"
 touch "$CASE/release" "$CASE/release-slot"
 # Only our mock worker, with its exact private path, may be terminated.
 pid=$(cat "$CASE/mock.pid" 2>/dev/null || :)
 case "$pid" in ''|*[!0-9]*) ;; *)
  if [ -r "/proc/$pid/cmdline" ] && tr '\000' ' ' < "/proc/$pid/cmdline" | "$BB" grep -qF "$CASE/bin/app_process"; then kill -TERM "$pid" 2>/dev/null || :; fi;;
 esac
 rm -rf "$CASE"
}
trap cleanup EXIT
trap 'exit 1' TERM INT
mkdir "$CASE/bin"
printf '#!/system/bin/sh\necho 1\n' > "$CASE/bin/getprop"
printf '#!/system/bin/sh\necho RUNNING_UNLOCKED\n' > "$CASE/bin/cmd"
printf '#!/system/bin/sh\necho "package:$TEST_CASE/mock.apk"\n' > "$CASE/bin/pm"
cat > "$CASE/bin/app_process" <<'EOF'
#!/system/bin/sh
echo $$ > "$TEST_CASE/mock.pid"
touch "$TEST_CASE/entered"
while [ -f "$TEST_CASE/block" ] && [ ! -f "$TEST_CASE/release" ]; do
 sleep 0.1 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&-
done
EOF
chmod 700 "$CASE/bin/"*
PATH="$CASE/bin:$PATH"; export PATH
printf mock > "$CASE/mock.apk"
"$BB" sha256sum "$CASE/mock.apk" | "$BB" awk '{print $1}' > "$CASE/health-helper.sha256"
cp "$CASE/health-helper.sha256" "$CASE/health-shizuku.sha256"
"$BB" sed -e "s|^BASE=.*|BASE=$CASE|" \
 -e "s|/data/adb/call-service-health.lock|$CASE/shared.lock|g" \
 -e '/^if \[ "$1" = --retire \]; then$/a\ touch "$TEST_CASE/retire-entered"' "$SOURCE" > "$CASE/health.sh"
touch "$CASE/.owned" "$CASE/health-enabled"
free_lock() { "$BB" flock -n "$CASE/$1" true || { echo "FAIL: $1 remains held after completion"; exit 1; }; }
busy_lock() {
 if "$BB" flock -n "$CASE/$1" true; then echo "FAIL: $1 released during active check"; exit 1; fi
}
# Successful app_process exit must not wait for 10/180-second timer children.
/system/bin/sh "$CASE/health.sh" once
[ -f "$CASE/entered" ]
free_lock health-check.lock; free_lock shared.lock
echo 'PASS: complete check releases both locks immediately'
# An early APK-validation return exercises all three nested query deadlines too.
printf invalid > "$CASE/health-helper.sha256"
rm "$CASE/entered"
/system/bin/sh "$CASE/health.sh" once
[ ! -f "$CASE/entered" ]
free_lock health-check.lock; free_lock shared.lock
cp "$CASE/health-shizuku.sha256" "$CASE/health-helper.sha256"
echo 'PASS: early validation failure releases query and check locks immediately'
# The actual check must continue holding both locks; upgrade waits behind it.
touch "$CASE/block"
/system/bin/sh "$CASE/health.sh" once & check=$!
n=0
while [ ! -f "$CASE/entered" ] && [ "$n" -lt 50 ]; do sleep 0.1; n=$((n+1)); done
[ -f "$CASE/entered" ]
busy_lock health-check.lock; busy_lock shared.lock
if /system/bin/sh "$CASE/health.sh" once; then echo 'FAIL: overlapping check accepted'; exit 1; fi
/system/bin/sh "$CASE/health.sh" upgrade & upgrade=$!
sleep 0.2
busy_lock health-upgrade.lock
busy_lock health-check.lock; busy_lock shared.lock
rm "$CASE/health-enabled"
touch "$CASE/release"
wait "$check"; wait "$upgrade"
free_lock health-check.lock; free_lock shared.lock; free_lock health-upgrade.lock
echo 'PASS: live check holds locks, upgrade waits, all release without timer delay'
/system/bin/sh "$CASE/health.sh" upgrade
free_lock health-upgrade.lock; free_lock health-check.lock
echo 'PASS: disabled upgrade releases its locks immediately'

# Reproduce a competing owner during handoff. Keep the supervisor slot occupied
# with a private mock holder: the old algorithm would pin the check for 200s.
printf '#!/system/bin/sh\nexit 1\n' > "$CASE/bin/pidof"
printf '#!/system/bin/sh\necho isForeground=true\n' > "$CASE/bin/dumpsys"
chmod 700 "$CASE/bin/pidof" "$CASE/bin/dumpsys"
cat > "$CASE/slot.sh" <<'EOF'
#!/system/bin/sh
touch "$TEST_CASE/slot-held"
while [ ! -f "$TEST_CASE/release-slot" ]; do /data/adb/magisk/busybox sleep 0.05 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&-; done
EOF
"$BB" timeout 20 "$BB" flock "$CASE/health-supervisor.lock" /system/bin/sh "$CASE/slot.sh" & slot=$!
n=0; while [ ! -f "$CASE/slot-held" ]; do n=$((n+1)); [ "$n" -lt 40 ] || exit 1; "$BB" sleep 0.05; done
touch "$CASE/health-enabled"
rm -f "$CASE/retire-entered" "$CASE/entered"
/system/bin/sh "$CASE/health.sh" upgrade & handoff=$!
n=0; while [ ! -f "$CASE/retire-entered" ]; do n=$((n+1)); [ "$n" -lt 40 ] || exit 1; "$BB" sleep 0.05; done
n=0; until "$BB" flock -n "$CASE/health-check.lock" true; do n=$((n+1)); [ "$n" -lt 40 ] || exit 1; "$BB" sleep 0.05; done
/system/bin/sh "$CASE/health.sh" once
[ -f "$CASE/entered" ]; kill -0 "$handoff"
echo 'PASS: fresh business check completes while handoff waits for an occupied owner slot'
"$BB" flock -n "$CASE/health-loop.lock" /system/bin/sh "$CASE/health.sh" --loop & replacement=$!
n=0; while [ ! -s "$CASE/health-loop.pid" ]; do n=$((n+1)); [ "$n" -lt 40 ] || exit 1; "$BB" sleep 0.05; done
wait "$handoff"
kill -0 "$(cat "$CASE/health-loop.pid")"; busy_lock health-supervisor.lock; free_lock health-upgrade.lock
rm "$CASE/health-enabled"; wait "$replacement"
echo 'PASS: existing replacement loop survives and finishes the handoff with the supervisor slot still busy'

# If neither slot owner can be confirmed, bounded deferral must also release
# the upgrade lock. Do not change a deliberately enabled/disabled preference.
touch "$CASE/health-enabled"
set +e
/system/bin/sh "$CASE/health.sh" upgrade; result=$?
set -e
[ "$result" -eq 75 ]; [ -f "$CASE/health-enabled" ]; free_lock health-check.lock; free_lock health-upgrade.lock
rm "$CASE/health-enabled"; touch "$CASE/release-slot"; wait "$slot"
echo 'PASS: bounded deferral releases locks and retains requested state'
