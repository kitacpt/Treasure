package com.treasure.core.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.treasure.core.domain.Category
import com.treasure.core.domain.HeroSpec
import com.treasure.core.domain.HeroVector
import com.treasure.core.domain.HistoryEvent
import com.treasure.core.domain.Item
import com.treasure.core.domain.ItemStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "items")
internal data class ItemEntity(
    @PrimaryKey val id: String,
    val category: String,
    val brand: String,
    val model: String,
    val nickname: String,
    val acquired: String,
    val parted: String?,
    val status: String,
    @ColumnInfo(name = "palette_json") val paletteJson: String,
    @ColumnInfo(name = "one_liner") val oneLiner: String,
    @ColumnInfo(name = "hero_vector") val heroVector: String,
    @ColumnInfo(name = "hero_specs_json") val heroSpecsJson: String,
    @ColumnInfo(name = "specs_json") val specsJson: String,
    @ColumnInfo(name = "history_json") val historyJson: String,
    @ColumnInfo(name = "photos_json") val photosJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
) {
    fun toDomain(): Item = Item(
        id = id,
        category = Category.fromId(category),
        brand = brand,
        model = model,
        nickname = nickname,
        acquired = acquired,
        parted = parted,
        status = ItemStatus.valueOf(status),
        palette = JsonCodec.decodeStringList(paletteJson),
        oneLiner = oneLiner,
        heroVector = HeroVector.valueOf(heroVector),
        heroSpecs = JsonCodec.decodeHeroSpecs(heroSpecsJson),
        specs = JsonCodec.decodeStringMap(specsJson),
        history = JsonCodec.decodeHistory(historyJson),
        photos = JsonCodec.decodeStringList(photosJson),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    companion object {
        fun fromDomain(item: Item): ItemEntity = ItemEntity(
            id = item.id,
            category = item.category.id,
            brand = item.brand,
            model = item.model,
            nickname = item.nickname,
            acquired = item.acquired,
            parted = item.parted,
            status = item.status.name,
            paletteJson = JsonCodec.encodeStringList(item.palette),
            oneLiner = item.oneLiner,
            heroVector = item.heroVector.name,
            heroSpecsJson = JsonCodec.encodeHeroSpecs(item.heroSpecs),
            specsJson = JsonCodec.encodeStringMap(item.specs),
            historyJson = JsonCodec.encodeHistory(item.history),
            photosJson = JsonCodec.encodeStringList(item.photos),
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,
        )
    }
}

internal object JsonCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encodeStringList(value: List<String>): String = json.encodeToString(value)
    fun decodeStringList(text: String): List<String> =
        if (text.isBlank()) emptyList() else json.decodeFromString(text)

    fun encodeStringMap(value: Map<String, String>): String = json.encodeToString(value)
    fun decodeStringMap(text: String): Map<String, String> =
        if (text.isBlank()) emptyMap() else json.decodeFromString(text)

    fun encodeHeroSpecs(value: List<HeroSpec>): String = json.encodeToString(value)
    fun decodeHeroSpecs(text: String): List<HeroSpec> =
        if (text.isBlank()) emptyList() else json.decodeFromString(text)

    fun encodeHistory(value: List<HistoryEvent>): String = json.encodeToString(value)
    fun decodeHistory(text: String): List<HistoryEvent> =
        if (text.isBlank()) emptyList() else json.decodeFromString(text)
}
