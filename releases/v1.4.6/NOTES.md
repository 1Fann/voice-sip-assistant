# Voice SIP Assistant 1.4.6

- 修复限时检查/升级结束后的锁残留：总计时器在加锁前启动，内部限时查询关闭继承锁；实际检查期间仍完整持有两把锁。
- 升级只在短暂交接步骤内持有业务检查锁：等原检查结束后，仅通知身份仍与快照一致的旧监护进程退出，随即放行检查。接任者已经启动时让位，不再等待两个监护槽同时空闲；未知占用约 4 秒后结束本次交接尝试，不改变手动设置。
- 修复网关在完整检查中退出后丢失原进程证据，以及已观察退出后因争锁而丢弃证据的问题；若出现替代进程或选择的包改变，会取消旧证据。首次查询始终没有 PID 时只增加一次普通复查，不跳过任何业务启动条件。
- 已知网关进程每约 1 秒检测一次；前台服务每 10 秒检查，完整检查仍按原配置执行。工作进程意外退出由独立监护重建。
- 恢复前直接查询当前 PID / Wi-Fi，减少重复命令启动；保留完整通话检查、启动前两次授权和签名检查、手动停止及唤醒偏好。
- 包含常驻权限服务兼容及冷启动待绑定请求修复，建议与 Voice SIP Gateway 0.3.5-custom.5 配套升级。只接受当前服务进程、签名、APK/版本和最新授权请求。
- root 检查进程通过子命令退出通知减少短命令的等待开销；原有超时、错误处理、普通应用授权流程和业务检查条件不变。120 项 Java 离线检查和 5 组实际守护循环模拟测试通过，生产签名不变，可覆盖升级并保留原配置。
- 本版 root 守护脚本与上一候选 `c12eb566…` 相同，后者已核对手机安装包与部署脚本一致。在实际 Magisk BusyBox 的隔离目录中，7 组锁/升级交接检查及 2 组真实 PID/争锁检查全部通过，覆盖旧槽占用时新检查可执行、保留合法接任进程、有限让位，以及检查中退出/争锁后保留同一死亡证据。使用模拟业务服务，不代表实拨通话或固定恢复时限。

Check/upgrade deadlines are outside all acquired locks; nested query timers do not inherit them. The active checker still owns both locks until it exits. Known-process observation stays at about one second, without faster permanent polling. Direct liveness/network observations reduce startup work. Active/unknown calls, current authorization, signatures, manual stop and the corrected daemon request lifetime rules remain enforced. Pair with gateway custom.5. Existing settings and the production signing identity are retained.

Detection cadence and host tests do not establish an exact recovery deadline, uninterrupted connectivity or real-call audio success. Initial unlock, unavailable dependencies and system/network failures retain their existing limits. No phone/SMS test traffic or private account data is included in this release.

The monitor now retains a confirmed gateway exit across a blocking check and retryable lock contention. Replacement processes and package changes invalidate old evidence. A missing first observation triggers one early ordinary recheck; all existing authorization and call-safety checks remain active.

Upgrade releases the business check lock as soon as the old monitor owners have been signaled. Successor recognition and bounded slot retries occur without blocking checks. A replacement does not have to vacate its own slot for handoff to finish.

The root monitor script is unchanged from the installed and verified `c12eb566…` candidate. Seven lock/handoff groups and two real-PID/lock-contention groups passed in isolated fixtures on the phone's actual Magisk BusyBox. The final APK additionally observes UID 0 shell subprocess exits without the timed polling tick, keeping original deadlines and ordinary app behavior. The five additional host cases exercise completion, errors and cancellation; measurements for different APK digests should be kept separate.
