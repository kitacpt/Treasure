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

    /** Cycle 0029：分类编辑改成全屏路由（之前在 Manager 抽屉里内嵌），跟物品
     *  编辑页同款风格。new 用专门的路由表示"新建模式"。 */
    const val CategoryNew = "category/new"
    const val CategoryEditPattern = "category/edit/{categoryId}"
    fun categoryEdit(categoryId: String) = "category/edit/$categoryId"

    /** Cycle 0029：图鉴页右上小搜索 icon → 全屏搜索。 */
    const val Search = "search"
}
