# Cycle 0013 · 验收

## 性能（用 layoutInspector / `adb shell dumpsys gfxinfo` 验证）

- LazyVerticalGrid（图鉴 tab）滚动同一组 ItemCard：滚回已经划过的位置时不应再触发 Canvas redraw（Compose 的 recomp counter 应该停留）
- HorizontalPager 切 tab 时只在切换瞬间布局新页面；不在前一页期间预制邻接页
- AddViewModel 的 recentConversations 单一 combine flow 链；HistoryDropdown 滚动应丝滑

## 插画

- 录入 / 编辑两屏的 HeroAvatarPicker 大圆 + 候选小圆都正常画出 line drawing
- Portal / Grid / Detail / DraftCta 各处 HeroIllustration 不变（之前就用 fillMaxSize，没受影响）

## 图标

- Adaptive icon foreground：环身外径 23 / 内径 17.5，环厚度 5.5
- 环身仍是 5 段 linear gradient（亮金 → 主金 → 深棕）
- 内 / 外缘高光 + 阴影弧、上下 rune、左右 tick、4 道 hairline 都按比例缩进
- 环内中心 **空** —— 不再有任何 terra dot / halo
- 环底 19dp 椭圆 cast shadow

## 编译

- `cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug` 全绿
- :core 现在依赖 compose-runtime（仅 annotation），不影响 release APK 体积
