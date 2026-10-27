<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="FlashMorse Icon" width="108" height="108" style="border-radius: 24px;">

# 闪光摩斯 FlashMorse

> **把手机闪光灯变成一台可校准的摩斯电码发报机**

![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-API%2024%2B-3DDC84?logo=android&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.02-4285F4?logo=jetpackcompose&logoColor=white)
![Miuix](https://img.shields.io/badge/Miuix-0.9.4-FF6900)
![Version](https://img.shields.io/badge/version-1.1.0-38BDF8)
![License](https://img.shields.io/badge/License-MIT-38BDF8)

</div>

---

## 这是什么

当基站失联、屏幕没信号、喊话传不出五十米时，唯一还能穿透距离的载体是**光**。

**闪光摩斯 FlashMorse** 把手机闪光灯变成一台可校准的摩斯电码发报机：输入一句话，点按发送，灯就按 ITU-R M.1677-1 的时序把它发出去；对端只要看得见光，就能逐字读回来。全程离线，**不申请任何权限**——不需要 CAMERA，也不需要 INTERNET，装完就能用。

它同时解决摩斯发报最容易翻车的三件事：**时序精度**（硬件延迟、调度抖动、点划比例失真）、**通道选择**（LED 远但慢，屏幕快但弱）、**可读性**（对端要有节奏参照，自己要知道发到哪了）。

## ✨ 特性

- 🔦 **双发光通道** — 手电筒（`CameraManager.setTorchMode`，零权限、远距离）与全屏白闪（低延迟、100% 可用），自动模式按速度择优
- ⏱ **可校准时序** — 5–25 WPM 无级调速；指令补偿 ±60 ms 整体平移锚点，抵消机型 30–300 ms 的硬件点亮延迟
- 🎚 **手电筒亮度** — 10–100% 滑杆经 γ=2.2 感知补偿映射到硬件档位（API 33+ 读 HAL 档位逐档下发），低于 40% 提示仅建议近距离使用
- ✍️ **手动发报** — 按住即亮，松手出符号；2t 判定点划，3t 自动断字、7t 自动断词，边敲边解码
- 📚 **短语与历史** — 6 条内置应急短语开箱可用，支持行内改名/改报文、长按拖拽排序、一键直发；历史记录可单删或清空
- 🔤 **摩斯对照表** — 字母 / 数字 / 标点三组，每个字符可单独试闪，并显示点划数量与当前速度
- 🎯 **识读练习** — 灯光出题、四选一作答、10 题一轮；答完立即揭示点划形态，并按正确率给出下一步建议
- ⚡ **桌面快捷方式** — 长按图标直达「发 SOS」，省掉「打开应用 → 找到按钮」两步（首次仍需免责确认）
- 🧭 **求生与求救指南** — SOS、三次法则、遮挡法、对空对地手势，以及省电与留痕的野外要点
- 🌡 **热保护与中断检测** — 发送期间每 2 s 轮询系统热状态，达到「中度发热」即中止并说明原因；手电筒被其他应用抢占时立即中止并说明原因
- 🎵 **发报侧音** — 600 Hz 滴声与灯同步（可关），盲发时靠听觉核对节奏
- 🌙 **HyperOS 观感** — Miuix 深色设计体系，进度环按帧外推、屏幕闪光绘制期读态零重组

## 🛠 技术栈

| 分类 | 技术 |
|------|------|
| 语言 | Kotlin 2.4.10（JVM target 21） |
| UI | Jetpack Compose（BOM 2026.02.01）+ Miuix 0.9.4 设计体系 |
| 构建 | AGP 9.4.1 + Gradle 9.6 + Version Catalog |
| 架构 | 11 个模块分层，依赖方向严格单向 `core/* → feature/* → app` |
| 状态 | ViewModel + StateFlow，一次性提示走 `Channel` |
| 持久化 | DataStore Preferences + kotlinx.serialization JSON 文件 |
| 发光 | `CameraManager.setTorchMode`（零权限）/ 全屏白闪（StateFlow 驱动） |
| 时序 | 独立 HandlerThread（`THREAD_PRIORITY_URGENT_AUDIO`）+ 绝对时间轴 + 自旋命中 |
| 依赖注入 | 手工 `AppContainer`（依赖图浅，不引入注解处理器） |
| 测试 | JUnit4，35 个 JVM 单测（码表逐字比对、SOS 时长、点划比例、时间轴边界、编解码闭环、亮度分级） |

`core:model` 与 `core:morse` 是纯 Kotlin/JVM 模块，零 Android 依赖，因此时序与编解码逻辑可以直接在 JVM 上跑单测，不需要模拟器。

架构约束、时序模型的取舍与不可随意改动的设计决定，见 [`docs/architecture.md`](docs/architecture.md)。代码注释中的 `架构文档 §X.X` 均指向该文件。

## 🧠 时序引擎怎么工作

```
用户输入
  → TextNormalizer      全角转半角、中文标点映射、无法编码的字符降级为 "?"（绝不静默丢字）
  → MorseEncoder        ITU-R M.1677-1 码表（字母 / 数字 / 标点），无法编码的字符降级为 "?"
  → ScriptCompiler      编译为绝对时间轴：事件偏移 + 每个符号的 [start, end] 区间
  → MorseTransmitter    HandlerThread(URGENT_AUDIO) 上按锚点调度
        ├─ parkNanos 粗睡 → 最后 2 ms 自旋命中目标时刻
        ├─ 锚点整体前移 L_cmd（补偿"下发指令 → 真正发光"的延迟）
        └─ 逐事件下发：setTorchMode / 全屏白闪的 lit 状态
  → UI                  以 withFrameNanos 按帧外推进度，并 clamp 到当前单元区间
```

关键工程决策：

- **绝对时间轴，而不是逐条 `delay()` 累加**。每个事件都带相对锚点的绝对偏移，单次抖动不会向后传递，累积误差不随时间增长（10 WPM 下真机逐事件 `late=0`）。
- **通道对调度器透明**。同一份时间轴，"灯亮/灯灭"由 `TorchController` 的实现决定，LED 与屏幕可以随时互换而不影响时序层。
- **15 WPM 是 LED 的可靠边界**。单位时长 < 80 ms 后，HAL 的点亮/熄灭延迟会吃掉点划比例；自动模式在此之上切到屏幕通道，并明确告知原因。
- **补偿是整体平移，不是拉伸**。`L_cmd` 只挪锚点，不改点划时长，所以补偿后比例与总时长指标都不受影响。
- **屏幕闪光零重组**。`lit` 状态不再于组合期收集，而是下传对象、在 `drawBehind` 绘制期读取——每次通断不再重组整棵树，高速发报下不再出现频闪观感。

## 📁 项目结构

```
FlashMorse/
├── app/                        # 组装层：Application、MainActivity、导航图、手工 DI 容器
├── core/
│   ├── model/                  # 纯 Kotlin 模型：TimingProfile / MorseCode / SendScript / PlaybackState
│   ├── morse/                  # 编解码与时序：ITU 码表、TextNormalizer、ScriptCompiler
│   ├── torch/                  # 发光与调度：MorseTransmitter、LED / 屏幕控制器、热保护
│   ├── data/                   # DataStore 设置、短语库与历史（JSON 文件）
│   └── designsystem/           # Miuix 主题、图标别名层、光斑覆盖层、摩斯流组件
└── feature/
    ├── compose/                # 发报页：输入、发送卡、SOS、手电筒常亮
    ├── manual/                 # 手动发报：点按敲击 → 断字断词 → 解码
    ├── library/                # 短语与历史
    ├── learn/                  # 摩斯对照表 + 识读练习 + 求生与求救指南
    └── settings/               # 设置 / 速度 / 校准 / 诊断 / 关于
```

## 🚀 快速开始

### 环境要求

- Android Studio（较新版本，需支持 AGP 9.x）+ JDK 21
- Android SDK 24 (minSdk) — 37 (targetSdk / compileSdk)

### 构建

```bash
git clone https://github.com/Fq77/FlashMorse.git
cd FlashMorse
./gradlew installDebug     # 或直接用 Android Studio Run
```

debug 变体的 `applicationId` 带 `.debug` 后缀（`com.flashmorse.app.debug`），
因此调试版与正式版可以同时安装、互不覆盖，桌面上分别显示为「闪光摩斯 Debug」与「闪光摩斯」。

### 首次使用

1. 输入报文，点按发送卡即可发报；首次发 SOS 会弹出一次免责确认；
2. 设置页可调速度（5–25 WPM）、通道偏好、循环发送、发报侧音与震动反馈；
3. 若点划观感整体偏早/偏晚，进「时序补偿」以 5 ms 为步长微调，或直接试闪 SOS 校准；
4. 无闪光灯的设备会自动固定为屏幕闪光通道，无需额外设置；
5. 「对照」页顶部的练习卡可进入识读训练；长按桌面图标可直达「发 SOS」。

## 📌 版本

当前 **v1.1.0**（versionCode 2）：双通道发报、可校准时序、手动发报、短语与历史、对照表、识读练习、求生指南、热保护与中断检测、桌面快捷键。

## ⚠️ 免责声明

SOS 与灯光信号是**辅助手段**，不能替代专业救援设备与 110 / 112 / 12395 报警。请仅在真实紧急情况下使用求救信号；滥用求救信号可能承担法律责任。野外场景请优先保障电量与体温，信号发出后原地等待通常优于长途移动。

## 🙏 鸣谢

- 设计体系：[Miuix](https://github.com/miuix-kotlin-multiplatform/miuix) — HyperOS 风格 Compose 组件库
- 核心框架：[Kotlin](https://kotlinlang.org/) · [Jetpack Compose](https://developer.android.com/jetpack/compose) · [AndroidX Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle)
- 数据与序列化：[DataStore](https://developer.android.com/jetpack/androidx/releases/datastore) · [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
- 标准依据：ITU-R M.1677-1《International Morse code》

## 📄 License

[MIT](LICENSE) © Fq77

---

<p align="center">Made with ❤️ using Android & Jetpack Compose</p>
