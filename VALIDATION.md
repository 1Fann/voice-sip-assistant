# Validation / 验证范围

## 2026-10-31 — 1.4.6 direct recovery observations

Production APK built and original signing certificate verified. 115 host checks passed: 26 health-policy, 33 guarded fast-start, 51 MTK request/recovery and 5 readiness-wait scenarios. The corrected daemon-token cold-boot cases are included. The monitor's one-second cadence is unchanged. Current PID absence uses signal 0 with ESRCH and `/proc` verification; current IPv4 enumeration replaces repeated `ip` command startup.

An additional pre-publication correction places the 180/200-second check/upgrade deadlines outside all acquired locks. All three nested ten-second system-query timers close inherited descriptors 3–9, while the actual app_process checker retains its locks until exit. This addresses an observed Magisk BusyBox timer child retaining a lock after the command had already completed. Four shell syntax checks and structural checks passed locally.

The device-test owner then ran `tests/check_check_lock_runtime.sh` against the phone's actual Magisk BusyBox in an isolated temporary directory with fake APK/worker fixtures. All four groups passed: immediate lock release after a successful check; immediate release after early APK-validation failure; both locks held during live work with upgrade correctly waiting and releasing on completion; and immediate release after a disabled upgrade. The test did not operate production services, credentials or call audio. These results establish the tested lock-lifetime behavior, not an end-to-end service-recovery deadline.

The following revision also samples the selected gateway PID before and after each full check. A positively observed process that exits during the check can enter the existing guarded recovery immediately. A busy check retains this evidence for another attempt, while a replacement PID, PID reuse or package change invalidates it. A first observation that finds no PID gets one early **ordinary** recheck per continuous absence; it never authorizes fast start. Manual stop, call-state, identity and foreground checks are unchanged.

Five host fixture groups in `tests/test_monitor_observation.py` passed using the real polling loop, disposable processes and a mock checker. Cases include death during the first check, repeated contention and bounded retry spacing, replacement/package selection changes, disabling the monitor and a continuously missing PID. The 115 Java host checks passed again. `tests/check_monitor_observation_runtime.sh` provides separate Android tests for first-check exit and actual shared-lock contention. The four device lock-lifetime results above were collected before this sampling change; they are not recovery measurements for the new APK.

Upgrade handoff is now split into two phases. The per-app check lock covers only waiting for any active check and signaling the captured old monitor owners; identity must still match the owned script, mode, PID and process start time. The coordinator releases that lock before inspecting successor slots. It accepts a valid replacement owner, can launch a missing supervisor without waiting for the worker slot, and defers after about four seconds of unrecognized contention. It never signals the business checker or changes the requested monitor setting. The shared-call lock continues to serialize actual repairs.

The five host observation fixture groups passed again. On the Redmi Note 10 Pro / MIUI 14 device, the installed `c12eb566…` APK and deployed root monitor script matched this build. The device-test owner reran `tests/check_check_lock_runtime.sh` on real Magisk BusyBox: all seven lock/handoff groups passed. The three added cases showed a fresh check completes while the old owner slot is occupied, a replacement loop survives handoff, and unrecognized contention returns after bounded retries with the check and upgrade locks free. `tests/check_monitor_observation_runtime.sh` also passed both groups: a disposable gateway PID exited during the first blocking check, and real shared-lock contention preserved that same observed PID until a later check could enter. These isolated tests used mock business services; they establish the tested handoff and evidence retention behavior, not actual call delivery or an absolute recovery deadline.

On that `c12eb566…` build, two real-device checker-replacement samples were 0.41 and 0.39 seconds; gateway local-readiness samples were 3.09 and 2.97 seconds. They describe that specific build and two trials, not a guaranteed deadline or a successful live call.

The final candidate changes only how UID 0 `Root.run` waits for short shell commands: a daemon waiter notifies the checker of process exit, with the original command timeout and kill-on-timeout behavior. The ordinary application/superuser path, output handling, exit-code interpretation, root monitor script, authorization and call/repair gates remain unchanged. Five further host checks passed for immediate exit, later completion, timeout cleanup, interruption and a failed waiter. The production-signed APK is `out/Voice-SIP-Assistant-1.4.6.apk`, versionCode 15, SHA-256 `c7975e06d0dbb05c8b761bb1766d4c956332e62dc0ef4df947e5ee498ed93adf`. Device measurements for this new digest must be recorded separately.

## 1.3.3: phone gateway UI / 手机通话网关界面

The helper now labels its takeover control as **Phone gateway / 手机通话网关** and explains both cellular-to-SIP incoming and SIP-to-cellular outgoing calls. Enabling retains companion access, direct audio, and the user's local-mute setting. System-dialer restoration remains available under advanced repair. Manual gateway OFF, call guards, signature validation, credentials, routes and recovery intervals are unchanged. Fresh deployment no longer explicitly disables local output mute; it preserves the gateway setting. Outdated stock-dialer integration instructions were removed from the current UI and documentation.

界面改为“手机通话网关”，明确启用及停用对来电和 SIM 呼出的影响。保留系统电话界面的访问方式和本机静音设置；“恢复系统默认电话”移至高级修复。未改变账号、双卡路由、定时检查或手动停用保护。

## Verified deployment / 已验证部署

2026-10-29, before the 1.3.3 wording cleanup: one ARM64 MediaTek Redmi on Android 13 / Magisk restored successfully. Both SIM outbound calls, both-SIM Groundwire lock-screen push, caller-number display/grouping and two-way audio were confirmed by the user. Following an actual reboot without operating the Redmi screen, services recovered automatically and the user reconfirmed echo, incoming push, number grouping and two-way audio. The tested setup uses an additional, server-scoped Wi-Fi high-performance compatibility worker for screen-off IPv6 reception. These device-specific restore components are not automatically installed by this helper APK.

此前运行逻辑的检查覆盖间隔 1–1440 分钟、通话/未知状态跳过、手动停止保护和两个助手的职责分离。当前设备保留每 1 分钟检查设置。界面清理后仍需核对安装签名、配置保持、网关状态及检查任务。

No real cellular calls are placed automatically. Device audio and push success are established by the user's manual tests, not by unit tests. Results do not establish compatibility with every ROM or Android phone. Private phone accounts, passwords, configuration and signing keys are excluded from this source tree.

## 2026-10-29 portability release / 新手机部署修复

The APKs package the migration fixes. The original signing identities were compared against APKs pulled from the connected phone before each upgrade; all four updated in place. SIP, DDNS and gateway account configuration hashes were unchanged. The temporary KSU BusyBox alias was removed; Magisk-backed IPv6 selection, both independent monitors and Asterisk authenticated echo self-test (61 returned packets) still passed. The temporary Wi-Fi boot hook was removed by the APK-owned runtime.

电话权限向导在实机完成授权后，网关自行恢复 Shizuku CONNECTED、audioReady、SIP ready。绑定超时重试日志已在实机观察到；未自动批准系统权限。号码库及空姓名修复已由 APK 部署，AGI 四项回归测试和部署八项检查通过。完整网关 JVM 测试通过，包括权限缺失、返回空数据、绑定超时、晚到回调及停止时取消重试。

An APK install is not a silent private-account restore or a cross-ROM audio guarantee. Fresh installation still requires user-approved Root/Shizuku/system permissions and local account setup. No private credentials, signing keys or permission databases are included in this release. The original cellular audio implementation is unchanged.

## Final manual call validation / 最终实际通话验证

2026-10-29: after disabling the iPhone VPN, the user confirmed SIP echo, two-way cellular audio, Groundwire lock-screen incoming push and caller-number grouping. The failed media attempt advertised the VPN address `198.18.0.1` and delivered no client RTP. Packet diagnostics recorded only addresses and counters, not audio. No real cellular calls were initiated automatically.

用户确认关闭 iPhone VPN 后，回声、普通号码双向声音、锁屏推送和号码分段均正常。网关原生音频实现未改动；新手机流程通过隔离配置与现有手机原签名升级验证，没有清空用户手机做出厂重置测试。

## 2026-10-29 background recovery and Chinese UI / 后台恢复与汉化

- Production-signed APKs updated the existing Android 13 / Magisk installation; SIP, dialplan, DDNS and gateway configuration hashes were unchanged.
- Host checks: 26 health-policy cases per assistant; the gateway Gradle unit-test suite passed, including Chinese status/error distinctions and call-count privacy checks. Native audio libraries retain their previously pinned hashes.
- Explicit background setup was exercised in the actual APKs. The ROM rejects the symbolic AUTO_START shell operation; its framework exposes OP_AUTO_START=10008. The fix only uses that vendor operation when the actual MIUI operation appears in the ROM output. Error messages identify the setting instead of blaming Root.
- Screen-off fault injection force-stopped both assistants, the gateway and DDNS UI. Asterisk foreground ownership returned at the 11.1-second sample; complete gateway/Shizuku/audio/SIP readiness returned at 32.6 seconds. No activity was manually launched during recovery.
- A second screen-off run also force-stopped the Shizuku manager UI. All readiness checks passed at 33.3 seconds. Independent IPv6 SIP OPTIONS responses remained available throughout both sampled runs. Timings are measurements on this device, not a service guarantee.
- 用户随后确认：红米息屏且未手动打开软件，iPhone 蜂窝网络 / VPN 关闭时，600 回声、真实呼出双向声音、锁屏来电推送、号码分段和接听双向声音正常。
- No automatic real calls or audio recordings were made. Root and system consent are still required on a new phone; no router changes, account secrets or signing keys are included.

## 2026-10-31 — 1.4.2 reliability build

- Existing production signing certificate verified; local APK build and `apksigner verify` passed. No repository push or public release performed.
- 26 host monitor-policy checks passed, including active/unknown call states and interval validation.
- `tests/check_monitor_runtime.sh` ran on the connected Android device against a transformed copy of the new monitor in `/data/local/tmp/voice-reliability-test-20261031`. Android status queries were mocked; real services, repair locks and wake locks were not used.
- All four isolated behaviors passed: nonblocking shared-lock contention; duplicate-start exclusion; automatic recreation after killing the worker; worker/supervisor exit after disabling monitoring.
- Root runtime and boot-hook publication now use atomic rename. Existing foreground/service repair logic and call-safety decisions are unchanged.
- These are isolated regression checks, not an end-to-end call, reboot or overnight screen-off acceptance. A device/supervisor failure and external connectivity can still interrupt service. Credential-protected storage still requires first unlock.

## 2026-10-31 Redmi / MIUI 14 device verification

Installed same-signature updates on one Redmi Note 10 Pro running Android 13, MIUI 14 and Magisk. Existing application data and monitor settings were retained. The device currently has no secure lock-screen credential; no lock-screen setting was changed.

真实息屏测试中，分别结束五项套件的检测工作进程，均自动恢复：Voice SIP 15.3 秒、Asterisk 6.8 秒、Bark 11.2 秒、短信互联 11.1 秒、DDNS 28.6 秒。这里只结束检测工作进程，没有用结束真实通话或发送短信来测试。

整机重启一次，全程没有手动解锁或打开应用；约 67 秒后，所需前台服务和重启后的新检测记录齐备。随后确认网关 SIP 注册、Shizuku 连接、音频就绪、本机短信 HTTPS、Bark ping 及 DDNS 本机健康正常。DDNS 配置和原偏好摘要保持一致。

随后强制进入深度 Doze 75 秒，Voice SIP、Asterisk、短信互联和 Bark 的检测记录均在此期间更新，电话检查状态正常。已退出强制 Doze，恢复正常电源策略。

These are service-readiness tests, not new end-to-end cellular calls, actual SMS delivery, APNs delivery, or an overnight battery-powered suspend test. No automatic real calls or messages were sent. Unattended boot on this device does not establish Direct Boot support before first credential unlock on a password-protected device. Existing wake-lock preferences were preserved; no global battery policy was disabled.

## 2026-10-31 — 1.4.3 speed regression checks

- Same production signing identity retained; local build, APK signature verification and 26 host monitor-policy checks passed.
- The shared `tests/check_fast_monitor_runtime.sh` harness in the Voice assistant repository ran transformed copies under /data/local/tmp/voice-speed-test-20261031, with mocked status/check commands and isolated locks, PID files and wake files. 9 checks passed for this monitor.
- Verified: no per-second dumpsys; first killed worker recreated within the 3-second test threshold; a surviving check retains repair exclusion without retaining the loop lock; supervisor lock does not leak to its child; cached process loss triggers a check within 3 seconds; explicit temporary dependency failure retries within 4.5 seconds; upgrade waits for a live check and then replaces both monitor owners; disabling monitoring stops both owners. Asterisk also covers engine-PID loss with a live service owner.
- These are isolated scheduling/lock checks. Actual gateway/SIP readiness, reboot and screen-off service fault injection are measured separately; no production service or real call was touched by this test harness.
Additional unpublished 1.4.3 hot-recovery refinement:

- A live Shizuku server is reused only after verifying the installed official signature; repeated starter extraction and runtime staging are skipped on this path. Cold-server startup and its stability confirmation are unchanged.
- The two 1.5-second startup grace periods now poll readiness within the same window and return early only when all readiness fields pass. Existing bounded status-query timeouts remain unchanged.
- After foreground startup or binding recovery, complete gateway readiness, foreground ownership and the PBX contact are checked before deciding to restart SIP. An already recovered registration is retained. Call/manual-stop guards and the reconnect fallback remain.
- Five fake-clock polling scenarios passed (immediate success, delayed readiness, partial readiness rejected, expired budget and failed query), alongside 26 existing monitor-policy checks. Runtime shell assets were unchanged from the nine passing isolated scheduling tests. A new production-signed local APK was built; actual recovery gains require a separate device measurement.

## 2026-10-31 recovery-speed follow-up

The second pass removes fixed first-failure waits, adds inexpensive cached-process checks, and keeps backoff for repeated failures. Full network checks retain their configured cadence. These measurements replace neither the original baseline above nor its stated limitations.

| Component | Checker replacement (s) | Actual local service ready after process kill (s) |
|---|---:|---:|
| voice | 0.3 | 13.16 |
| asterisk | 0.2 | 11.77 |
| sms | 1.9 | 2.69 |
| bark | 1.4 | 1.48 |
| ddns | 2.1 | 1.41 |

Phone readiness required SIP registration plus the gateway’s connected Shizuku and ready audio flags; PBX readiness required the engine and gateway registration. SMS, Bark and DDNS required successful local HTTP(S) responses, with the configured Host header retained for SMS. No real call, SMS or push notification was generated. Measurements include polling overhead and are not upper bounds for network faults or repeated failures.

In the final unattended reboot, Android reported boot completion at 36.8 seconds; all five service checks were simultaneously ready by 69.39 seconds. These timings include the operating-system restart and polling overhead; the initial 67-second baseline above used foreground-service presence and fresh monitor records, so it is not the same measurement.

The unattended reboot phase measurements and final installed artifact hashes are recorded in `D:/CodeX/HELP/reliability-20261031/speed-verification.json`. No unlock input or app-launch request was sent by that test. The device had no secure screen-lock credential before testing; no lock-screen setting was changed.

## 2026-10-31 — 1.4.4 known-MTK recovery

- A previously enabled MTK workaround now proceeds on an explicit BINDING state after the normal Shizuku permission check. It skips the redundant foreground start and natural-bind grace periods. All helper/gateway/Shizuku signatures, current no-call state, takeover/pending state and permission status remain checked.
- A monitor-originated bind repair reuses its already verified runtime; manual UI repairs retain staging and root-backed process reads. Bootstrap checks the identities and start ticks of both processes, the current gateway APK/version and the latest matching Shizuku request after gateway process start. It waits up to 1.5 seconds for that request; it does not grant permission, rewrite authorization or reuse an older process's token. Identity/request uncertainty retains the existing bounded normal-binding observation before fallback.
- Before attaching, bootstrap rechecks current gateway status and real call state, exits if normal binding already succeeded, revalidates identities and request generation, and honors a newly disabled monitor. CONNECTED gets the original 1.5-second readiness window for automatic SIP registration before a SIP restart is considered.
- Cached PID probes remain once per second as requested. Full checks retain their configured minute interval; transient retries remain 3 seconds and foreground-service checks remain 10 seconds. The monitor asset matches the previously tested 1.4.3 monitor, including inherited-lock descriptor closures.
- 26 existing policy checks, 5 readiness-wait scenarios and 34 MTK state/process/request checks passed. The unchanged monitor has nine passing isolated Android checks. Same production signature and local APK build verified. No production service, authorization, gateway APK or Asterisk APK was changed by these tests; actual endpoint recovery is measured separately.

1.4.4 device regression and correction: the initial candidate limited each `logcat -d` dump to 400 ms, but a read-only diagnostic on this MIUI device measured normal completion at about 503–505 ms. The initial candidate therefore rejected every request as missing and was not accepted as a working release. The per-dump allowance is now 1.5 seconds; the request-discovery loop remains bounded. Reusing the installed parsing code in a read-only root process confirmed that the current request passes process-time, APK path, version, daemon and timestamp predicates when the dump is allowed to finish. No token or account data was printed. This corrects the observed timeout regression; final endpoint recovery is verified separately after installation.
## 2026-10-31 — 1.4.5 daemon-request compatibility

The MTK request validator accepts both daemon=false and daemon=true from the signed gateway. This field controls the lifetime of an already authorized Shizuku service; it is not an authorization credential. Current Shizuku/gateway process identity, process-start time window, exact package, APK path/version, call state, permission state and manual switches remain checked. This change prepares joint validation with the separately developed warm-service gateway; it does not enable persistence in the gateway itself.

26 monitor policy checks, 5 readiness-wait scenarios and 39 MTK request/recovery checks passed. New cases accept the daemon=true request while still rejecting its stale time, wrong APK and wrong version. The one-second monitor and existing log-dump timeout fix are unchanged. No APK installation or production configuration change was performed during this build.
The final unpublished 1.4.5 build also contains a restricted early foreground start. Only the root monitor's observed loss of its cached gateway PID forwards an event. The selected package must match; the old PID must still be absent (unknown/EPERM/reused PIDs fail closed); monitor/background authorization, takeover, PBX-enabled state, Wi-Fi, all three signing identities and a live Shizuku server are checked. A complete live cellular/PBX call guard immediately precedes a second authorization/PID/signature check and the idempotent foreground-service request. Missing or uncertain conditions fall back to the original path.

The full PBX JSON/SIP and background-policy checks then continue while the gateway initializes. They still determine the final health result; fastStarted in health-status.json only records whether the early start ran. Normal boot, scheduled checks and user-requested checks do not carry this PID-loss event. The one-second PID cadence is unchanged.

An additional 33 host scenarios cover rejected events, missing prerequisites, active/unknown calls and stop/signature/PID changes during the final call check. All 103 host checks passed. Ten isolated monitor scenarios passed, including forwarding the exact selected package and old PID only after actual mock PID loss. No production service was modified or APK installed by these tests.

Cold-boot correction for the unpublished 1.4.5 build: joint warm-gateway testing measured full readiness after ordinary gateway process loss at 2.48, 2.62 and 2.53 seconds, with three seconds of stable observation and the same privileged-service PID. The subsequent reboot exposed two request_missing failures and full readiness only after 115.89 seconds. The daemon's pending service record survived a gateway-client replacement, while the helper incorrectly required every record to postdate that replacement. Both token checks now use the current Shizuku-server start boundary for daemon requests; non-daemon requests use the later of server/client start. Current PID/start-tick identities, exact package/APK/version, latest-token selection, signatures, permission/binding state, actual call state and monitor/manual-stop checks remain in force. A failed attachment still cannot report readiness. Twelve added regression cases cover a retained pending daemon request, non-daemon rejection across the same client boundary, prior-server rejection, future/invalid times and APK/version changes. The monitor and warm fast-start path are unchanged; post-fix device reboot validation is separate.

## 2026-10-31 phone recovery follow-up

The configured Redmi remained plugged in with its existing CPU and high-performance Wi-Fi locks active. Phone process observation now uses a one-second lightweight path; full checks retain their configured real-time intervals.

Three independent gateway process-exit measurements reached foreground + takeover + Shizuku connected + audio-ready + SIP-ready in [2.48, 2.62, 2.53] seconds. Three PBX engine process-exit measurements reached running + gateway registration in [6.0, 5.17, 5.22] seconds. Each fault was preceded by an idle cellular/PBX call check. No real call, SMS or notification was generated.

The final unattended reboot reported all services ready after 67.31 seconds, with no unlock input or activity/service launch by the observer. Measurements and artifact hashes: `D:/CodeX/HELP/reliability-20261031/phone-speed-verification.json`. These are single-device observations with polling overhead, not upper bounds.

After correcting the cold-start pending-request lifetime check, a further gateway process-exit test after reboot reached stable readiness in 2.47 seconds and retained the same idle privileged process. The final installed APK hashes were checked byte-for-byte against the local builds.

<!-- recovery-target-20261031:start -->
## 2026-10-31 恢复目标实机复测

红米 Note 10 Pro / MIUI 14，接电、息屏、ADB 连接，沿用现有唤醒设置。每次终止单个进程前均确认蜂窝与 PBX 空闲。

- 守护工作进程（电话网关）：0.41, 0.36 s（范围 0.36–0.41 s）。
- 电话网关业务进程退出至本地就绪：3.09, 2.69 s（范围 2.69–3.09 s）。整机重启后该项首次观测就绪：61.89 s。

电话单次设备请求补测（独立故障试验，未并入或替代原多次请求样本）：

- 电话网关：首次观测本地就绪 3.89 秒；分别再稳定观测 3.39 秒。

每次采样仅往返设备一次，并行检查网关与 PBX；目标进程身份前后一致，仍要求网关前台、状态应答及五项就绪标志、PBX 进程和命名空间身份、与配置匹配的动态注册记录，并至少持续抽样 3 秒。计时包含实际设备观测开销。

先前候选的实测记录（按管理 APK 的 SHA-256 区分，以下未计入最终版本样本）：

- `07d225e20b21`，电话网关守护：0.36 s。
- `07d225e20b21`，电话网关业务：2.66 s。
- `07d225e20b21`，电话网关守护：0.44 s。
- `07d225e20b21`，电话网关业务：3.00 s。
- `c12eb5669510`，电话网关守护：0.41 s。
- `c12eb5669510`，电话网关业务：3.09 s。
- `c12eb5669510`，电话网关守护：0.39 s。
- `c12eb5669510`，电话网关业务：2.97 s。

重启各时间均从同一次重启请求起算。全部六项首次齐备 61.97 s，稳定确认 66.12 s；无安全锁屏凭据，观察者未输入解锁、未主动启动界面或服务。强制深度休眠 77.66 s，4 次抽查六项服务与五个工作进程均正常，结束后已解除强制状态。

数值为本机含探测开销的首次观测，不是普适上限。守护与业务分别连续抽样确认至少 1 秒和 3 秒。电话就绪检查含网关状态及匹配的 PBX 注册记录；本次未拨打真实电话、发送短信或通知，也未验证外网送达或双向通话音频。
<!-- recovery-target-20261031:end -->


## 2026-11-01 reliability follow-up 1.4.7


增加网关内部线程无响应的受控恢复。只有网关自身连续返回状态请求超时，同一进程身份至少持续异常 30 秒且累计至少三次，才考虑恢复。操作前重复核验授权、网关签名、进程启动标识和蜂窝／PBX 通话空闲状态；恢复尝试间隔至少五分钟，手动停用和未知通话状态继续阻止恢复。

Shizuku 更新后，如果原 APK 指纹变化，守护只加载已固定校验的助手代码，单独验证官方签名及安装包稳定性。满足现有授权与空闲条件后，原子交接启动组件和指纹，下一轮继续常规检查。缺失、签名不符、正在通话或暂停授权时显示具体诊断；不会把未经核验的新依赖加入 Root 类路径。

本地测试覆盖连续无响应、进程替换、冷却、状态保存失败、签名不符、更新竞争和通话／停用保护。部署后的进程退出与整机重启实测见下；内部线程卡死恢复和实际通话音频仍需单独实机验收。

Adds bounded recovery for an unresponsive gateway runtime. Recovery requires at least three explicit status-timeout acknowledgements spanning 30 seconds from the same process identity. Consent, signing identity, process start ticks and independent cellular/PBX idle checks are repeated before signalling that process. Attempts are separated by a five-minute cooldown; manual stop and unknown call state remain authoritative.

When Shizuku's APK fingerprint changes, only the already pinned helper is loaded as root. It independently verifies the official signature and stable package contents, then atomically hands off the starter and fingerprint while authorized and idle. The next check resumes the ordinary dependency path. Missing or untrusted packages, active calls and paused authorization produce explicit diagnostics. A changed dependency is never loaded before verification.

Local regressions cover stalls, process replacement, cooldown, persistence failure, signature rejection, update races and call/stop guards. Post-deployment process-exit and reboot observations follow below; recovery from an internal thread stall and real-call audio still require separate device acceptance.

### 2026-11-01 最终 APK 业务退出及整机重启验收 / Business-process and reboot acceptance

最终 1.4.7 APK 的 SHA-256 为 `ddbc0c9c762969f5fa8bcb4cba938ec0593d336769aebb32a8b8d376e177c604`。部署后，单次受控终止电话网关业务进程，首次观测完整本地就绪为 **2.59 秒**，随后连续稳定确认至少 3 秒。同轮 PBX 业务进程退出测试为 2.97 秒，并单独通过至少 3 秒稳定确认。电话网关就绪检查要求进程存在、前台服务运行、状态请求获得应答，以及 `ready=true`、`takeover=on`、`sip=ready`、`shizuku=CONNECTED`、`audioReady=true`；同时核对 PBX 及匹配的网关注册记录。进程退出试验不等同于内部线程卡死试验。

随后进行一次整机重启，所有时间均从发出重启请求起算，包含系统启动：系统启动完成 **37.53 秒**，用户自动解锁 **37.84 秒**，PBX 网关注册 **54.66 秒**，电话网关完整本地就绪 **62.00 秒**，全套服务首次齐备 **62.06 秒**，在 **66.08 秒**完成持续 4.02 秒的稳定确认。观察者没有输入解锁指令，也没有主动启动 Activity 或服务；该红米设备 `deviceSecure=false`，因此这不是设置安全锁屏密码后首次解锁之前的恢复验证。

以上为单台设备各一次故障／重启实测，时间是包含查询延迟的首次成功观测，不是固定上限或通用保证。仅核验本地接口及状态；`audioReady=true` 不证明实际呼叫接通或双向通话音频，也未验证公网可达。重启记录：调查工作区 `reliability-20261031/private/full-retest-20261101-084900/reboot.json`。

The final 1.4.7 APK has SHA-256 `ddbc0c9c762969f5fa8bcb4cba938ec0593d336769aebb32a8b8d376e177c604`. After deployment, one controlled gateway business-process kill first reached full local readiness in **2.59 seconds**, followed by at least three seconds of stable observation. The separate PBX business-process trial reached readiness in 2.97 seconds and likewise passed at least three seconds of stability observation. Gateway readiness requires a live process, foreground service, acknowledged status request, and `ready=true`, `takeover=on`, `sip=ready`, `shizuku=CONNECTED` and `audioReady=true`, alongside a running PBX and matching gateway registration. A process-exit trial does not test an internal thread stall.

One subsequent whole-device reboot measured every stage from the reboot request, including operating-system startup: boot completed at **37.53 seconds**, the user unlocked automatically at **37.84 seconds**, the PBX gateway registered at **54.66 seconds**, full local gateway readiness appeared at **62.00 seconds**, and all services were first ready at **62.06 seconds**. Stability was confirmed at **66.08 seconds**, after 4.02 seconds of continuous readiness. The observer issued no unlock, Activity-start or service-start commands. The Redmi device reported `deviceSecure=false`; this does not validate recovery before the first credential unlock on a securely locked device.

These are one trial per fault/reboot on one device. Times are first successful observations including query latency, not fixed upper bounds or general guarantees. Checks cover local interfaces and state: `audioReady=true` does not prove a completed real call or two-way call audio, and public-network reachability was not tested. Reboot evidence is in the investigation workspace at `reliability-20261031/private/full-retest-20261101-084900/reboot.json`.
