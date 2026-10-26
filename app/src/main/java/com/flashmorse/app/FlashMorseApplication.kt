package com.flashmorse.app

import android.app.Application
import com.flashmorse.app.di.AppContainer
import com.flashmorse.core.model.Brightness
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FlashMorseApplication : Application() {

    lateinit var container: AppContainer
        private set

    /** 应用级作用域：只承载"把设置同步到硬件"这类进程级订阅。 */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // 亮度的唯一真源是 Settings：冷启动、设置页拖动、将来其它入口修改，都走这一条下发路径，
        // 避免"某个页面忘了同步"导致界面显示 30% 而实际满亮。
        appScope.launch {
            container.settingsRepository.settings.collect { settings ->
                container.transmitter.setBrightness(
                    Brightness.percentToFraction(settings.effectiveBrightnessPercent),
                )
            }
        }
    }
}
