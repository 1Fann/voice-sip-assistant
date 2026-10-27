# voice-sip-assistant v1.3.1

## 中文

独立网关自动检查：可配置 1–1440 分钟，默认 5 分钟；检查 Shizuku、网关特权与音频就绪、SIP 状态及服务器登记。只读查看服务器依赖，不会启动或重启 Asterisk。

下载 `Voice-SIP-Assistant-1.3.1.apk`，可以覆盖升级相同签名的旧版。附件还提供对应源码和 SHA256SUMS。不含手机账号、密码或签名私钥。

两个助手的 Root 检查任务独立于界面，清理最近任务后仍能检查；深度休眠可能延后检查，恢复也不能证明实际通话音质。已验证范围和已知限制见 README / VALIDATION 或 CHANGES。

## English

Independent gateway recovery, configurable 1–1440 minutes (default 5), Shizuku/privileged audio/SIP readiness and server registration checks. Asterisk is observed without starting or restarting it.

Download `Voice-SIP-Assistant-1.3.1.apk` to update an installation with the matching signature. Corresponding source and SHA256SUMS are attached. No device accounts, passwords or signing keys are included. See the repository's validation notes for tested scope and remaining device/call checks.
