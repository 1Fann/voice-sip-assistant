# Voice SIP Assistant

**Version: 1.4.6** — [新手机部署与权限说明 / New-phone setup](NEW-PHONE-SETUP.md)

One-click deployment now selects the fixed integrated gateway. Signature-protected Shizuku recovery, actionable phone-permission diagnostics, root-provider discovery and automatic runtime refresh.

[中文](README.md) · [Download APK](https://github.com/1Fann/voice-sip-assistant/releases/latest) · [MIT](LICENSE)

Version **1.4.6**. An original Root deployment/management app for Voice SIP Agent and Shizuku; not an upstream official app or a SIP server.

It downloads the production-signed integrated gateway and official Shizuku APKs with source/hash/certificate checks, prepares official Shizuku in Root mode, controls the phone gateway and SIP reconnection, offers system-dialer restoration under advanced repair, and provides a Root bootstrap for the tested MediaTek ROM's Shizuku UserService failure. User consent and service caller checks remain. It recognizes the separately signed [custom gateway](https://github.com/1Fann/voice-sip-gateway) and does not overwrite it with an official upgrade.


## Update 1.4.6

Known gateway process liveness is observed about once per second. Recovery retains current call-state, signature, authorization and manual-stop guards. Direct PID and Wi-Fi observations reduce startup work, with support for idle privileged-service reuse and daemon requests retained across a cold-boot client replacement. Pair with gateway custom.5. Detection cadence is not a fixed service-recovery guarantee; readiness still requires current permission, audio, SIP and server checks.

Check and upgrade deadlines start before their locks are acquired. Nested query timers cannot inherit those locks, so completed work releases them immediately instead of delaying the next recovery. The actual checker retains both singleton and shared call-recovery locks for its entire lifetime.

## Reliability hardening 1.4.2

A singleton supervisor recreates a terminated health worker. Shared repair-lock contention is nonblocking and retried without waiting for the full configured interval. System queries have timeouts, and runtime scripts are replaced atomically so refreshes cannot truncate an executing script. Asterisk boot and sticky-service recovery preserve an explicit manual stop; disabled autostart cancels boot waiting.

Existing call-safety checks, signing checks, intervals and wake settings remain in effect. No automatic device reboot or credential-unlock bypass is added. Isolated regression tests cover contention, duplicate starts, worker death and disabling. Long screen-off operation, password-protected reboot, supervisor/device failure and external reachability still need separate validation.

## Previous update 1.4.1

The reproduced failure involved Android background restrictions, disabled MIUI autostart, and a readiness check accepting stale registration without a foreground service. Shortening the full-check interval alone was insufficient.

The assistants now show a background-access setup prompt. After explicit confirmation, Root changes are limited to the assistant and its trusted paired gateway: background execution, battery-optimization exemption, and recognized MIUI autostart. Phone, microphone, notification, and Shizuku consent remain separate. Unsupported settings identify the failed step instead of incorrectly reporting missing Root.

When monitoring is enabled, an approximately 10-second lightweight check detects a missing foreground service. Full checks retain the configured 1–1440-minute interval. Consented background settings may be repaired if restrictions return. Server and gateway recovery retain separate ownership, a shared repair lock, active-call guards and manual-OFF behavior. Recovery is not uninterrupted availability.

The existing Asterisk dedicated-wake option now also controls a renewable, bounded root-monitor wake lease. Disabling it releases the lease. DDNS keeps its separate existing daemon and wake option.

## Independent gateway recovery

Opt-in root checks run independently of the helper UI at1–1440minutes (default5), with immediate check and last result. They resume after boot when app storage is available. Without a screen password, PIN, or pattern, manually dismissing the lock screen is normally unnecessary. Credential-protected devices wait for the first unlock without the former approximately 15-minute limit. Only Shizuku, gateway binding/audio readiness and SIP registration are repaired. [Asterisk Assistant](https://github.com/1Fann/asterisk-assistant) owns server recovery; server dependency checks here are read-only. A shared lock serializes recovery. Calls/unknown state or absent Wi-Fi defer repair; manual gateway OFF is respected. Local output mute is preserved. Severe binding failures may restart Shizuku and open the gateway activity, briefly affecting other Shizuku clients. Deep sleep can delay checks.

Duplicate boot workers are suppressed. Failed initial Shizuku preparation still starts monitoring for later recovery. See [boot recovery and device verification](BOOT-RECOVERY.md).

## Use

Android12+, ARM64 and Root are required; the apps discover Magisk, KernelSU/BakaSU and APatch BusyBox paths automatically. No manually created alias is required. Grant notifications and Root, deploy dependencies, then approve Shizuku and enter SIP credentials in the gateway itself. The helper does not store SIP passwords. One-click deployment installs or updates the integrated gateway, retaining existing same-signature configuration. The phone-gateway switch enables both cellular incoming forwarding and outgoing SIM calls through a PBX. Turning it off stops both paths; a separate PBX echo test can still work. Companion access preserves the normal phone UI and remains required for the tested setup. Fresh deployment no longer forces local call mute off; updates and switch-on preserve the user setting. Disable monitoring and optional Shizuku boot preparation before uninstalling. After helper updates, runtime hashes are refreshed automatically; opening the app retries if the ROM blocked its update broadcast.

## Build and signing

Normal builds verify the existing signing certificate against the app's pinned identity before packaging. Disposable compilation-only CI builds must explicitly pass `-DevelopmentSigning`; their APKs cannot update production installs.

On Windows with JDK21, Android SDK35 and Build Tools36.0.0, run `./build.ps1 -SdkRoot C:/Android/Sdk -JavaHome C:/Java/jdk-21`. Keys are generated locally under ignored `private/`. Public release APKs retain the maintainer's existing signing identity; CI uses a disposable key to verify compilation. Self-signed builds must update certificate pins (`RootBridge.CERT` or `Root.DUALSIM_CERT`, including the latter's paired gateway identity). A different certificate cannot update an existing installation. No private signing keys or device configuration are published.

Upstream: [Voice SIP Agent](https://github.com/sparkcyf/android-voice-sip-agent) (GPL-2.0-or-later with its Apache-2.0 linking exception), [Shizuku](https://github.com/RikkaApps/Shizuku) (Apache-2.0). Dependency APKs are downloaded from the integrated gateway and official Shizuku Releases, not bundled here; Shizuku is unmodified. See [validation](VALIDATION.md).

## 1.4.3 local recovery-speed update (2026-10-31)

The first monitor-worker exit is restarted immediately; repeated short failures back off. Cached service and engine PIDs are checked once per second without a Binder query, retaining the existing 10-second foreground-service check. Explicit transient dependency waits retry after 3 seconds for six attempts before slowing down. Boot readiness is polled once per second. Runtime handover waits for an existing repair check before replacing monitor owners.

Detection timings are not full call-readiness guarantees. Active/unknown call guards, manual stops, signature validation and credential-storage restrictions remain in force.
## 1.4.4 known-MTK binding recovery (2026-10-31)

When the existing MTK compatibility mode is enabled and the gateway has entered an authorized BINDING state, recovery can attach the current request without redundant startup grace periods. Current process identity, request generation, APK version, signatures, call state, manual switches and permissions remain checked. Normal SIP registration gets its existing readiness window after connection.

Cached PID probes remain once per second, following the user's preference. Full checks retain their configured interval, transient retries remain 3 seconds and foreground checks remain 10 seconds. Detection latency does not guarantee complete call-readiness latency.

## 1.4.5 daemon-request compatibility (2026-10-31)

Accepts either standard Shizuku service-lifetime mode from the signed gateway. A pending daemon request may predate the current gateway client, but must belong to the current Shizuku server. Non-daemon requests additionally require the current gateway generation. Latest-request, both process identities, APK/version, permission, call-state and manual-stop checks remain enforced. Monitoring stays at one second; this helper update does not itself change the gateway lifetime setting.
A restricted early start is also included: only an observed old gateway PID loss, matching selected package and successful live authorization/signature/call preflight can start the foreground service ahead of the full PBX/SIP/background checks. Those checks still run and final readiness is unchanged. A successful start request is not reported as recovered telephone service.

## 1.4.6 warm-recovery observations (2026-10-31)

The root checker now queries process liveness and current `wlan0` IPv4 addresses directly instead of spawning repeated command processes. Only ESRCH plus verified `/proc` absence can enter the existing fast-start guard; access errors, PID reuse, active/unknown calls, revoked authorization and manual stop still reject it. Both signature/authorization checks and the corrected cold-boot daemon-token rules remain intact. The monitor still checks known process liveness about once per second and foreground ownership every ten seconds. Recovery readiness still requires current audio and SIP checks, not cached readiness or a fixed-time claim.
