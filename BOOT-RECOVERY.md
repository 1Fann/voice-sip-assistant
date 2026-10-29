# 开机自动恢复 / Boot recovery

## 中文

Voice SIP 助手 1.3.3 沿用既有 Root 开机入口，会等待 Android 启动和应用数据可用，然后准备 Shizuku 并启动已启用的网关自动检查。检查仍使用用户设置的间隔；Asterisk 由 Asterisk 助手独立恢复。所有恢复继续遵守通话中跳过、接管手动关闭及签名校验规则。

- 没有设置锁屏密码、PIN 或图案时，Android 通常会自动进入 `RUNNING_UNLOCKED`。显示滑动锁屏页面不妨碍这个状态，不要求用户滑开页面。
- 有锁屏凭据时，等待首次输入凭据，不绕过加密。等待不再限制为约 15 分钟；关闭开机准备和自动检查两个开关后会退出等待。
- 开机工作进程使用独立锁，避免重复启动。首次 Shizuku 启动设有超时；失败不会阻止自动检查启动。检查可以在网络、服务器就绪后再次尝试恢复。
- 不更改 SIP 密码、拨号规则、本机静音或系统电话默认应用。不通过保存锁屏密码、自动模拟输入来启动。

2026-10-29 Android 13 / Magisk 实机恢复验证：重启后未要求操作红米屏幕，DDNS、Asterisk、Shizuku、网关和两个检查任务自行恢复。用户确认 Groundwire 蜂窝网络回声正常；双卡呼出及两张卡的锁屏来电均双向有声。重启后再次确认回声、锁屏推送、号码分段及双向声音正常。

结果仅代表已验证条件；其他 ROM、有密码的设备、SIM PIN 或权限变更仍需单独测试。1.3.3 的界面整理不改变开机与网关控制逻辑。

## English

Version 1.3.3 retains the existing startup worker, which waits for Android boot completion and accessible app storage, prepares Shizuku, and starts the enabled gateway health monitor. The configured check interval is preserved. Asterisk Assistant independently owns server recovery. Active-call guards, manual takeover settings, and signature checks remain in effect.

Without a screen password, PIN, or pattern, Android normally reaches `RUNNING_UNLOCKED` automatically, even while the swipe lock screen is visible. With a screen credential, the worker waits for the first credential entry without bypassing encryption. The former approximately 15-minute waiting limit is removed. Disabling both boot preparation and monitoring cancels waiting.

A lock prevents duplicate boot workers. Initial Shizuku preparation has a timeout; failure does not prevent the health monitor from starting and retrying after network/server readiness. SIP credentials, routing, local call mute, and the default dialer are preserved. No lock-screen credential is stored or automatically typed.

2026-10-29 Android 13 / Magisk restore verification: DDNS, Asterisk, Shizuku, the gateway and both monitors recovered after reboot without asking the user to operate the Redmi screen. The user confirmed cellular-network Groundwire echo, outbound calls on both SIMs, and lock-screen incoming push with two-way audio on both SIMs. Echo, incoming push, number grouping and two-way audio passed again after reboot.

Results are limited to the tested conditions. Other ROMs, screen credentials, SIM PINs and permission changes require separate checks. The 1.3.3 UI cleanup does not change startup or gateway-control behavior.
