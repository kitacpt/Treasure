package com.treasure.ui.nav

/**
 * Route catalogue. Stringly-typed because nav-compose's type-safe routes
 * (the `@Serializable` ones) are still evolving — keep it boring.
 *
 * Cycle 0010 折叠成只有 3 个：
 * - Main 是包含 Portal / Grid / Add / Settings 的横滑 Pager
 * - Detail / Edit 仍是 push 上来的覆盖屏
 */
object Routes {
    const val Main = "main"

    const val DetailPattern = "detail/{itemId}"
    fun detail(itemId: String) = "detail/$itemId"

    const val EditPattern = "edit/{itemId}"
    fun edit(itemId: String) = "edit/$itemId"
}
