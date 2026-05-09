package com.treasure.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.treasure.core.domain.Item
import com.treasure.illust.HeroIllustration

/**
 * 物品的 "头像" — 用户在 Edit / 手动录入页可以从影集里挑一张照片当头像；
 * 没挑就回退到 [HeroIllustration] 的线描。所有展示物品 hero 的位置（Portal /
 * Grid / Detail / DraftCta）都改用这个 dispatch composable，自动跟随用户的
 * 选择切换。
 */
@Composable
fun HeroAvatar(item: Item?, modifier: Modifier = Modifier) {
    val photoPath = item?.avatarPhotoPath
    if (item != null && !photoPath.isNullOrBlank()) {
        AsyncImage(
            model = photoPath,
            contentDescription = null,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } else {
        HeroIllustration(item = item, modifier = modifier)
    }
}
