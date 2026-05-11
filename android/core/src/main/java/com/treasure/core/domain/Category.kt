package com.treasure.core.domain

enum class Category(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    /**
     * Cycle 0028：每个内建分类的"基础插画"。manager 编辑页里内建是锁定状
     * 态，Portal doorway 显示的也是这个图（之前 cycle 0026 种子 migration
     * 给 hero_vector 写的是 GENERIC，是个错；这里 enum 维护正确的默认）。
     */
    val defaultHeroVector: HeroVector,
) {
    BADMINTON("badminton", "羽毛球", "Badminton", HeroVector.RACKET),
    PHOTO("photo", "摄影", "Photography", HeroVector.CAMERA_DSLR),
    CARS("cars", "汽车", "Cars", HeroVector.CAR_SEDAN),
    TECH("tech", "电子产品", "Tech", HeroVector.LAPTOP),
    COFFEE("coffee", "咖啡", "Coffee", HeroVector.ESPRESSO_MACHINE),
    WINE("wine", "酒水", "Spirits", HeroVector.WINE_BOTTLE);

    companion object {
        fun fromId(id: String): Category =
            entries.firstOrNull { it.id == id } ?: TECH
    }
}
