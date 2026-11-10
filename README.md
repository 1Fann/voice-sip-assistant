# Voice SIP 助手

中文 | [English](README.en.md)

管理 Android 通话网关及 Shizuku，提供可选的 Root 后台检查和恢复。它不是 SIP 服务器。

[下载安装包](https://github.com/1Fann/voice-sip-assistant/releases/latest) · [更新说明](https://github.com/1Fann/voice-sip-assistant/releases)

## 功能

- 安装、升级和打开 Voice SIP Agent。
- 准备并启动官方 Shizuku，检查通话和音频权限。
- 查看网关状态、重新连接 SIP，并管理开机准备。
- 支持配套定制网关；不会用官方版本覆盖定制版本。
- 可选自动检查和恢复，尊重手动停止及通话保护。

## 使用

Root 部署与独立守护需要已授权的 Root 环境及 BusyBox。网关仍需按提示授予电话、麦克风和 Shizuku 权限。

从 Releases 下载，按安装说明完成授权，在本机填写自己的账号和连接参数。同包名、同签名的版本可覆盖升级并保留数据。

## 文档

- [新手机安装](NEW-PHONE-SETUP.md)
- [开机恢复](BOOT-RECOVERY.md)
- [验证范围](VALIDATION.md)

## 许可证

助手采用 [MIT](LICENSE)；Voice SIP Agent、Shizuku 及其他组件保留原许可证。
