## 中文

- 开机等待应用数据不再限制为约 15 分钟；无锁屏密码时通常无需手动滑开屏幕。
- 防止重复启动；首次启动 Shizuku 失败时仍启动自动检查，等待网络和服务器就绪后恢复。
- 保留检查间隔、SIP 账号、本机静音和手动停用行为，Asterisk 仍由独立助手管理。
- 已实测重启后不操作红米屏幕：服务恢复、IPv6 认证及回声通过；用户确认 iPhone 蜂窝回声、普通号码双向通话、小米互联接听均正常。结果仅代表已测试设备与配置。

APK 使用既有维护者签名，可直接覆盖升级。升级后点击“立即检查”或重新保存自动检查设置，刷新受保护的 APK 校验值。手机上已安装并验证本版，无需重复安装。

## English

- Remove the approximately 15-minute app-storage wait limit. Devices without a screen credential normally need no manual lock-screen dismissal.
- Suppress duplicate boot workers and start monitoring even when initial Shizuku preparation fails. Recovery can retry after network/server readiness.
- Preserve intervals, SIP credentials, local mute, and manual stop behavior; Asterisk recovery remains independently managed.
- Verified an actual no-interaction Redmi reboot, IPv6 authentication and echo. The user confirmed iPhone cellular echo, a regular-number call with two-way audio, and Xiaomi interconnect incoming calls. This does not establish compatibility with every device or configuration.

The APK retains the existing maintainer signature. After updating, tap immediate check or save monitoring settings again to refresh protected APK hashes. APK, corresponding source archive, and SHA256SUMS are attached.
