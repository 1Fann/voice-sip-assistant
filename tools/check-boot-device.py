"""Exercise boot waiting with isolated fake services; never touch real call settings."""
import argparse
import pathlib
import subprocess
import tempfile
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--adb', default='adb')
parser.add_argument('--serial', required=True)
args = parser.parse_args()
ADB, SERIAL = args.adb, args.serial
REMOTE = '/data/local/tmp/vsa-boot-fixture'
PROJECT = pathlib.Path(__file__).resolve().parents[1]

def adb(*args):
    return subprocess.check_output([ADB, '-s', SERIAL, *args], text=True, encoding='utf-8').strip()

def root(command):
    return adb('shell', 'su', '-M', '-c', "'" + command.replace("'", "'\\''") + "'")

with tempfile.TemporaryDirectory(prefix='boot-fixture-') as local:
    folder = pathlib.Path(local)
    (folder / 'bin').mkdir()
    scripts = {
        'getprop': '#!/system/bin/sh\necho 1\n',
        'cmd': '#!/system/bin/sh\nn=$(cat "$FIXTURE/ticks" 2>/dev/null || echo 0)\nif [ "$n" -ge 181 ]; then printf "RUNNING_UNLOCKED\\r\\n"; else echo RUNNING_LOCKED; fi\n',
        'sleep': '#!/system/bin/sh\nn=$(cat "$FIXTURE/ticks" 2>/dev/null || echo 0); n=$((n+1)); echo "$n" > "$FIXTURE/ticks"\nif [ -f "$FIXTURE/cancel" ]; then rm -f "$FIXTURE/autostart" "$FIXTURE/health-enabled"; fi\n',
        'pidof': '#!/system/bin/sh\nexit 1\n',
        'pm': '#!/system/bin/sh\necho "package:$FIXTURE/fake.apk"\n',
    }
    for name, content in scripts.items():
        (folder / 'bin' / name).write_text(content, encoding='utf-8', newline='\n')
    content = (PROJECT / 'assets/runtime/boot.sh').read_text(encoding='utf-8')
    content = content.replace('BASE=/data/adb/voice-sip-assistant', f'BASE={REMOTE}\nexport FIXTURE="$BASE"\nexport PATH="$BASE/bin:/system/bin"')
    (folder / 'boot.sh').write_text(content, encoding='utf-8', newline='\n')
    (folder / 'health.sh').write_text('#!/system/bin/sh\necho requested > "$FIXTURE/monitor-requested"\n', encoding='utf-8', newline='\n')
    (folder / 'shizuku_starter').write_text('#!/system/bin/sh\necho attempted > "$FIXTURE/starter-attempted"\nexit 1\n', encoding='utf-8', newline='\n')
    (folder / 'fake.apk').write_text('test fixture only', encoding='utf-8')
    adb('push', local + '/.', REMOTE)
    root(f'chmod 700 {REMOTE}/bin/* {REMOTE}/shizuku_starter; touch {REMOTE}/.owned; /data/adb/ksu/bin/busybox sha256sum {REMOTE}/fake.apk | /data/adb/ksu/bin/busybox awk \'{{print $1}}\' > {REMOTE}/health-shizuku.sha256')
    root(f'touch {REMOTE}/autostart {REMOTE}/health-enabled; /system/bin/sh -n {REMOTE}/boot.sh')
    result = root(f'/system/bin/sh {REMOTE}/boot.sh --worker')
    time.sleep(0.3)
    checks = root(f'cat {REMOTE}/ticks; cat {REMOTE}/monitor-requested; cat {REMOTE}/starter-attempted')
    assert checks.splitlines() == ['181', 'requested', 'attempted'], checks
    assert 'shizuku_start_deferred' in result and 'health_monitor_requested' in result, result
    print('PASS: delayed storage readiness beyond previous 180 waits, CRLF state, failed Shizuku still launches monitoring')
    root(f'rm -f {REMOTE}/ticks {REMOTE}/monitor-requested {REMOTE}/starter-attempted; touch {REMOTE}/cancel; /system/bin/sh {REMOTE}/boot.sh --worker')
    checks = root(f'test ! -f {REMOTE}/monitor-requested && test ! -f {REMOTE}/starter-attempted && echo cancelled')
    assert checks == 'cancelled', checks
    print('PASS: turning off both settings cancels pending boot work')
