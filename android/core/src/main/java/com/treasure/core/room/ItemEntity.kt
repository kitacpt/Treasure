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
import com.treasure.core.domain.PhotoCallout
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
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
    @ColumnInfo(name = "specs_json") val specsJson: String,
    @ColumnInfo(name = "history_json") val historyJson: String,
    @ColumnInfo(name = "photos_json") val photosJson: String,
    /** Cycle 0010：照片文字标注 — JSON `Map<path, List<PhotoCallout>>`。空就是 "{}". */
    @ColumnInfo(name = "callouts_json", defaultValue = "{}") val calloutsJson: String,
    /** Cycle 0016：影集里被选作头像的那张照片 path，null = 用线描。 */
    @ColumnInfo(name = "avatar_photo_path") val avatarPhotoPath: String?,
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
        specs = JsonCodec.decodeHeroSpecs(specsJson),
        history = JsonCodec.decodeHistory(historyJson),
        photos = JsonCodec.decodeStringList(photosJson),
        callouts = JsonCodec.decodeCallouts(calloutsJson),
        avatarPhotoPath = avatarPhotoPath,
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
            specsJson = JsonCodec.encodeHeroSpecs(item.specs),
            historyJson = JsonCodec.encodeHistory(item.history),
            photosJson = JsonCodec.encodeStringList(item.photos),
            calloutsJson = JsonCodec.encodeCallouts(item.callouts),
            avatarPhotoPath = item.avatarPhotoPath,
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

    fun encodeHeroSpecs(value: List<HeroSpec>): String = json.encodeToString(value)
    fun decodeHeroSpecs(text: String): List<HeroSpec> =
        if (text.isBlank()) emptyList() else json.decodeFromString(text)

    fun encodeHistory(value: List<HistoryEvent>): String = json.encodeToString(value)
    fun decodeHistory(text: String): List<HistoryEvent> =
        if (text.isBlank()) emptyList() else json.decodeFromString(text)

    private val calloutsSerializer = MapSerializer(
        String.serializer(),
        ListSerializer(PhotoCallout.serializer()),
    )
    fun encodeCallouts(value: Map<String, List<PhotoCallout>>): String =
        json.encodeToString(calloutsSerializer, value)
    fun decodeCallouts(text: String): Map<String, List<PhotoCallout>> =
        if (text.isBlank()) emptyMap() else json.decodeFromString(calloutsSerializer, text)
}
