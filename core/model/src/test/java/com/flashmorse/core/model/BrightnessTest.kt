package com.flashmorse.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 亮度模型测试。
 *
 * 这里守的是三条对外承诺：百分比有下限、档位端点严格对齐、映射单调。
 * 任何一条被破坏，用户看到的现象都是"滑杆滑了但没什么变化"或"最暗档还是刺眼"。
 */
class BrightnessTest {

    @Test
    fun `percent is clamped to the usable range`() {
        assertEquals(Brightness.MIN_PERCENT, Brightness.clampPercent(0))
        assertEquals(Brightness.MIN_PERCENT, Brightness.clampPercent(-20))
        assertEquals(Brightness.MAX_PERCENT, Brightness.clampPercent(140))
        assertEquals(65, Brightness.clampPercent(65))
    }

    @Test
    fun `fraction and percent round trip`() {
        for (percent in Brightness.MIN_PERCENT..Brightness.MAX_PERCENT) {
            assertEquals(percent, Brightness.fractionToPercent(Brightness.percentToFraction(percent)))
        }
    }

    @Test
    fun `level endpoints are exact`() {
        assertEquals(1, Brightness.levelFor(0f, maxLevel = 100))
        assertEquals(100, Brightness.levelFor(1f, maxLevel = 100))
    }

    @Test
    fun `level is monotonic and never out of range`() {
        var previous = 0
        for (step in 0..1000) {
            val level = Brightness.levelFor(step / 1000f, maxLevel = 100)
            assertTrue("level decreased at $step", level >= previous)
            assertTrue("level out of range at $step: $level", level in 1..100)
            previous = level
        }
    }

    @Test
    fun `gamma curve keeps the middle of the slider well below the middle of the range`() {
        // 线性映射会让 50% 落在第 50 档，观感上"前半程几乎没变暗"。
        // 走 γ=2.2 后应当明显靠下，同时不至于暗到不可用。
        val middle = Brightness.levelFor(0.5f, maxLevel = 100)
        assertTrue("middle level too high: $middle", middle < 30)
        assertTrue("middle level too low: $middle", middle >= 15)
    }

    @Test
    fun `device with a single level degrades to level one`() {
        assertEquals(1, Brightness.levelFor(0.3f, maxLevel = 1))
        assertEquals(1, Brightness.levelFor(0.9f, maxLevel = 0))
    }

    @Test
    fun `out of range fractions are clamped instead of overflowing`() {
        assertEquals(1, Brightness.levelFor(-1f, maxLevel = 100))
        assertEquals(100, Brightness.levelFor(2f, maxLevel = 100))
    }
}
