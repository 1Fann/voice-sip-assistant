# Voice SIP Assistant

[中文](README.md) · [Download APK](https://github.com/1Fann/voice-sip-assistant/releases/latest) · [MIT](LICENSE)

Version **1.3.1**. An original Root deployment/management app for Voice SIP Agent and Shizuku; not an upstream official app or a SIP server.

It downloads official stable APKs with source/hash/certificate checks, prepares official Shizuku in Root mode, controls takeover and SIP reconnection, restores the system dialer, and provides a Root bootstrap for the tested MediaTek ROM's Shizuku UserService failure. User consent and service caller checks remain. It recognizes the separately signed [custom gateway](https://github.com/1Fann/voice-sip-gateway) and does not overwrite it with an official upgrade.

## Independent gateway recovery

Opt-in root checks run independently of the helper UI at1–1440minutes (default5), with immediate check and last result. They resume after boot and first unlock. Only Shizuku, gateway binding/audio readiness and SIP registration are repaired. [Asterisk Assistant](https://github.com/1Fann/asterisk-assistant) owns server recovery; server dependency checks here are read-only. A shared lock serializes recovery. Calls/unknown state or absent Wi-Fi defer repair; manual takeover OFF is respected. Local output mute is preserved. Severe binding failures may restart Shizuku and open the gateway activity, briefly affecting other Shizuku clients. Deep sleep can delay checks.

## Use

Android12+, ARM64 and Root are required; the tested compatibility bootstrap uses BakaSU/KernelSU BusyBox. Grant notifications and Root, deploy dependencies, then approve Shizuku and enter SIP credentials in the gateway itself. The helper does not store SIP passwords. Custom gateway updates are separate from official deployment. It does not implement an outgoing-only mode; Xiaomi interconnect coexistence requires device-specific routing and tests. Disable monitoring and optional Shizuku boot preparation before uninstalling. After helper/Shizuku updates, save monitoring settings again to refresh protected APK hashes.

## Build and signing

On Windows with JDK21, Android SDK35 and Build Tools36.0.0, run `./build.ps1 -SdkRoot C:/Android/Sdk -JavaHome C:/Java/jdk-21`. Keys are generated locally under ignored `private/`. Public release APKs retain the maintainer's existing signing identity; CI uses a disposable key to verify compilation. Self-signed builds must update certificate pins (`RootBridge.CERT` or `Root.DUALSIM_CERT`, including the latter's paired gateway identity). A different certificate cannot update an existing installation. No private signing keys or device configuration are published.

Upstream: [Voice SIP Agent](https://github.com/sparkcyf/android-voice-sip-agent) (GPL-2.0-or-later with its Apache-2.0 linking exception), [Shizuku](https://github.com/RikkaApps/Shizuku) (Apache-2.0). Dependency APKs are downloaded from official Releases, not bundled here; Shizuku is unmodified. See [validation](VALIDATION.md).
