package com.treasure.core.seed

import com.treasure.core.domain.Category
import com.treasure.core.domain.HeroSpec
import com.treasure.core.domain.HeroVector
import com.treasure.core.domain.HistoryEvent
import com.treasure.core.domain.HistoryKind
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus

/**
 * First-launch seed data. Hand-picked subset ported from
 * prototype/project/data.jsx — covers all four categories, with history
 * timelines so the Detail drawer "历史" tab has real content.
 */
object SeedItems {
    private const val NOW: Long = 1746576000_000L // 2026-05-06 UTC midnight

    fun all(): List<Item> = listOf(
        // Badminton ───────────────────────────────────────────────────────
        Item(
            id = "racket-vt-zf2",
            category = Category.BADMINTON,
            brand = "Yonex",
            model = "Voltric Z-Force II",
            nickname = "黑刃",
            acquired = "2023-04-12",
            parted = null,
            status = ItemStatus.OWNED,
            palette = listOf("#0e0e0e", "#c9362f", "#e8e2d4", "#5a5a5a"),
            oneLiner = "进攻型 4U · 拉26磅",
            heroVector = HeroVector.RACKET,
            heroSpecs = listOf(
                HeroSpec("重量", "4U / 83g"),
                HeroSpec("平衡点", "294mm"),
                HeroSpec("中杆", "硬"),
                HeroSpec("握把", "G5"),
            ),
            specs = mapOf(
                "型号" to "Voltric Z-Force II",
                "类型" to "进攻型",
                "材质" to "高弹性碳素 + Nanometric DR",
                "拉线" to "BG80 Power · 26lbs",
                "购入价" to "¥1,580",
            ),
            history = listOf(
                HistoryEvent("2023-04-12", HistoryKind.ACQUIRED,  "购入",        "上海徐家汇 1580 入手二代"),
                HistoryEvent("2023-05-20", HistoryKind.MILESTONE, "第一场比赛",  "公司单打 8 强"),
                HistoryEvent("2023-09-03", HistoryKind.MAINTAIN,  "换线 BG80",  "26 磅，老师傅手工拉的"),
                HistoryEvent("2024-02-14", HistoryKind.MOD,       "换握把胶",   "AC102 黑色，手感更干"),
                HistoryEvent("2024-08-11", HistoryKind.MILESTONE, "杭州友谊赛", "混双第三名"),
                HistoryEvent("2025-11-02", HistoryKind.MAINTAIN,  "换线 BG65Ti","27 磅，更耐用"),
            ),
            createdAt = NOW, updatedAt = NOW,
        ),
        Item(
            id = "racket-astrox-99",
            category = Category.BADMINTON,
            brand = "Yonex",
            model = "Astrox 99 Pro",
            nickname = "橘子",
            acquired = "2024-09-21",
            parted = null,
            status = ItemStatus.OWNED,
            palette = listOf("#d97757", "#8a3a1f", "#f6f0e6", "#231e1a"),
            oneLiner = "进攻型 4U · 桃田款",
            heroVector = HeroVector.RACKET,
            heroSpecs = listOf(
                HeroSpec("重量", "4U / 83g"),
                HeroSpec("平衡点", "305mm"),
                HeroSpec("中杆", "极硬"),
                HeroSpec("握把", "G5"),
            ),
            specs = mapOf(
                "型号" to "Astrox 99 Pro",
                "配色" to "Cherry Sunburst",
                "拉线" to "BG66UM · 27lbs",
                "购入价" to "¥1,890",
            ),
            history = listOf(
                HistoryEvent("2024-09-21", HistoryKind.ACQUIRED,  "购入",        "看了桃田比赛太眼馋"),
                HistoryEvent("2024-10-08", HistoryKind.MAINTAIN,  "首次拉线",    "BG66UM 27 磅"),
                HistoryEvent("2025-03-15", HistoryKind.MILESTONE, "俱乐部联赛",  "团体赛第二名"),
            ),
            createdAt = NOW, updatedAt = NOW,
        ),

        // Photography ─────────────────────────────────────────────────────
        Item(
            id = "cam-fuji-xt5",
            category = Category.PHOTO,
            brand = "Fujifilm",
            model = "X-T5",
            nickname = "小富士",
            acquired = "2023-02-14",
            parted = null,
            status = ItemStatus.OWNED,
            palette = listOf("#1a1a1a", "#3a3530", "#d8d2c4", "#8a8378"),
            oneLiner = "APS-C 旗舰 · 4020 万像素",
            heroVector = HeroVector.CAMERA_DSLR,
            heroSpecs = listOf(
                HeroSpec("传感器", "X-Trans CMOS 5"),
                HeroSpec("像素", "40.2 MP"),
                HeroSpec("机身防抖", "7档 IBIS"),
                HeroSpec("快门寿命", "50万次"),
            ),
            specs = mapOf(
                "型号" to "X-T5 Black",
                "处理器" to "X-Processor 5",
                "视频" to "6.2K 30p / 4K 60p",
                "购入价" to "¥12,500（机身）",
            ),
            history = listOf(
                HistoryEvent("2023-02-14", HistoryKind.ACQUIRED,  "机身购入",    "首发后等三个月才买到"),
                HistoryEvent("2023-03-20", HistoryKind.MOD,       "配 23mm F2", "人文挂机镜头"),
                HistoryEvent("2023-08-15", HistoryKind.MILESTONE, "京都之行",    "拍了 1200 张，胶片模拟太香"),
                HistoryEvent("2024-05-01", HistoryKind.MOD,       "加 56mm F1.2", "拍人像专用"),
                HistoryEvent("2024-10-12", HistoryKind.MAINTAIN,  "传感器除尘",  "官方上海店清理"),
                HistoryEvent("2025-09-08", HistoryKind.MILESTONE, "冰岛环线",    "极光夜，长曝光神器"),
            ),
            createdAt = NOW, updatedAt = NOW,
        ),
        Item(
            id = "cam-leica-m6",
            category = Category.PHOTO,
            brand = "Leica",
            model = "M6",
            nickname = "老玩具",
            acquired = "2024-11-30",
            parted = null,
            status = ItemStatus.OWNED,
            palette = listOf("#2a2a2a", "#0a0a0a", "#d4cdb8", "#a89c7a"),
            oneLiner = "胶片旁轴 · 1984 年制",
            heroVector = HeroVector.CAMERA_RANGEFINDER,
            heroSpecs = listOf(
                HeroSpec("类型", "35mm 旁轴"),
                HeroSpec("快门", "1s - 1/1000s"),
                HeroSpec("取景器", "0.72x"),
                HeroSpec("生产年份", "1984"),
            ),
            specs = mapOf(
                "型号" to "Leica M6 (Classic)",
                "配镜" to "Summicron 50mm F2 v4",
                "产地" to "德国韦茨拉尔",
                "购入价" to "¥38,000（中古）",
            ),
            history = listOf(
                HistoryEvent("2024-11-30", HistoryKind.ACQUIRED,  "购入",        "生日礼物，给自己"),
                HistoryEvent("2025-01-05", HistoryKind.MILESTONE, "拍完第一卷", "Portra 400 · 北京胡同"),
                HistoryEvent("2025-04-10", HistoryKind.MAINTAIN,  "快门 CLA",   "北京 fix-it 老师傅，2400元"),
            ),
            createdAt = NOW, updatedAt = NOW,
        ),

        // Cars (rentals) ──────────────────────────────────────────────────
        Item(
            id = "car-911",
            category = Category.CARS,
            brand = "Porsche",
            model = "911 Carrera S",
            nickname = "梦中情车",
            acquired = "2025-07-04",
            parted = "2025-07-06",
            status = ItemStatus.RENTED,
            palette = listOf("#dcd8d0", "#a8a39a", "#1a1a1a", "#d97757"),
            oneLiner = "租赁 2 天 · 加州海岸",
            heroVector = HeroVector.CAR_SEDAN,
            heroSpecs = listOf(
                HeroSpec("动力", "F6 3.0T 双涡轮"),
                HeroSpec("马力", "450 PS"),
                HeroSpec("0-100", "3.7 s"),
                HeroSpec("配置", "PDK 8速"),
            ),
            specs = mapOf(
                "车型" to "911 Carrera S 992.1",
                "颜色" to "Crayon Grey",
                "租赁公司" to "Hertz Dream Cars",
                "里程" to "480 mi",
                "路线" to "SF → Big Sur → Carmel",
            ),
            history = listOf(
                HistoryEvent("2025-07-04", HistoryKind.ACQUIRED,  "SFO 提车",   "金门大桥日落开过"),
                HistoryEvent("2025-07-05", HistoryKind.MILESTONE, "1号公路",     "Big Sur 海岸 280 公里"),
                HistoryEvent("2025-07-06", HistoryKind.PARTED,    "还车",        "依依不舍"),
            ),
            createdAt = NOW, updatedAt = NOW,
        ),
        Item(
            id = "car-defender",
            category = Category.CARS,
            brand = "Land Rover",
            model = "Defender 110",
            nickname = "大方块",
            acquired = "2024-09-28",
            parted = "2024-10-04",
            status = ItemStatus.RENTED,
            palette = listOf("#3a4a3a", "#1a2a1a", "#c4b89c", "#d8d2c4"),
            oneLiner = "租赁 7 天 · 川西环线",
            heroVector = HeroVector.CAR_SUV,
            heroSpecs = listOf(
                HeroSpec("动力", "L6 3.0T 轻混"),
                HeroSpec("马力", "400 PS"),
                HeroSpec("驱动", "四驱"),
                HeroSpec("涉水", "900mm"),
            ),
            specs = mapOf(
                "车型" to "Defender 110 P400",
                "颜色" to "Pangea Green",
                "里程" to "2380 km",
                "路线" to "成都 → 稻城亚丁 → 新都桥",
            ),
            history = listOf(
                HistoryEvent("2024-09-28", HistoryKind.ACQUIRED,  "成都提车",    "满油 + 全险"),
                HistoryEvent("2024-09-30", HistoryKind.MILESTONE, "稻城亚丁",    "4500m 牛奶海"),
                HistoryEvent("2024-10-02", HistoryKind.MILESTONE, "新都桥",     "摄影天堂，拍了一整天"),
                HistoryEvent("2024-10-04", HistoryKind.PARTED,    "还车",        "右后门一道小划痕 800元"),
            ),
            createdAt = NOW, updatedAt = NOW,
        ),

        // Tech ────────────────────────────────────────────────────────────
        Item(
            id = "tech-mbp",
            category = Category.TECH,
            brand = "Apple",
            model = "MacBook Pro 14\" M4 Pro",
            nickname = "主力机",
            acquired = "2024-11-20",
            parted = null,
            status = ItemStatus.OWNED,
            palette = listOf("#3a3a3c", "#1a1a1c", "#d8d2c4", "#8a8378"),
            oneLiner = "工作机 · 24G/1T",
            heroVector = HeroVector.LAPTOP,
            heroSpecs = listOf(
                HeroSpec("CPU", "M4 Pro 12核"),
                HeroSpec("内存", "24 GB"),
                HeroSpec("存储", "1 TB"),
                HeroSpec("屏幕", "14.2\" XDR 120Hz"),
            ),
            specs = mapOf(
                "芯片" to "Apple M4 Pro 12CPU/16GPU",
                "颜色" to "Space Black",
                "购入价" to "¥18,499",
            ),
            history = listOf(
                HistoryEvent("2024-11-20", HistoryKind.ACQUIRED, "购入",            "替换服役 4 年的 M1"),
                HistoryEvent("2025-02-10", HistoryKind.MOD,      "配 Studio Display","外接 27 寸屏"),
            ),
            createdAt = NOW, updatedAt = NOW,
        ),
        Item(
            id = "tech-airpods",
            category = Category.TECH,
            brand = "Apple",
            model = "AirPods Pro 2",
            nickname = "耳朵",
            acquired = "2023-09-22",
            parted = null,
            status = ItemStatus.OWNED,
            palette = listOf("#f6f4ef", "#dcd8d0", "#1a1a1a", "#a8a39a"),
            oneLiner = "TWS 耳机 · USB-C",
            heroVector = HeroVector.EARBUDS,
            heroSpecs = listOf(
                HeroSpec("芯片", "H2"),
                HeroSpec("续航", "6h + 30h 盒"),
                HeroSpec("降噪", "主动降噪"),
                HeroSpec("接口", "USB-C"),
            ),
            specs = mapOf(
                "型号" to "AirPods Pro (2nd gen, USB-C)",
                "购入价" to "¥1,899",
            ),
            history = listOf(
                HistoryEvent("2023-09-22", HistoryKind.ACQUIRED, "购入", "首发当天去三里屯"),
                HistoryEvent("2024-08-04", HistoryKind.MAINTAIN, "换右耳", "充电仓里掉出来摔了，换右耳"),
            ),
            createdAt = NOW, updatedAt = NOW,
        ),
    )
}
