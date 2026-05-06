package com.treasure.ui.nav

import com.treasure.core.domain.Category

/**
 * Route catalogue. Stringly-typed because nav-compose's type-safe routes
 * (the `@Serializable` ones) are still evolving — keep it boring.
 */
object Routes {
    const val Portal = "portal"
    const val GridPattern = "grid/{categoryId}"
    fun grid(categoryId: String) = "grid/$categoryId"
    fun grid(category: Category) = grid(category.id)

    const val DetailPattern = "detail/{itemId}"
    fun detail(itemId: String) = "detail/$itemId"

    const val Add = "add"
    const val Settings = "settings"
}
