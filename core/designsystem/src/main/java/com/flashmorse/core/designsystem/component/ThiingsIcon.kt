package com.flashmorse.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flashmorse.core.designsystem.R
import androidx.compose.foundation.layout.Box

/**
 * thiings.co 的 3D 图标（https://www.thiings.co）。
 *
 * 这批位图自带烘焙好的深色背景，在深色主题上可以自然融合；位图无法 tint，
 * 所以「可用/熄灭」这类状态用 alpha 表达，而不是换色。
 */
private const val THIINGS_SOURCE = "https://www.thiings.co"

/** 手电筒（Flashlight）：顶栏常亮开关。 [lit] 点亮时完全不透明。 */
@Composable
fun ThiingsFlashlightIcon(
    lit: Boolean,
    modifier: Modifier = Modifier,
) {
    // 资源已做透明底处理（去除 thiings 原图的烘焙深色背景）并整体提亮，
    // 深色顶栏上可直接辨识；点亮/熄灭用 alpha 表达。
    Image(
        painter = painterResource(R.drawable.ic_thiings_flashlight),
        contentDescription = if (lit) "关闭手电筒" else "手电筒常亮",
        modifier = modifier.alpha(if (lit) 1f else 0.55f),
    )
}

/** 纸飞机（Paper Airplane）：发报页空状态的「点按发报」。 */
@Composable
fun ThiingsSendIcon(
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.ic_thiings_send),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(10.dp)),
    )
}
