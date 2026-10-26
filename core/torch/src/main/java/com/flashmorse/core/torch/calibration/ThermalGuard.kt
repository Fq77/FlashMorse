package com.flashmorse.core.torch

import android.content.Context
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 热状态分级，映射自 [PowerManager.getCurrentThermalStatus]（API 29+）。 */
enum class ThermalLevel {
    None,
    Light,
    Moderate,
    Severe,
    Critical,
    Shutdown,
    Unknown,
    ;

    /** 达到该级别即建议降速与提示（规划文档 §4.2）。 */
    val requiresThrottle: Boolean
        get() = this == Moderate || this == Severe || this == Critical || this == Shutdown

    val displayName: String
        get() = when (this) {
            None -> "正常"
            Light -> "轻微升温"
            Moderate -> "中度发热"
            Severe -> "严重发热"
            Critical -> "临界过热"
            Shutdown -> "即将关断"
            Unknown -> "未知"
        }
}

/**
 * 热保护监听（规划文档 §4.2）。
 *
 * 只在发送期间轮询，空闲时零开销——不注册传感器、不运行定时器。
 */
class ThermalGuard(context: Context) {

    private val powerManager = context.applicationContext
        .getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val _level = MutableStateFlow(currentLevel())
    val level: StateFlow<ThermalLevel> = _level.asStateFlow()

    /** 由发送循环在每个循环边界调用，开销仅为一次系统属性读取。 */
    fun refresh(): ThermalLevel {
        val level = currentLevel()
        if (level != _level.value) _level.value = level
        return level
    }

    fun currentLevel(): ThermalLevel {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ThermalLevel.Unknown
        val manager = powerManager ?: return ThermalLevel.Unknown
        return when (manager.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalLevel.None
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalLevel.Light
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalLevel.Moderate
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalLevel.Severe
            PowerManager.THERMAL_STATUS_CRITICAL -> ThermalLevel.Critical
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalLevel.Shutdown
            else -> ThermalLevel.Unknown
        }
    }
}
