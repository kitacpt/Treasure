package com.treasure.illust

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.treasure.core.domain.HeroVector
import com.treasure.core.domain.Item

/**
 * Dispatch an item to its hero illustration. Many enum values share a single
 * Composable for now — e.g. RACKET / SHOES split, but CAMERA_DSLR and
 * CAMERA_RANGEFINDER both render via [Camera] until we specialise. Anything
 * we haven't drawn yet falls through to [Generic].
 *
 * Cycle 0013：palette 包成 [IllustPalette]（@Immutable），让 Compose 在
 * Item 不变的情况下跳过整个 Canvas 重组。LazyColumn / LazyVerticalGrid
 * 滚动时帧时间立刻降下来。
 */
@Composable
fun HeroIllustration(item: Item?, modifier: Modifier = Modifier) {
    val palette = remember(item?.palette) {
        IllustPalette(
            item?.palette.orEmpty().mapNotNull { hex -> runCatching { parseHex(hex) }.getOrNull() },
        )
    }
    when (item?.heroVector) {
        HeroVector.RACKET             -> Racket(palette, modifier)
        HeroVector.SHOES              -> Shoes(palette, modifier)
        HeroVector.CAMERA_DSLR        -> Camera(palette, modifier)
        HeroVector.CAMERA_RANGEFINDER -> Camera(palette, modifier)
        HeroVector.LENS_PRIME         -> Lens(palette, modifier)
        HeroVector.TRIPOD             -> Tripod(palette, modifier)
        HeroVector.CAR_SEDAN          -> Car(palette, modifier)
        HeroVector.CAR_SUV            -> Car(palette, modifier)
        HeroVector.LAPTOP             -> Laptop(palette, modifier)
        HeroVector.TABLET             -> Tablet(palette, modifier)
        HeroVector.KINDLE             -> Tablet(palette, modifier)
        HeroVector.EARBUDS            -> Earbuds(palette, modifier)
        HeroVector.WATCH              -> Watch(palette, modifier)
        HeroVector.ESPRESSO_MACHINE   -> EspressoMachine(palette, modifier)
        HeroVector.COFFEE_GRINDER     -> CoffeeGrinder(palette, modifier)
        HeroVector.COFFEE_BEAN        -> CoffeeBean(palette, modifier)
        HeroVector.WINE_BOTTLE        -> WineBottle(palette, modifier)
        HeroVector.COCKTAIL_GLASS     -> CocktailGlass(palette, modifier)
        HeroVector.GENERIC            -> Generic(palette, modifier)
        null                          -> Generic(IllustPalette.Empty, modifier)
    }
}
