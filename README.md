# Voice SIP 助手 / Voice SIP Assistant

**Latest: 1.4.0** — [新手机部署与权限说明 / New-phone setup](NEW-PHONE-SETUP.md)

默认一键部署已修复的整合网关；受签名保护的 Shizuku 绑定恢复；真实电话信息异常时停止无效重启；Root 路径自动识别，升级自动刷新运行组件。

[English](README.en.md) · [下载 APK](https://github.com/1Fann/voice-sip-assistant/releases/latest) · [MIT](LICENSE)

当前版本 **1.4.0**。用于部署、管理 Voice SIP Agent 与 Shizuku 的原创 Root 工具，不是上游官方 App，也不是 SIP 服务器。

- 获取上游最新正式版 APK，校验来源、摘要和签名；安装或升级时保留数据。
- 准备并以 Root 模式启动官方 Shizuku；支持可选开机准备。
- 查看状态、启停手机通话网关、重连 SIP、打开配置；在高级修复中恢复系统默认电话。
- 针对已测试联发科 ROM 的 Shizuku UserService 启动异常，提供 Root 兼容入口；保留用户授权与调用身份检查。
- 识别配套 [Voice SIP 网关](https://github.com/1Fann/voice-sip-gateway) 定制签名；不会用官方升级覆盖定制网关。

## 检查与恢复

可独立开启、关闭，间隔1–1440分钟（默认5），支持立即检查和上次结果。Root 检查任务独立于界面，清理后台后仍能检查；开机应用数据可用后恢复。未设置锁屏密码、PIN 或图案时通常无需手动滑开屏幕；有凭据时持续等待首次解锁，不再限制为约 15 分钟。通话中或无法确认通话状态时跳过恢复，Wi-Fi 未就绪时等待。关闭检查不会停止现有服务。深度休眠可能延后检查；清理至下次检查之间可能短暂不可用。

本助手只恢复 Shizuku、网关权限连接、音频就绪和 SIP 注册。Asterisk 由 [Asterisk 助手](https://github.com/1Fann/asterisk-assistant) 独立恢复；本助手只读检查服务器依赖。手机通话网关被手动停用时不拉起，恢复保留本机静音设置。严重权限故障可能重启 Shizuku 并打开网关页面，其他 Shizuku 客户端可能短暂断开。

开机入口防止重复启动；首次准备 Shizuku 失败仍启动检查，后续按配置重试。详见[开机恢复与实测](BOOT-RECOVERY.md)。

## 使用

Android12+、ARM64、Root；自动识别 Root 管理器的 BusyBox，无需软链接。安装后允许通知并授予 Root，再部署依赖，在网关内自行授权 Shizuku、填写 SIP 账号。账号不由助手保存。一键部署默认下载本项目的整合网关；保留同签名已有配置。

手机通话网关是双向蜂窝与 SIP 桥接的总开关。启用后将手机来电送到服务器，并允许 Groundwire 等 SIP 客户端通过手机 SIM 呼出；保留系统电话界面。停用后这两条通话链路停止，但独立 Asterisk 的回声测试仍可能正常。恢复默认电话仅用于高级修复。首次部署不再额外强制关闭本机通话静音，升级和启用均保留用户设置。开机准备与自动恢复开关独立；卸载前均应关闭。Root 文件位于 `/data/adb/voice-sip-assistant`。更新助手后自动刷新运行代码校验值；若系统拦截更新广播，打开助手即可重新检查。

## 自行构建与签名

Windows、JDK21、Android SDK35、Build Tools36.0.0：

```powershell
./build.ps1 -SdkRoot C:/Android/Sdk -JavaHome C:/Java/jdk-21
```

普通构建先核对现有签名与 App 信任指纹，失配立即停止。仅用于编译验证的临时 CI 构建需显式添加 `-DevelopmentSigning`；其 APK 不能用于覆盖升级正式安装。

构建脚本在本机 `private/` 生成签名文件。公开发布 APK 使用维护者的既有签名，可覆盖升级已安装版本；CI 使用临时签名，只验证构建。自行签名需要同步修改代码中的证书指纹：Asterisk 的 `RootBridge.CERT`；Voice SIP 助手的 `Root.DUALSIM_CERT`（其自身及配套网关信任指纹）。不同签名不能直接覆盖安装。私钥、密码和手机配置不在仓库中。

## 上游与验证

- [Voice SIP Agent](https://github.com/sparkcyf/android-voice-sip-agent)：GPL-2.0-or-later，含上游 Apache-2.0 链接例外。
- [Shizuku](https://github.com/RikkaApps/Shizuku)：Apache-2.0。

助手不捆绑依赖 APK，按需从本项目网关和官方 Shizuku Releases 下载，且不修改官方 Shizuku APK。助手自身 MIT。测试范围见 [VALIDATION.md](VALIDATION.md)。
