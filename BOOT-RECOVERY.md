# 开机自动恢复 / Boot recovery

## 中文

Voice SIP 助手 1.3.2 的 Root 开机入口会等待 Android 启动和应用数据可用，然后准备 Shizuku 并启动已启用的网关自动检查。检查仍使用用户设置的间隔；Asterisk 由 Asterisk 助手独立恢复。所有恢复继续遵守通话中跳过、接管手动关闭及签名校验规则。

- 没有设置锁屏密码、PIN 或图案时，Android 通常会自动进入 `RUNNING_UNLOCKED`。显示滑动锁屏页面不妨碍这个状态，不要求用户滑开页面。
- 有锁屏凭据时，等待首次输入凭据，不绕过加密。等待不再限制为约 15 分钟；关闭开机准备和自动检查两个开关后会退出等待。
- 开机工作进程使用独立锁，避免重复启动。首次 Shizuku 启动设有超时；失败不会阻止自动检查启动。检查可以在网络、服务器就绪后再次尝试恢复。
- 不更改 SIP 密码、拨号规则、本机静音或系统电话默认应用。不通过保存锁屏密码、自动模拟输入来启动。

2026-10-28 实机重启验证：未操作手机屏幕，手机仍显示锁屏页面时，应用数据自动解锁，DDNS、Asterisk、Shizuku 和网关启动。两个检查器报告正常，网关显示 SIP 已注册、音频就绪、本机静音开启。认证与回声自检收到 60 个回传音频包；另一设备通过域名取得 IPv6 SIP 认证挑战。用户随后确认：在不操作红米屏幕的情况下，iPhone 蜂窝网络 Linphone 的 600 回声、普通号码双向通话，以及第三部手机来电后通过小米互联在 iPhone 接听，三项均正常。这次反馈未单独区分两个 SIM 的测试结果。

仅保证已验证条件；移植系统、未来 ROM 更新、SIM PIN 或权限变更可能影响启动。没有把小米互联系统组件改造成 Direct Boot 应用。

## English

Version 1.3.2 waits for Android boot completion and accessible app storage, prepares Shizuku, and starts the enabled gateway health monitor. The configured check interval is preserved. Asterisk Assistant independently owns server recovery. Active-call guards, manual takeover settings, and signature checks remain in effect.

Without a screen password, PIN, or pattern, Android normally reaches `RUNNING_UNLOCKED` automatically, even while the swipe lock screen is visible. With a screen credential, the worker waits for the first credential entry without bypassing encryption. The former approximately 15-minute waiting limit is removed. Disabling both boot preparation and monitoring cancels waiting.

A lock prevents duplicate boot workers. Initial Shizuku preparation has a timeout; failure does not prevent the health monitor from starting and retrying after network/server readiness. SIP credentials, routing, local call mute, and the default dialer are preserved. No lock-screen credential is stored or automatically typed.

Device reboot verification on 2026-10-28, without screen interaction: app storage became available while the lock screen remained visible; DDNS, Asterisk, Shizuku, and the gateway started. Both monitors reported healthy service, including registered SIP, audio readiness, and local mute enabled. Authentication/echo self-test returned 60 audio packets, and a separate device received an IPv6 SIP challenge through the DDNS hostname. The user subsequently confirmed all three post-reboot tests without interacting with the Redmi screen: Linphone echo on extension 600 over iPhone cellular data, a regular-number call with two-way audio, and answering a third phone's incoming call on the iPhone through Xiaomi interconnect. SIM-specific results were not separately reported.

This does not establish compatibility with every ROM, future system update, SIM PIN configuration, or permission change, and does not modify Xiaomi interconnect for Direct Boot.
