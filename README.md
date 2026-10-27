# Voice SIP 助手 / Voice SIP Assistant

[English](README.en.md) · [下载 APK](https://github.com/1Fann/voice-sip-assistant/releases/latest) · [MIT](LICENSE)

当前版本 **1.3.1**。用于部署、管理 Voice SIP Agent 与 Shizuku 的原创 Root 工具，不是上游官方 App，也不是 SIP 服务器。

- 获取上游最新正式版 APK，校验来源、摘要和签名；安装或升级时保留数据。
- 准备并以 Root 模式启动官方 Shizuku；支持可选开机准备。
- 查看状态、启停接管、重连 SIP、打开配置、恢复系统电话。
- 针对已测试联发科 ROM 的 Shizuku UserService 启动异常，提供 Root 兼容入口；保留用户授权与调用身份检查。
- 识别配套 [Voice SIP 网关](https://github.com/1Fann/voice-sip-gateway) 定制签名；不会用官方升级覆盖定制网关。

## 检查与恢复

可独立开启、关闭，间隔1–1440分钟（默认5），支持立即检查和上次结果。Root 检查任务独立于界面，清理后台后仍能检查；开机首次解锁后恢复。通话中或无法确认通话状态时跳过恢复，Wi-Fi 未就绪时等待。关闭检查不会停止现有服务。深度休眠可能延后检查；清理至下次检查之间可能短暂不可用。

本助手只恢复 Shizuku、网关权限连接、音频就绪和 SIP 注册。Asterisk 由 [Asterisk 助手](https://github.com/1Fann/asterisk-assistant) 独立恢复；本助手只读检查服务器依赖。接管被手动关闭时不拉起，恢复保留本机静音设置。严重权限故障可能重启 Shizuku 并打开网关页面，其他 Shizuku 客户端可能短暂断开。

## 使用

Android12+、ARM64、Root；兼容入口目前使用 BakaSU/KernelSU 的 BusyBox。安装后允许通知并授予 Root，再部署依赖，在网关内自行授权 Shizuku、填写 SIP 账号。账号不由助手保存。定制网关采用独立发布更新；官方部署按钮不会覆盖它。

网关仍沿用上游的蜂窝来电逻辑，本助手没有新增真正“只允许呼出”的模式。小米互联等共存需结合服务器路由和设备实测。开机准备与自动恢复开关独立；卸载前均应关闭。Root 文件位于 `/data/adb/voice-sip-assistant`。更新助手或 Shizuku 后，重新保存检查设置以刷新代码校验值。

## 自行构建与签名

Windows、JDK21、Android SDK35、Build Tools36.0.0：

```powershell
./build.ps1 -SdkRoot C:/Android/Sdk -JavaHome C:/Java/jdk-21
```

构建脚本在本机 `private/` 生成签名文件。公开发布 APK 使用维护者的既有签名，可覆盖升级已安装版本；CI 使用临时签名，只验证构建。自行签名需要同步修改代码中的证书指纹：Asterisk 的 `RootBridge.CERT`；Voice SIP 助手的 `Root.DUALSIM_CERT`（其自身及配套网关信任指纹）。不同签名不能直接覆盖安装。私钥、密码和手机配置不在仓库中。

## 上游与验证

- [Voice SIP Agent](https://github.com/sparkcyf/android-voice-sip-agent)：GPL-2.0-or-later，含上游 Apache-2.0 链接例外。
- [Shizuku](https://github.com/RikkaApps/Shizuku)：Apache-2.0。

助手不捆绑两个上游 APK，按需从官方 Releases 下载，且不修改官方 Shizuku APK。助手自身 MIT。测试范围见 [VALIDATION.md](VALIDATION.md)。
