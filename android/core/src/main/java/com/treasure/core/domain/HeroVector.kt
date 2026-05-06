package com.treasure.core.domain

/**
 * Identifier for a bundled hero illustration. The :app module maps this
 * to a concrete @Composable that paints the museum-line drawing.
 *
 * Cycle 0001 keeps a small, deliberately generic set; per-model variants
 * (e.g. RACKET_VT_ZF2 vs RACKET_ASTROX_99) come later.
 */
enum class HeroVector {
    RACKET,
    SHOES,
    CAMERA_DSLR,
    CAMERA_RANGEFINDER,
    LENS_PRIME,
    TRIPOD,
    CAR_SEDAN,
    CAR_SUV,
    LAPTOP,
    TABLET,
    EARBUDS,
    KINDLE,
    WATCH,
    GENERIC,
}
