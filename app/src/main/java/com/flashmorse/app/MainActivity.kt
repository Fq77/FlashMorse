package com.flashmorse.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.flashmorse.app.navigation.FlashMorseApp
import com.flashmorse.core.designsystem.theme.FlashMorseTheme

class MainActivity : ComponentActivity() {

    /**
     * 来自快捷方式的待处理动作。
     *
     * 用 Compose 状态而不是 Flow：它只被一棵组合树读取，
     * 并且需要在 [onNewIntent] 之后立刻驱动重组。
     */
    private var pendingShortcut by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingShortcut = intent.shortcutAction()

        val container = (application as FlashMorseApplication).container
        setContent {
            FlashMorseTheme {
                FlashMorseApp(
                    container = container,
                    sosRequested = pendingShortcut == SHORTCUT_SOS,
                    onSosRequestHandled = { pendingShortcut = null },
                )
            }
        }
    }

    /** 应用已在栈中时从快捷方式再次进入走这里（配合清单里的 singleTop）。 */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingShortcut = intent.shortcutAction()
    }
}

private const val SHORTCUT_SOS = "sos"

/** 与 `res/xml/shortcuts.xml` 里的 extra name 保持一致。 */
private const val EXTRA_SHORTCUT = "com.flashmorse.app.extra.SHORTCUT"

private fun Intent?.shortcutAction(): String? = this?.getStringExtra(EXTRA_SHORTCUT)
