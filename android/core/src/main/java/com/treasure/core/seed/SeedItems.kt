package com.treasure.core.seed

import com.treasure.core.domain.HeroSpec
import com.treasure.core.domain.HeroVector
import com.treasure.core.domain.HistoryEvent
import com.treasure.core.domain.HistoryKind
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus

/**
 * Cycle 0031 复修：从 8 条样例瘦身到每个内建分类一条共 6 条（badminton /
 * photo / cars / tech / coffee / wine），新装看着不至于太冷清，又不会塞满
 * 让用户清半天。用户随时能删。
 *
 * 历史包：原 8 条样例 (2× badminton/photo/cars/tech) 留作参考代码已下线 —
 * 当前 [all] 返回的就是种子全集。
 */
object SeedItems {
    private const val NOW: Long = 1746576000_000L // 2026-05-06 UTC midnight

    fun all(): List<Item> = listOf(
        // Badminton ───────────────────────────────────────────────────────
        Item(
            id = "racket-vt-zf2",
            category = "badminton",
            brand = "Yonex",
            model = "Voltric Z-Force II",
            nickname = "黑刃",
            acquired = "2023-04-12",
            parted = null,
            status = ItemStatus.OWNED,
            palette = listOf("#0e0e0e", "#c9362f", "#e8e2d4", "#5a5a5a"),
            oneLiner = "进攻型 4U · 拉 26 磅",
            heroVector = HeroVector.RACKET,
            specs = listOf(
                HeroSpec("重量", "4U / 83g"),
                HeroSpec("平衡点", "294mm"),
                HeroSpec("中杆", "硬"),
                HeroSpec("握把", "G5"),
                HeroSpec("型号", "Voltric Z-Force II"),
                HeroSpec("类型", "进攻型"),
                HeroSpec("拉线", "BG80 Power · 26lbs"),
                HeroSpec("购入价", "¥1,580"),
            ),
            history = listOf(
                HistoryEvent("2023-04-12", HistoryKind.ACQUIRED,  "购入",        "上海徐家汇 1580 入手二代"),
                HistoryEvent("2023-05-20", HistoryKind.MILESTONE, "第一场比赛",   "公司单打 8 强"),
                HistoryEvent("2023-09-03", HistoryKind.MAINTAIN,  "换线 BG80",   "26 磅，老师傅手工拉的"),
                HistoryEvent("2024-02-14", HistoryKind.MOD,       "换握把胶",     "AC102 黑色，手感更干"),
                HistoryEvent("2024-08-11", HistoryKind.MILESTONE, "杭州友谊赛",   "混双第三名"),
            ),
            photos = emptyList(),
            createdAt = NOW, updatedAt = NOW,
        ),

        // Photography ─────────────────────────────────────────────────────
        Item(
            id = "cam-fuji-xt5",
            category = "photo",
            brand = "Fujifilm",
            model = "X-T5",
            nickname = "小富士",
            acquired = "2023-02-14",
            parted = null,
            status = ItemStatus.OWNED,
            palette = listOf("#1a1a1a", "#3a3530", "#d8d2c4", "#8a8378"),
            oneLiner = "APS-C 旗舰 · 4020 万像素",
            heroVector = HeroVector.CAMERA_DSLR,
            specs = listOf(
                HeroSpec("传感器", "X-Trans CMOS 5"),
                HeroSpec("像素", "40.2 MP"),
                HeroSpec("机身防抖", "7 档 IBIS"),
                HeroSpec("快门寿命", "50 万次"),
                HeroSpec("型号", "X-T5 Black"),
                HeroSpec("处理器", "X-Processor 5"),
                HeroSpec("视频", "6.2K 30p / 4K 60p"),
                HeroSpec("购入价", "¥12,500（机身）"),
            ),
            history = listOf(
                HistoryEvent("2023-02-14", HistoryKind.ACQUIRED,  "机身购入",    "首发后等三个月才买到"),
                HistoryEvent("2023-03-20", HistoryKind.MOD,       "配 23mm F2",  "人文挂机镜头"),
                HistoryEvent("2023-08-15", HistoryKind.MILESTONE, "京都之行",    "拍了 1200 张，胶片模拟太香"),
                HistoryEvent("2025-09-08", HistoryKind.MILESTONE, "冰岛环线",    "极光夜，长曝光神器"),
            ),
            photos = emptyList(),
            createdAt = NOW, updatedAt = NOW,
        ),

        // Cars (rentals) ──────────────────────────────────────────────────
        Item(
            id = "car-911",
            category = "cars",
            brand = "Porsche",
            model = "911 Carrera S",
            nickname = "梦中情车",
            acquired = "2025-07-04",
            parted = "2025-07-06",
            status = ItemStatus.RENTED,
            palette = listOf("#dcd8d0", "#a8a39a", "#1a1a1a", "#d97757"),
            oneLiner = "租赁 2 天 · 加州海岸",
            heroVector = HeroVector.CAR_SEDAN,
            specs = listOf(
                HeroSpec("动力", "F6 3.0T 双涡轮"),
                HeroSpec("马力", "450 PS"),
                HeroSpec("0-100", "3.7 s"),
                HeroSpec("配置", "PDK 8 速"),
                HeroSpec("车型", "911 Carrera S 992.1"),
                HeroSpec("颜色", "Crayon Grey"),
                HeroSpec("租赁公司", "Hertz Dream Cars"),
                HeroSpec("路线", "SF → Big Sur → Carmel"),
            ),
            history = listOf(
                HistoryEvent("2025-07-04", HistoryKind.ACQUIRED,  "SFO 提车", "金门大桥日落开过"),
                HistoryEvent("2025-07-05", HistoryKind.MILESTONE, "1 号公路", "Big Sur 海岸 280 公里"),
                HistoryEvent("2025-07-06", HistoryKind.PARTED,    "还车",     "依依不舍"),
            ),
            photos = emptyList(),
            createdAt = NOW, updatedAt = NOW,
        ),

        // Tech ────────────────────────────────────────────────────────────
        Item(
            id = "tech-mbp",
            category = "tech",
            brand = "Apple",
            model = "MacBook Pro 14\" M4 Pro",
            nickname = "主力机",
            acquired = "2024-11-20",
            parted = null,
            status = ItemStatus.OWNED,
            palette = listOf("#3a3a3c", "#1a1a1c", "#d8d2c4", "#8a8378"),
            oneLiner = "工作机 · 24G / 1T",
            heroVector = HeroVector.LAPTOP,
            specs = listOf(
                HeroSpec("CPU", "M4 Pro 12 核"),
                HeroSpec("内存", "24 GB"),
                HeroSpec("存储", "1 TB"),
                HeroSpec("屏幕", "14.2\" XDR 120Hz"),
                HeroSpec("芯片", "Apple M4 Pro 12CPU / 16GPU"),
                HeroSpec("颜色", "Space Black"),
                HeroSpec("购入价", "¥18,499"),
            ),
            history = listOf(
                HistoryEvent("2024-11-20", HistoryKind.ACQUIRED, "购入",             "替换服役 4 年的 M1"),
                HistoryEvent("2025-02-10", HistoryKind.MOD,      "配 Studio Display", "外接 27 寸屏"),
            ),
            photos = emptyList(),
            createdAt = NOW, updatedAt = NOW,
        ),

        // Coffee ──────────────────────────────────────────────────────────
        Item(
            id = "coffee-mara-x",
            category = "coffee",
            brand = "Lelit",
            model = "MaraX V2",
            nickname = "马拉",
            acquired = "2024-03-15",
            parted = null,
            status = ItemStatus.OWNED,
            palette = listOf("#3a2a1a", "#8a5a3a", "#e8d8c4", "#1a1a1a"),
            oneLiner = "家用半自动 · E61 冲煮头",
            heroVector = HeroVector.ESPRESSO_MACHINE,
            specs = listOf(
                HeroSpec("冲煮头", "E61"),
                HeroSpec("锅炉", "热交换 1.8L"),
                HeroSpec("水箱", "2.5L"),
                HeroSpec("功率", "1400 W"),
                HeroSpec("型号", "MaraX V2"),
                HeroSpec("尺寸", "33 × 40 × 41 cm"),
                HeroSpec("购入价", "¥12,800"),
            ),
            history = listOf(
                HistoryEvent("2024-03-15", HistoryKind.ACQUIRED,  "购入",      "深圳老咖啡店出的二手"),
                HistoryEvent("2024-04-02", HistoryKind.MAINTAIN,  "换三通阀",   "首次保养，顺便除水垢"),
                HistoryEvent("2024-11-20", HistoryKind.MILESTONE, "学拉花",    "天鹅终于画得像样了"),
            ),
            photos = emptyList(),
            createdAt = NOW, updatedAt = NOW,
        ),

        // Wine / Spirits ──────────────────────────────────────────────────
        Item(
            id = "wine-chateau-margaux-2015",
            category = "wine",
            brand = "Château Margaux",
            model = "2015",
            nickname = "玛歌",
            acquired = "2024-12-25",
            parted = null,
            status = ItemStatus.OWNED,
            palette = listOf("#5a1a1a", "#3a0a0a", "#d4c4a0", "#1a1a1a"),
            oneLiner = "波尔多一级庄 · 2015 年份",
            heroVector = HeroVector.WINE_BOTTLE,
            specs = listOf(
                HeroSpec("产区", "波尔多 · 玛歌"),
                HeroSpec("年份", "2015"),
                HeroSpec("酒精度", "13.5%"),
                HeroSpec("容量", "750 ml"),
                HeroSpec("葡萄", "赤霞珠 90% · 美乐 6%"),
                HeroSpec("评分", "RP 99 · JS 99"),
                HeroSpec("购入价", "¥6,800"),
            ),
            history = listOf(
                HistoryEvent("2024-12-25", HistoryKind.ACQUIRED, "购入", "圣诞节给自己的礼物，等十年再开"),
            ),
            photos = emptyList(),
            createdAt = NOW, updatedAt = NOW,
        ),
    )
}
