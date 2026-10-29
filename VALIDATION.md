# Validation / 验证范围

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
