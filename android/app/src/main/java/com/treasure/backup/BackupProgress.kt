package com.treasure.backup

/**
 * Cycle 0037：导出 / 导入流程对外的状态报告。UI 渲染：
 *  - Idle：抽屉首屏，两个大按钮（导出 / 导入）。
 *  - Running：进度条 + 阶段文字（"正在打包物品..." / "正在写入照片 12/240..."）。
 *  - Done：成功面板（"已导出 N 物品 / M 张照片 / 共 X MB" + [完成]）。
 *  - Failed：错误面板（红色文案 + [重试]，[关闭]）。
 *
 * [percent] 0..100；写文件期间按"已写字节 / 总字节"估算。Indeterminate 阶段
 * 用 [percent] = -1 表示。
 */
sealed interface BackupProgress {
    data object Idle : BackupProgress

    data class Running(
        val stage: String,
        val percent: Int = -1,
        val detail: String = "",
    ) : BackupProgress

    /** 导出成功 — 包含输出文件大小 + 物品 / 照片计数，UI 拼成"已导出"卡片。 */
    data class ExportDone(
        val sizeBytes: Long,
        val items: Int,
        val photoFiles: Int,
        val fileName: String,
    ) : BackupProgress

    /** 导入成功 — 同上。 */
    data class ImportDone(
        val items: Int,
        val photoFiles: Int,
    ) : BackupProgress

    /** 出错。[stage] 是出错时所处的阶段，[message] 是用户能看懂的原因。 */
    data class Failed(
        val stage: String,
        val message: String,
    ) : BackupProgress
}
