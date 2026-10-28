# Validation / 验证范围

Tested on one ARM64 MediaTek Redmi, Android17 port, BakaSU. Signed builds installed with prior data retained. Independent configurable monitors passed normal-state, immediate-check, monitor-off and manual-stop checks. With recent-task locks OFF, both helpers and gateway were force-stopped; independent root checks restored the foreground network owner, gateway privileged/audio readiness and server-confirmed SIP registration without keeping helper UI running. An external LAN SIP probe received401. Boot-entry duplicate suppression passed; prolonged deep sleep was not tested. No automated real cellular calls were placed.

The server and gateway monitors were separately paused to verify ownership: the gateway helper did not restart the Asterisk manager, and the server helper did not restart the gateway. Current default interval is5minutes, configurable1–1440. Policy tests cover interval input rejection and unknown/ringing/active-call deferral. Automated checks alone do not establish actual two-way cellular audio.

No phone accounts, passwords, device identifiers, logs, signing keys or Cloudflare data are included.

## 1.3.2 reboot verification / 重启验证

2026-10-28: an isolated device fixture simulated more than the former 180 boot waits, CRLF user-state output, a failed Shizuku starter with continued monitoring, and cancelling both startup settings. All passed. The production-signed APK updated the installed helper with data retained.

After an actual reboot without screen interaction, app storage unlocked automatically with no screen credential. DDNS, Asterisk, official Shizuku, and the gateway recovered while the swipe lock screen remained visible. Both monitors subsequently reported healthy service; gateway SIP registration, audio readiness, and local mute were verified. Authentication/echo self-test returned 60 RTP packets. Another device received a correlated IPv6 SIP challenge through the DDNS hostname. The user confirmed iPhone cellular-network echo on 600, a normal-number call with two-way audio, and answering an incoming call through Xiaomi interconnect. Individual SIM results were not separately reported.

实机无锁屏密码，重启后未滑开屏幕，所有后台服务自动恢复；用户确认蜂窝网络回声、普通号码双向通话、小米互联来电接听三项正常。没有据此声称所有 ROM、有密码设备、SIM PIN 或长时间深度休眠均已验证。详见 [BOOT-RECOVERY.md](BOOT-RECOVERY.md)。
