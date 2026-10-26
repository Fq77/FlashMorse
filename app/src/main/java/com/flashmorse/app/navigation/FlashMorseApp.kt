package com.flashmorse.app.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flashmorse.app.BuildConfig
import com.flashmorse.app.di.AppContainer
import com.flashmorse.core.designsystem.component.ScreenFlashOverlay
import com.flashmorse.core.model.TransmissionChannel
import com.flashmorse.core.model.isActive
import com.flashmorse.feature.compose.SenderRoute
import com.flashmorse.feature.learn.LearnRoute
import com.flashmorse.feature.library.LibraryRoute
import com.flashmorse.feature.manual.ManualRoute
import com.flashmorse.feature.settings.AboutRoute
import com.flashmorse.feature.settings.CalibrationRoute
import com.flashmorse.feature.settings.DiagnosticsRoute
import com.flashmorse.feature.settings.SettingsRoute
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.nav.transition.navGraphicsTransition

/**
 * 应用装配层：只负责路由、转场与全局覆盖层。
 *
 * 顶栏/底栏归属各自页面（见 [HomePage] / [DetailPage]），这样可预测返回手势拖拽时，
 * 上下两层的内容与 chrome 会作为整体一起位移，不会出现「正文动了、顶栏没动」的割裂。
 */
@Composable
fun FlashMorseApp(container: AppContainer) {
    val backStack = rememberNavBackStack<Route>(Route.Home)

    var tabName by rememberSaveable { mutableStateOf(HomeTab.Sender.name) }
    val tab = HomeTab.entries.firstOrNull { it.name == tabName } ?: HomeTab.Sender

    // 提示统一走系统 Toast：跨页面、跨弹层都不会被页面层级遮挡
    val context = LocalContext.current
    val showMessage: (String) -> Unit = remember(context) {
        { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }

    val playback by container.transmitter.state.collectAsStateWithLifecycle()
    val activeChannel by container.transmitter.activeChannel.collectAsStateWithLifecycle()
    // 注意：这里故意不读取屏幕闪光的亮灭值本身（不用 by 委托），
    // 只把 State 对象下传给全屏覆盖层、在绘制阶段读取——
    // 灯的每次通断如果走到组合阶段，整棵页面树都会被重组，掉帧即乱闪。
    val screenLitState = container.screenFlashController.lit.collectAsStateWithLifecycle()

    // 发送期间禁止息屏：这是时序可靠的物理前提
    val view = LocalView.current
    DisposableEffect(playback.isActive) {
        view.keepScreenOn = playback.isActive
        onDispose { view.keepScreenOn = false }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            transition = AppTransition,
            effects = NavDisplayEffects.None,
            modifier = Modifier.fillMaxSize(),
        ) {
            entry<Route.Home> {
                HomePage(
                    title = tab.title,
                    selectedTab = tab,
                    onTabSelected = { tabName = it.name },
                    onOpenSettings = { backStack.add(Route.Settings) },
                    onOpenSurvival = { backStack.add(Route.Survival) },
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // 必须声明「这些 inset 已被消费」：页面内部还会用 imePadding()
                            // 给键盘让位。若不消费，Scaffold 已扣掉的状态栏/导航栏会被
                            // ime inset 再算一次，键盘上方就多出一条被 pad 掉的黑色空带。
                            .padding(padding)
                            .consumeWindowInsets(padding),
                    ) {
                        HomeContent(
                            container = container,
                            tab = tab,
                            onTabSelected = { tabName = it.name },
                            onMessage = showMessage,
                            onOpenManual = { backStack.add(Route.Manual) },
                        )
                    }
                }
            }

            entry<Route.Manual> {
                DetailPage(title = "手动发报", onBack = { backStack.removeLastOrNull() }) { padding ->
                    PageBody(padding) {
                        ManualRoute(
                            settingsRepository = container.settingsRepository,
                            transmitter = container.transmitter,
                            onMessage = showMessage,
                        )
                    }
                }
            }

            entry<Route.Settings> {
                DetailPage(title = "设置", onBack = { backStack.removeLastOrNull() }) { padding ->
                    PageBody(padding) {
                        SettingsRoute(
                            settingsRepository = container.settingsRepository,
                            transmitter = container.transmitter,
                            thermalGuard = container.thermalGuard,
                            appVersion = APP_VERSION,
                            onOpenCalibration = { backStack.add(Route.Calibration) },
                            onOpenDiagnostics = { backStack.add(Route.Diagnostics) },
                            onOpenAbout = { backStack.add(Route.About) },
                        )
                    }
                }
            }

            entry<Route.Calibration> {
                DetailPage(title = "时序补偿", onBack = { backStack.removeLastOrNull() }) { padding ->
                    PageBody(padding) {
                        CalibrationRoute(
                            settingsRepository = container.settingsRepository,
                            transmitter = container.transmitter,
                        )
                    }
                }
            }

            entry<Route.Diagnostics> {
                DetailPage(title = "设备诊断", onBack = { backStack.removeLastOrNull() }) { padding ->
                    PageBody(padding) {
                        DiagnosticsRoute(
                            settingsRepository = container.settingsRepository,
                            transmitter = container.transmitter,
                            thermalGuard = container.thermalGuard,
                        )
                    }
                }
            }

            entry<Route.Survival>(
                // 求生指南的入口是顶栏左侧的 Info 图标，页面也从左侧滑入，与入口方向一致
                transition = EntryFromLeft,
            ) {
                DetailPage(title = "求生与求救指南", onBack = { backStack.removeLastOrNull() }) { padding ->
                    PageBody(padding) {
                        com.flashmorse.feature.learn.SurvivalScreen(modifier = Modifier)
                    }
                }
            }

            entry<Route.About> {
                DetailPage(title = "关于闪光摩斯", onBack = { backStack.removeLastOrNull() }) { padding ->
                    PageBody(padding) {
                        AboutRoute(appVersion = APP_VERSION)
                    }
                }
            }
        }

        // 屏幕闪光通道的全屏光斑必须盖在所有页面之上，且自身可点按停止
        ScreenFlashOverlay(
            active = playback.isActive && activeChannel == TransmissionChannel.Screen,
            litState = screenLitState,
            onStop = { container.transmitter.stop() },
        )
    }
}

/** 二级页内容区：统一消费 Scaffold 的 innerPadding。 */
@Composable
private fun PageBody(
    padding: androidx.compose.foundation.layout.PaddingValues,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .consumeWindowInsets(padding),
    ) {
        content()
    }
}

/**
 * 页面转场：直接使用 Miuix 设计预置。
 *
 * 之前这里手写过 260 ms 的 Tween + FastOutSlowInEasing，那是拍脑袋的数值：
 * 库的设计速率由 `NavMotion.Default.programmatic` 定义（整步 500 ms，配套缓动），
 * 自行覆盖只会让切页节奏与同一套设计语言的其他动效脱节。
 * 几何（整页滑入 + 下层 1/4 视差 + 10% 透明度衰减）也一并交回预置。
 */
private val AppTransition: NavTransition = NavTransitions.MiuixDefault

/**
 * 顶栏左侧 Info 入口（求生指南）专用转场：镜射 [NavTransitions.MiuixDefault] 的水平方向——
 * 进入页从**左侧**全宽滑入，被覆盖层向尾缘（右）做 1/4 宽视差并带 10% 透明衰减。
 * 与库内预置一样只映射 graphicsLayer 属性并在绘制阶段读取深度，零重组开销。
 */
private val EntryFromLeft: NavTransition = navGraphicsTransition { scope ->
    val width = scope.layoutSize.width.toFloat()
    val d = scope.relativeDepth
    if (d <= 0f) {
        // 进入/离开顶层：从左缘全宽滑入（d: -1 -> 0，translationX: -width -> 0）
        translationX = (d.coerceIn(-1f, 0f) * width).roundToInt().toFloat()
    } else {
        // 被覆盖：向尾缘 1/4 宽视差 + 轻微透明衰减
        val cover = d.coerceIn(0f, 1f)
        translationX = cover * width * 0.25f
        alpha = 1f - 0.1f * cover
    }
}

@Composable
private fun HomeContent(
    container: AppContainer,
    tab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    onMessage: (String) -> Unit,
    onOpenManual: () -> Unit,
) {
    // 左右滑动切换页面：pager 与底部导航双向同步——
    // 点导航 → animateScrollToPage；滑页面 → snapshotFlow 回写 tab 状态。
    val pagerState = rememberPagerState(initialPage = tab.ordinal) { HomeTab.entries.size }

    // 双向同步用 settledPage（动画完全落定才更新）而不是 currentPage：
    // currentPage 在滑动中途就会跳到最近邻页，用它回写 tab 会让
    // LaunchedEffect(tab) 反向 animateScrollToPage，两个动画互相拉扯——
    // 表现为"点对照页结果卡在短语与对照之间"。
    LaunchedEffect(tab) {
        if (pagerState.settledPage != tab.ordinal) {
            pagerState.animateScrollToPage(tab.ordinal)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { page ->
                val next = HomeTab.entries.getOrNull(page) ?: return@collect
                if (next != tab) onTabSelected(next)
            }
    }

    HorizontalPager(
        state = pagerState,
        // 三页常驻组合：默认设置下相邻页要等手势进行到一半才被现场组合，
        // 整页 Scaffold + Canvas 的组合成本落在拖动帧上，表现为掉帧卡顿。
        // 页面总数只有 3，常驻的内存代价可以忽略；切页从此只剩纯位移动画。
        beyondViewportPageCount = HomeTab.entries.size - 1,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        when (HomeTab.entries.getOrNull(page)) {
            HomeTab.Sender -> SenderRoute(
                settingsRepository = container.settingsRepository,
                historyRepository = container.historyRepository,
                transmitter = container.transmitter,
                thermalGuard = container.thermalGuard,
                onOpenManual = onOpenManual,
                onMessage = onMessage,
            )

            HomeTab.Library -> LibraryRoute(
                phraseRepository = container.phraseRepository,
                historyRepository = container.historyRepository,
                settingsRepository = container.settingsRepository,
                transmitter = container.transmitter,
                onMessage = onMessage,
            )

            HomeTab.Learn -> LearnRoute(
                settingsRepository = container.settingsRepository,
                transmitter = container.transmitter,
                onMessage = onMessage,
            )

            else -> Unit
        }
    }
}

private val APP_VERSION: String = BuildConfig.VERSION_NAME
