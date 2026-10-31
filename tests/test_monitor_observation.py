"""Run the actual polling loop with disposable PIDs and an isolated fake checker."""
from pathlib import Path
import argparse
import os
import shutil
import subprocess
import tempfile
import unittest

parser = argparse.ArgumentParser()
parser.add_argument("--shell", default=shutil.which("sh"))
args, remaining = parser.parse_known_args()
ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "com.fann.voicesip.dualsim"


@unittest.skipUnless(args.shell, "POSIX sh required")
class ObservationTest(unittest.TestCase):
    def run_scenario(self, scenario):
        with tempfile.TemporaryDirectory(prefix="voice-monitor-") as td:
            root = Path(td)
            commands = root / "bin"; commands.mkdir()
            (root / ".owned").touch(); (root / "health-enabled").touch()
            (root / "health-minutes").write_text("1440\n")
            source = (ROOT / "assets/runtime/health.sh").read_text(encoding="utf-8")
            begin = source.index("BASE=/data/adb/voice-sip-assistant")
            end = source.index('[ -n "$BB" ]', begin)
            source = source[:begin] + 'BASE="$CASE"\nBB="$CASE/bin/box"\n' + source[end:]
            entry = '/system/bin/sh "$BASE/health.sh" once "$RECOVERY_PACKAGE" "$RECOVERY_PID"'
            self.assertEqual(1, source.count(entry))
            (root / "health.sh").write_text(source.replace(entry, '"$CASE/bin/check" "$RECOVERY_PACKAGE" "$RECOVERY_PID"'), encoding="utf-8", newline="\n")
            fake = {
                "box": 'case "$1" in timeout) shift 2; exec "$@";; grep) shift; exec grep "$@";; *) exit 90;; esac',
                "dumpsys": 'exit 1',  # no mock foreground service
                "pidof": 'case "$1" in shizuku_server) exit 1;; esac; cat "$CASE/owner" 2>/dev/null || :',
                "sleep": 'echo "$1" >> "$CASE/sleeps"; :',
                "check": '''n=0; [ ! -f "$CASE/n" ] || read n < "$CASE/n"
n=$((n+1)); echo "$n" > "$CASE/n"
pauses=0; [ ! -f "$CASE/sleeps" ] || pauses=$(wc -l < "$CASE/sleeps")
printf '%s|%s|%d\\n' "$1" "$2" "$pauses" >> "$CASE/events"
case "$SCENARIO:$n" in
 empty:1|empty:2) exit 0;; empty:3) rm "$CASE/health-enabled"; exit 0;;
 *:1) kill -TERM "$OLD"; rm "$CASE/owner"; n=0; while kill -0 "$OLD" 2>/dev/null && [ "$n" -lt 50 ]; do /usr/bin/true; n=$((n+1)); done; exit 0;;
 busy:2|busy:3|busy:4|busy:5) exit 1;;
 replace:2) echo "$OTHER" > "$CASE/owner"; exit 1;;
 selection:2) echo io.github.sparkcyf.voicesipagent > "$CASE/health-agent"; exit 1;;
 disable:2) rm "$CASE/health-enabled"; exit 1;;
 *) rm "$CASE/health-enabled"; exit 0;; esac''',
            }
            for name, body in fake.items():
                script = commands / name; script.write_text("#!/bin/sh\n"+body+"\n", newline="\n"); script.chmod(0o755)
            env = dict(os.environ, CASE=root.as_posix(), SCENARIO=scenario, TEST_SH=Path(args.shell).as_posix())
            env['PATH'] = str(commands) + os.pathsep + str(Path(args.shell).parent) + os.pathsep + env.get('PATH','')
            wrapper = '''tail -f "$CASE/health-minutes" >/dev/null & OLD=$!
tail -f "$CASE/health-minutes" >/dev/null & OTHER=$!
export OLD OTHER
echo "$OLD" > "$CASE/started"
[ "$SCENARIO" = empty ] || echo "$OLD" > "$CASE/owner"
"$TEST_SH" "$CASE/health.sh" --loop; result=$?
kill "$OLD" "$OTHER" 2>/dev/null || :; wait 2>/dev/null || :; exit "$result"'''
            run = subprocess.run([args.shell, "-c", wrapper], env=env, capture_output=True, text=True, timeout=12)
            self.assertEqual(0, run.returncode, run.stderr)
            events = [(pkg, pid, int(pauses)) for pkg,pid,pauses in (line.split("|") for line in (root/"events").read_text().splitlines())]
            return (root/"started").read_text().strip(), events

    def test_death_during_initial_check_is_presented_immediately(self):
        old, events = self.run_scenario("first")
        self.assertEqual([("", "", 0), (PACKAGE, old, 0)], events)

    def test_contended_retries_keep_same_evidence_and_back_off(self):
        old, events = self.run_scenario("busy")
        self.assertEqual([("", "", 0)] + [(PACKAGE, old, n) for n in [0, 1, 2, 3, 6]], events)

    def test_replacement_and_selection_change_drop_pending_evidence(self):
        for scenario in ["replace", "selection"]:
            with self.subTest(scenario=scenario):
                old, events = self.run_scenario(scenario)
                self.assertEqual([("", "", 0), (PACKAGE, old, 0), ("", "", 3)], events)

    def test_manual_monitor_disable_stops_retries(self):
        old, events = self.run_scenario("disable")
        self.assertEqual([("", "", 0), (PACKAGE, old, 0)], events)

    def test_empty_first_probe_only_triggers_one_ordinary_early_recheck(self):
        _, events = self.run_scenario("empty")
        self.assertEqual([("", "", 0), ("", "", 1), ("", "", 11)], events)


if __name__ == '__main__':
    unittest.main(argv=[__file__]+remaining)
