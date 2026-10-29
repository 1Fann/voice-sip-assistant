# Voice SIP Assistant

**Latest: 1.4.1** — [新手机部署与权限说明 / New-phone setup](NEW-PHONE-SETUP.md)

One-click deployment now selects the fixed integrated gateway. Signature-protected Shizuku recovery, actionable phone-permission diagnostics, root-provider discovery and automatic runtime refresh.

[中文](README.md) · [Download APK](https://github.com/1Fann/voice-sip-assistant/releases/latest) · [MIT](LICENSE)

Version **1.4.1**. An original Root deployment/management app for Voice SIP Agent and Shizuku; not an upstream official app or a SIP server.

It downloads the production-signed integrated gateway and official Shizuku APKs with source/hash/certificate checks, prepares official Shizuku in Root mode, controls the phone gateway and SIP reconnection, offers system-dialer restoration under advanced repair, and provides a Root bootstrap for the tested MediaTek ROM's Shizuku UserService failure. User consent and service caller checks remain. It recognizes the separately signed [custom gateway](https://github.com/1Fann/voice-sip-gateway) and does not overwrite it with an official upgrade.

## Update 1.4.1

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
