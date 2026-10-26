package com.flashmorse.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.SmallTitle

/**
 * 分组标题。
 *
 * Miuix 的分组语汇是「`SmallTitle` 放在 `Card` 外部」，标题自身带左右 28 dp 内边距；
 * 这里只补一个可选的右侧动作槽，避免每个页面重复写 Row。
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            SmallTitle(text = text)
        }
        if (trailing != null) {
            Box(modifier = Modifier.padding(end = 28.dp)) { trailing() }
        }
    }
}
