package com.treasure.core.domain

enum class Category(val id: String, val nameZh: String, val nameEn: String) {
    BADMINTON("badminton", "羽毛球", "Badminton"),
    PHOTO("photo", "摄影", "Photography"),
    CARS("cars", "汽车", "Cars"),
    TECH("tech", "电子产品", "Tech");

    companion object {
        fun fromId(id: String): Category =
            entries.firstOrNull { it.id == id } ?: TECH
    }
}
