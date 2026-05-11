package com.treasure.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.treasure.core.domain.Category
import com.treasure.ui.add.AddRoute
import com.treasure.ui.components.ControlIsland
import com.treasure.ui.components.IslandTab
import com.treasure.ui.grid.GridRoute
import com.treasure.ui.portal.PortalRoute
import com.treasure.ui.settings.SettingsRoute
import kotlinx.coroutines.launch

private const val PAGE_PORTAL = 0
private const val PAGE_GRID = 1
private const val PAGE_ADD = 2
private const val PAGE_SETTINGS = 3
private const val PAGE_COUNT = 4

/**
 * 四 Tab 横滑主屏：门厅 / 图鉴 / 录入 / 设置 都在 HorizontalPager 里，
 * Detail / Edit 仍由 NavHost push 出来覆盖。
 *
 * 控制岛点击 → animateScrollToPage；左右滑 → 控制岛跟着高亮。
 * 门厅点品类 / “去设置” 横幅 / Add 配置 AI 这类内部跳转，全部走
 * 同一个 pagerState。
 */
@Composable
fun MainScreen(
    onOpenDetail: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (String) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    var gridCategoryId by rememberSaveable { mutableStateOf(Category.PHOTO.id) }
    // Cycle 0028：分类管理抽屉提到 MainScreen 顶层 — 既能从 Grid 右上小红点
    // 打开，也能从 Portal 空态引导打开（"去分类管理 →"）。
    var categoryManagerOpen by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }

    // Cycle 0029：BackHandler — 非 Portal tab 时返回回首页，Portal 才走默认
    // （= 退出应用）。Manager 抽屉打开时优先收抽屉，与 Compose 系统 back
    // 双重保险（ModalBottomSheet 自身也会响应 back，但用 androidx 版本经常
    // 不收 — 显式拦下更稳）。
    androidx.activity.compose.BackHandler(
        enabled = categoryManagerOpen || pagerState.currentPage != PAGE_PORTAL,
    ) {
        when {
            categoryManagerOpen -> categoryManagerOpen = false
            pagerState.currentPage != PAGE_PORTAL ->
                scope.launch { pagerState.animateScrollToPage(PAGE_PORTAL) }
        }
    }

    // Cycle 0019：监听 shareIntake — 从京东 / 淘宝 / 浏览器分享过来的文字
    // 一旦到，就先切到录入 tab，让 AddRoute 那边的 LaunchedEffect 派给 VM。
    val app = androidx.compose.ui.platform.LocalContext.current
        .applicationContext as com.treasure.TreasureApp
    androidx.compose.runtime.LaunchedEffect(Unit) {
        app.shareIntake.collect { text ->
            if (!text.isNullOrBlank()) {
                pagerState.animateScrollToPage(PAGE_ADD)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // 默认 beyondViewportPageCount = 0：只渲染当前页。Cycle 0013 把
            // 之前显式置 1 改回默认 — 保持邻接页在内存里的代价是每次 swipe 闲
            // 时都在 LazyColumn / LazyVerticalGrid 上跑测量；6 张 doorway +
            // 整个 Grid 同时常驻会拖滚动。让它 lazy。
        ) { page ->
            when (page) {
                PAGE_PORTAL -> PortalRoute(
                    onEnterCategory = { id ->
                        gridCategoryId = id
                        scope.launch { pagerState.animateScrollToPage(PAGE_GRID) }
                    },
                    onOpenItem = onOpenDetail,
                    onOpenCategoryManager = { categoryManagerOpen = true },
                )
                PAGE_GRID -> GridRoute(
                    initialCategoryId = gridCategoryId,
                    // Cycle 0019：chip 点击要回写到 MainScreen 的 saved state，
                    // 否则从 Detail 返回后 GridRoute 重组，LaunchedEffect 会按
                    // gridCategoryId 强行覆盖 chip 的选择。
                    onCategoryChanged = { id -> gridCategoryId = id },
                    onBack = {
                        scope.launch { pagerState.animateScrollToPage(PAGE_PORTAL) }
                    },
                    onOpenItem = onOpenDetail,
                    onOpenCategoryManager = { categoryManagerOpen = true },
                    onOpenSearch = onOpenSearch,
                )
                PAGE_ADD -> AddRoute(
                    onSaved = onOpenDetail,
                    onGoSettings = {
                        scope.launch { pagerState.animateScrollToPage(PAGE_SETTINGS) }
                    },
                )
                PAGE_SETTINGS -> SettingsRoute()
            }
        }

        ControlIsland(
            selected = pageToTab(pagerState.currentPage),
            onSelect = { tab ->
                scope.launch { pagerState.animateScrollToPage(tabToPage(tab)) }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
        )

        // Cycle 0028：分类管理抽屉提到 MainScreen 顶层 — 入口可以是 Grid 右上
        // 小红点（cycle 0026），也可以是 Portal 空态 "去分类管理" 链接（cycle
        // 0028）。两边都用同一份抽屉、同一个 VM。Cycle 0029：抽屉只剩 List，
        // 编辑 / 新建都跳全屏路由（onAddCategory / onEditCategory）。
        if (categoryManagerOpen) {
            com.treasure.ui.category.CategoryManager(
                onClose = { categoryManagerOpen = false },
                onAddCategory = onAddCategory,
                onEditCategory = { info -> onEditCategory(info.id) },
            )
        }
    }
}

private fun pageToTab(page: Int): IslandTab = when (page) {
    PAGE_PORTAL -> IslandTab.Portal
    PAGE_GRID -> IslandTab.Grid
    PAGE_ADD -> IslandTab.Add
    PAGE_SETTINGS -> IslandTab.Settings
    else -> IslandTab.Portal
}

private fun tabToPage(tab: IslandTab): Int = when (tab) {
    IslandTab.Portal -> PAGE_PORTAL
    IslandTab.Grid -> PAGE_GRID
    IslandTab.Add -> PAGE_ADD
    IslandTab.Settings -> PAGE_SETTINGS
}
