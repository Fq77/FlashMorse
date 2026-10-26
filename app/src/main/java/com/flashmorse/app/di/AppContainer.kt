package com.flashmorse.app.di

import android.content.Context
import com.flashmorse.core.data.prefs.SettingsRepository
import com.flashmorse.core.data.repository.HistoryRepository
import com.flashmorse.core.data.repository.PhraseRepository
import com.flashmorse.core.torch.MorseTransmitter
import com.flashmorse.core.torch.ThermalGuard
import com.flashmorse.core.torch.impl.ScreenFlashController
import com.flashmorse.core.torch.impl.TorchManagerController

/**
 * 应用级依赖容器（手工装配）。
 *
 * 为什么不是 Hilt：本项目模块数量虽多，但依赖图极浅（仓储 + 单一调度器），
 * 引入注解处理器只会增加构建链路风险而不带来收益；这与规划文档「团队 < 3 人
 * 且模块多 → Hilt」的判断并不冲突，因为该判断的前提是依赖图复杂。
 *
 * 所有实例均为进程级单例：手电筒与调度器必须唯一，否则多通道会互相抢占。
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** 屏幕闪光通道：UI 通过它的 lit 状态绘制全屏光斑。 */
    val screenFlashController = ScreenFlashController()

    /** 设备发光能力，在进程启动时探测一次。 */
    val torchCapabilities = TorchManagerController.detect(appContext)

    /**
     * LED 通道。无闪光灯设备上为 null，上层自动退到屏幕通道。
     */
    val ledController: TorchManagerController? =
        if (torchCapabilities.hasFlash) TorchManagerController(appContext, torchCapabilities) else null

    val transmitter = MorseTransmitter(
        ledController = ledController,
        screenController = screenFlashController,
    )

    val thermalGuard = ThermalGuard(appContext)

    val settingsRepository = SettingsRepository(appContext)

    val phraseRepository = PhraseRepository(appContext)

    val historyRepository = HistoryRepository(appContext)
}
