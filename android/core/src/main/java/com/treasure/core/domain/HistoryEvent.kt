package com.treasure.core.domain

import kotlinx.serialization.Serializable

@Serializable
data class HistoryEvent(
    val date: String,           // YYYY-MM-DD
    val kind: HistoryKind,
    val title: String,
    val note: String,
)

@Serializable
enum class HistoryKind {
    ACQUIRED,    // 购入
    MILESTONE,   // 比赛 / 旅行 / 重要时刻
    MAINTAIN,    // 维护 / 换线 / 清洁
    MOD,         // 改装 / 升级配件
    PARTED,      // 转让 / 还车
}
