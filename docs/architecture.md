# 闪光摩斯 · 架构文档

本文件是代码注释中 `架构文档 §X.X` 引用的唯一出处。

早期版本曾在注释里引用一份仓库外的「规划文档」，那份文档没有随代码发布，
导致所有 `§X.X` 引用对新维护者都是断链。这里把被引用到的结论固化为本文档，
章节号保持不变，以便逐条对照。

**约定**：本文档只记录「为什么这样设计」与「不可随意改动的约束」。
实现细节以代码为准，历史变更以 `git log` 为准——不在这里复述推演过程。

---

## 3. 功能边界

### §3.2 码表范围：只做 ITU-R M.1677-1 字符集

编码器覆盖 26 个字母、10 个数字与 19 个常用标点。**中文不在此列**：
`TextNormalizer` 会把任何无法编码的字符替换为 `?`，宁可发出一个可见的错误符号，
也不静默丢字（验收要求：无丢字、无乱序）。

内置短语因此采用「中文标签 + 英文报文」的结构（`Phrase.label` / `Phrase.payload`），
中文只承担展示职责。中文电报码（四位数字）是 v2 的候选方向，不在当前范围内。

原实现中的 7 组 prosign（`SK` / `AR` / `K` / `BT` / `VE` / `HH` / `SOS`）已移除：
它们没有 UI 入口，且 prosign 要求「连续符号、不插入字符间隔」，
与现有 `ScriptCompiler` 按字母切分的模型不兼容，需要单独的时间轴构造路径。

---

## 4. 发光、热与中断

### §4.1 零权限是产品的信任基石

主力通道使用 `CameraManager.setTorchMode`，自 API 23 起**不需要 CAMERA 权限**。
应用不申请 INTERNET 权限，离线是核心卖点。任何引入权限的改动都要先论证：
一旦首次启动弹权限窗，绝大多数用户会在那一刻离开。

### §4.2 热保护与外部抢占

**热保护**：发送期间由独立协程每 2 s 读取一次 `PowerManager.currentThermalStatus`，
达到 `Moderate` 及以上即 `stop(ThermalProtection)`。

- 监测**不在时序线程上**：`currentThermalStatus` 是一次跨进程调用，
  放进 `URGENT_AUDIO` 的事件循环会直接变成点划边沿抖动。
- 达到阈值时**中止而不是降速**：发送中途改变单位时长会让接收端已建立的节奏基准失效，
  比直接停下更糟。
- 空闲时零唤醒、零系统调用。

**外部抢占**：`TorchManagerController` 注册 `TorchCallback`，当灯被其他应用关掉
而本应用仍期望它亮着时，向 `externalTakeover` 发射信号。`MorseTransmitter` 订阅该流，
在 LED 通道发送过程中收到即 `stop(ExternalTakeover)`。
没有这条链路，灯已被相机拿走，界面却还在推进进度条。

### §4.3 闪光灯探测

遍历 `cameraIdList`，选首个「后置 + 有闪光灯」的相机，**绝不硬编码 `"0"`**；
找不到后置则退到任意带闪光灯的相机。探测结果在进程启动时缓存一次。

### §4.5 手动发报的触觉反馈

手动模式靠手指计时，视觉反馈存在延迟，因此在按下瞬间给出一次触觉节拍
（`HapticFeedbackType.LongPress`），让用户在不看屏幕时也能确认按键已生效。
自动发报不需要——它的确认信号是光本身。

### §4.6 隐私与免责

报文、短语、历史全部保存在本机（DataStore + JSON 文件），不采集、不上报、不联网。

免责说明放在「关于」页与「求生与求救指南」页，**不在发送路径上设确认弹窗**：
紧急场景下每多一次确认就多一次失败机会，而「滥用求救信号可能承担法律责任」
这一点已在两处说明页写明。

---

## 5. 模块分层与并发模型

### §5.1 依赖方向严格单向

```
core/model        纯 Kotlin，零 Android 依赖：建模与不变量
core/morse        纯 Kotlin，零 Android 依赖：编解码 + 时间轴编译
core/torch        Android：发光通道、时序调度、热监测
core/data         Android：DataStore 设置、JSON 短语与历史
core/designsystem Android：主题、图标、MorseStream、光斑覆盖层
feature/*         Android：五个页面模块
app               装配：DI 容器、导航、全局覆盖层
```

`core/model` 与 `core/morse` 保持纯 JVM，是为了让编解码与时序**能在 JVM 上直接跑单测**，
不需要模拟器或 Robolectric。这条约束不能破：任何 Android 类型泄漏进去，
测试链路就会退化为 instrumented test。

### §5.3 时间模型

**时钟**：必须基于 `SystemClock.elapsedRealtimeNanos()`，它包含深度休眠时间；
`System.nanoTime()` 在息屏休眠时停摆，会让时间轴彻底错位。
`HighResolutionClock` 抽象让 JVM 测试可以注入假时钟。

**符号元素**：点 1t、划 3t、元素间隔 1t、字符间隔 3t、单词间隔 7t（ITU-R M.1677-1）。
`MorseSymbol` 把「信号」与「间隔」统一为一类可枚举单元，这样渲染层可以等宽排布，
进度能精确对应到像素。

### §5.4 调度器

`MorseTransmitter` 在独立 `HandlerThread`（`THREAD_PRIORITY_URGENT_AUDIO`）上运行，
不占用协程调度器。等待策略是「粗睡 + 自旋命中」：距目标 > 4 ms 时 `parkNanos`
（提前 2 ms 唤醒），最后 4 ms 用 `Thread.yield()` 自旋。纯 `parkNanos` 会因调度器提前
唤醒产生毫秒级偏差，纯自旋则在长间隔上浪费 CPU。

通道对调度器透明：它只认「在 t 时刻把灯设为 on/off」，LED 与屏幕由 `TorchController`
的具体实现决定，因此两者可以随时互换而不影响时序层。

### §5.6 UI 状态契约

所有页面 Composable 只接收 `State` + 回调，不直接引用 ViewModel。
这条约束让 `@Preview` 不需要真实依赖即可渲染整页，也让状态可以单测。

---

## 6. 时序正确性

### §6.1 通道特性与速度边界

| 通道 | 延迟 | 可用性 | 适用场景 |
|---|---|---|---|
| LED（`setTorchMode`） | 30–300 ms（机型差异极大） | 取决于硬件 | 远距离、抗环境光 |
| 屏幕（全屏白闪） | ≤ 16 ms（受帧率约束） | 100% | 高速、LED 不可用 |

**15 WPM 是 LED 的可靠边界**：单位时长 < 80 ms 后，HAL 的点亮/熄灭延迟会吃掉点划比例。
`ChannelPreference.Auto` 在此之上自动切到屏幕通道，并在界面上说明原因。

屏幕通道**不参与调光**：它的唯一优势是「最亮、最容易被看见」，调暗只会削弱这一点。
因此亮度设置只作用于 LED。

屏幕通道的亮灭状态**只在绘制阶段读取**（`drawBehind` 内读快照状态）。
若在组合期收集，每次通断都会重组整棵页面树，主线程一掉帧灯的边沿就错乱——
表现为「乱闪」。

### §6.2 绝对时间轴

`ScriptCompiler` 把 `MorseCode` + `TimingProfile` 编译为 `SendScript`：
每个灯态翻转事件都携带**相对锚点的绝对偏移**，而不是逐元素累加的延迟。

这是整个时序层最重要的决定：逐条 `delay()` 累积时，每次抖动的误差都会向后传递，
10 WPM 下几百个符号就能累积到可感知的错位；绝对偏移则让单次抖动就地消化，
累积误差不随时间增长。

`compensationMillis`（L_cmd）只**整体平移锚点**，不改变任何单元时长，
因此补偿后点划比例与总时长指标都不受影响。

### §6.4 播放中止语义

`AbortReason` 必须穷尽，且每个值都要有真实产生点：

| 原因 | 产生点 |
|---|---|
| `UserStopped` | 用户点按停止 |
| `ThermalProtection` | 热监测协程达到 `Moderate` |
| `ExternalTakeover` | `TorchCallback` 报告灯被外部关闭 |
| `TorchUnavailable` | 下发的通道不可用 |
| `Error` | 保留，当前未使用 |

`PlaybackState` 互斥且穷尽，避免「正在发报又显示暂停」这类非法态。
暂停功能已从 UI 移除（对光信号来说暂停几秒等于乱码），模型层保留 `Paused` 以备后用。

---

## 7. 界面

### §7.3 主题与设计体系

全局锁定深色，不读系统设置、不提供切换、不启用动态取色。
理由不是审美而是光学约束：夜间场景下一次亮屏即可破坏双方 20–30 分钟的暗适应。

`core:designsystem` 以 `api` 而非 `implementation` 暴露 Miuix 与 Compose 依赖，
因为公开 Composable 的签名会直接出现这些类型。

### §7.5 发报页布局

自上而下：报文与符号流 → 大发送卡 → 一键操作。

- **发送卡是唯一视觉焦点**，固定高度 340 dp（横屏 200 dp），不随键盘或窗口变化。
- 整页可滚动，输入法弹出时只收缩滚动视口，组件尺寸与相对位置保持不变。
  这是安卓里唯一能同时满足「不挤压 / 不隐藏 / 布局稳定」的做法。
- **进度环按帧外推**：调度器只在灯态翻转时发布状态，5 WPM 下一个采样间隔长达 240 ms，
  直接用采样值画环会一格一格地跳。UI 以事件携带的 `elapsedMs` 为锚点、
  用单调时钟按帧外推，并 clamp 在当前单元区间内。
- 速度只在设置页调节，发报页不重复提供入口。

---

## 11. 附录

### §11.1 时序换算

`t = 1200 / WPM`（毫秒，PARIS 标准）。因此 10 WPM → 120 ms，
一个标准单词 `PARIS ` 占 50t = 6 s。SOS 为 27t，10 WPM 下 3240 ms。

### §11.2 prosign

已移除，见 §3.2。

### §11.3 通道枚举

`TransmissionChannel` 是**物理通道**（`Led` / `Screen`），`ChannelPreference` 是**用户偏好**
（`Auto` / `Led` / `Screen`）。二者不可合并：偏好 `Auto` 会在不同速度下解析成不同通道，
把两者混为一谈会让「用户选了什么」和「实际用了什么」无法区分。

---

## 遗留决策：为什么没有引入 convention plugin

5 个 `feature/*` 模块的 `build.gradle.kts` 有约 20 行重复配置。
抽 `build-logic` convention plugin 能省下约 120 行，但当前构建链是
AGP 9.4 + Gradle 9.6，`compileSdk { version = release(37) }` 属于较新的 DSL，
在 convention plugin 内的写法尚未稳定。

在「省 120 行」与「构建脚本可能失效」之间，当前选择保留重复。
若将来构建链稳定或模块数量显著增加，再重新评估。
