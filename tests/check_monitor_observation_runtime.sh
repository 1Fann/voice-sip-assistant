#!/system/bin/sh
# Isolated real-BusyBox test: no installed apps, production locks or telephony calls.
set -eu
SOURCE=$1
BB=/data/adb/magisk/busybox
ROOT=/data/local/tmp/voice-observation-test-20261031
mkdir -p "$ROOT"
CASE=$("$BB" mktemp -d "$ROOT/case.XXXXXX")
case "$CASE" in /data/local/tmp/voice-observation-test-20261031/case.*) ;; *) exit 2;; esac
export CASE
cleanup() {
 rm -f "$CASE/health-enabled" "$CASE/keep-owner" "$CASE/hold-first" "$CASE/hold-lock"
 sleep 1
 rm -rf "$CASE"
}
trap cleanup EXIT
trap 'exit 1' INT TERM
mkdir "$CASE/bin"
cat > "$CASE/bin/pidof" <<'EOF'
#!/system/bin/sh
case "$1" in com.fann.voicesip.dualsim) cat "$CASE/owner.pid" 2>/dev/null || :;; *) exit 1;; esac
EOF
cat > "$CASE/bin/check" <<'EOF'
#!/system/bin/sh
echo "${1:-}|${2:-}" >> "$CASE/entered"
if [ -n "${2:-}" ]; then rm -f "$CASE/health-enabled"; fi
while [ -f "$CASE/hold-first" ]; do /data/adb/magisk/busybox sleep 0.05 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&-; done
EOF
cat > "$CASE/hold.sh" <<'EOF'
#!/system/bin/sh
touch "$CASE/holding"
while [ -f "$CASE/hold-lock" ]; do /data/adb/magisk/busybox sleep 0.05 3>&- 4>&- 5>&- 6>&- 7>&- 8>&- 9>&-; done
EOF
cat > "$CASE/owner.sh" <<'EOF'
#!/system/bin/sh
while [ -f "$CASE/keep-owner" ]; do /data/adb/magisk/busybox sleep 0.1; done
EOF
printf '#!/system/bin/sh\necho isForeground=true\n' > "$CASE/bin/dumpsys"
chmod 700 "$CASE/bin/"* "$CASE/hold.sh" "$CASE/owner.sh"
PATH="$CASE/bin:$PATH"; export PATH
"$BB" sed -e "s|^BASE=.*|BASE=$CASE|" -e "s|/data/adb/call-service-health.lock|$CASE/shared.lock|g" \
 -e '/^if \[ "$1" = --check \]; then$/a\ exec "$CASE/bin/check" "${2:-}" "${3:-}"' \
 -e '/^ CHECK_RESULT=\$?$/a\ printf "%s|%s|%s\\n" "$CHECK_RESULT" "$RECOVERY_PACKAGE" "$RECOVERY_PID" >> "$CASE/results"' \
 "$SOURCE" > "$CASE/health.sh"
touch "$CASE/.owned"
echo 1440 > "$CASE/health-minutes"
wait_match() {
 n=0
 while ! "$BB" grep -Fxq "$2" "$CASE/$1" 2>/dev/null; do
  n=$((n+1)); [ "$n" -lt 50 ] || { echo "FAIL: $1 $2"; exit 1; }; "$BB" sleep 0.1
 done
}
start_loop() {
 rm -f "$CASE/entered" "$CASE/results" "$CASE/holding"
 touch "$CASE/keep-owner" "$CASE/health-enabled"
 "$BB" ash "$CASE/owner.sh" & owner=$!; echo "$owner" > "$CASE/owner.pid"
 /system/bin/sh "$CASE/health.sh" --loop & loop=$!
 wait_match entered '|'
}
end_owner() { rm -f "$CASE/keep-owner"; wait "$owner"; }

touch "$CASE/hold-first"
start_loop
end_owner
rm "$CASE/hold-first"
wait_match entered "com.fann.voicesip.dualsim|$owner"
wait "$loop"
echo 'PASS: gateway death during first blocking check retains exact observed PID'

start_loop
touch "$CASE/hold-lock"
"$BB" timeout 15 "$BB" flock "$CASE/shared.lock" "$BB" ash "$CASE/hold.sh" & holder=$!
n=0; while [ ! -f "$CASE/holding" ]; do n=$((n+1)); [ "$n" -lt 50 ] || exit 1; "$BB" sleep 0.1; done
end_owner
wait_match results "1|com.fann.voicesip.dualsim|$owner"
[ "$(wc -l < "$CASE/entered" | tr -d ' ')" = 1 ]
rm "$CASE/hold-lock"; wait "$holder"
wait_match entered "com.fann.voicesip.dualsim|$owner"
wait "$loop"
echo 'PASS: real shared-lock contention rejects overlap; next check receives same death evidence'
