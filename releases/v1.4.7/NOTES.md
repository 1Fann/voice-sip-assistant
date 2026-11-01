# Voice SIP Assistant 1.4.7

增加网关内部线程无响应的受控恢复。只有网关自身连续返回状态请求超时，同一进程身份至少持续异常 30 秒且累计至少三次，才考虑恢复。操作前重复核验授权、网关签名、进程启动标识和蜂窝／PBX 通话空闲状态；恢复尝试间隔至少五分钟，手动停用和未知通话状态继续阻止恢复。

Shizuku 更新后，如果原 APK 指纹变化，守护只加载已固定校验的助手代码，单独验证官方签名及安装包稳定性。满足现有授权与空闲条件后，原子交接启动组件和指纹，下一轮继续常规检查。缺失、签名不符、正在通话或暂停授权时显示具体诊断；不会把未经核验的新依赖加入 Root 类路径。

本地测试覆盖连续无响应、进程替换、冷却、状态保存失败、签名不符、更新竞争和通话／停用保护。部署后的进程退出与整机重启实测见下；内部线程卡死恢复和实际通话音频仍需单独实机验收。

Adds bounded recovery for an unresponsive gateway runtime. Recovery requires at least three explicit status-timeout acknowledgements spanning 30 seconds from the same process identity. Consent, signing identity, process start ticks and independent cellular/PBX idle checks are repeated before signalling that process. Attempts are separated by a five-minute cooldown; manual stop and unknown call state remain authoritative.

When Shizuku's APK fingerprint changes, only the already pinned helper is loaded as root. It independently verifies the official signature and stable package contents, then atomically hands off the starter and fingerprint while authorized and idle. The next check resumes the ordinary dependency path. Missing or untrusted packages, active calls and paused authorization produce explicit diagnostics. A changed dependency is never loaded before verification.

Local regressions cover stalls, process replacement, cooldown, persistence failure, signature rejection, update races and call/stop guards. Post-deployment process-exit and reboot observations follow below; recovery from an internal thread stall and real-call audio still require separate device acceptance.

2026-11-01 最终版本实机补测：单次电话网关业务进程退出后首次观测完整本地就绪 **2.59 秒**，随后稳定确认至少 3 秒。一次整机重启从发出请求开始计时，包含系统启动：系统完成 **37.53 秒**、用户自动解锁 **37.84 秒**、PBX 网关注册 **54.66 秒**、电话网关完整本地就绪 **62.00 秒**、全套服务齐备 **62.06 秒**，**66.08 秒**完成稳定确认。观察者未输入解锁或主动启动界面／服务；设备 `deviceSecure=false`，不代表设置安全密码后的首次解锁前恢复能力。这些单台设备、各一次、包含查询开销的观测不是时限保证。进程退出试验不等同于内部线程卡死试验；本地就绪及 `audioReady=true` 不证明公网可达、实际呼叫或双向通话音频。

Final-version device follow-up on 2026-11-01: one gateway business-process kill first reached full local readiness in **2.59 seconds**, followed by at least three seconds of stable observation. One whole-device reboot was timed from the request, including operating-system startup: boot completed at **37.53 seconds**, automatic user unlock at **37.84 seconds**, PBX gateway registration at **54.66 seconds**, full local gateway readiness at **62.00 seconds**, all services ready at **62.06 seconds**, and stability confirmed at **66.08 seconds**. The observer issued no unlock or Activity/service-start commands. The device reported `deviceSecure=false`, so this does not establish recovery before the first credential unlock on a securely locked device. These single-device, single-trial observations include query overhead and are not timing guarantees. A process-exit trial does not test an internal thread stall; local readiness and `audioReady=true` do not prove public-network reachability, a real completed call or two-way call audio.
