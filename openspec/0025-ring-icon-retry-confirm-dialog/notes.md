# Cycle 0025 · notes

## 文件改动

- `res/drawable/ic_launcher_foreground.xml` — 重写。删第二个"前侧壁带"path、删 rune、压扁椭圆、改纯垂直渐变
- `app/.../ui/add/AddPreview.kt`
  - 新 `var confirming by remember { mutableStateOf(false) }`
  - [确认收入] 按钮 onClick → `confirming = true`
  - 函数体末尾 `if (confirming)` 弹 `AlertDialog`

## 设计取舍

### 为什么第二个 path 让前一版"不像戒指"

cycle 0024 那版除了主 evenOdd 圆环 path，还加了一个独立的"前侧壁渐变带" path，本意是给前下缘加一条更暗的窄月牙模拟"侧面厚度"。问题是：

1. 它和主 path 几何上重叠（位于 outer ellipse 下半区内），但用更暗的渐变填充 — 看起来像主 path **下面**又叠了一个小一点的椭圆，给"两个不同物体重合"的错觉
2. 这条带子的边缘和主 path 的下边缘重合，但梯度方向不同 — 边界处颜色突变，破坏"单一连续金属表面"的视觉

干掉它之后整个戒指变回 ONE 圆环 + 上下渐变（顶亮底暗就是凸面的合理打光），简单得多也读得清。

### 椭圆压扁 0.58 → 0.36

0.58 的 ratio 意味着大约 cos⁻¹(0.58) ≈ 54° 的俯视角度。在 small icon (48-72dp) 显示时眼睛对 ellipse 的"扁度"不敏感，0.58 容易被脑补成"圆形稍微变形"。

0.36 ≈ cos⁻¹(0.36) ≈ 69° 的俯视角度 — 已经接近极扁，"这是俯视的椭圆"几乎不会误读。这种程度的透视感才能让用户一眼意识到"我看到的是从上方稍斜看下去的环形物体"。

### 为什么没有 PNG 真渲染

最稳的办法当然是去 Blender 渲染一个真 torus，按 launcher 各种密度导出 PNG 包进 mipmap-*dpi/。但：
1. 工程量大（要打开 Blender / Krita，设光照、调材质、五种密度各导一份）
2. 矢量适应深色 / 浅色 launcher 主题更好（vector 可以走 tintColor）
3. 在 108dp viewport 下，简洁矢量比写实 PNG 看起来更"图鉴感"，呼应博物馆线描风的整体调性

如果用户再说"还是不像戒指"，下一刀候选是：保留 vector 主体 + 加 PNG 备份 (mipmap-anydpi-v26 走 vector，老 launcher 看 PNG)。

### 确认收入 dialog 文案

仿 SettingsScreen 重置设置 dialog 风格：
- 标题简短问句 "收入 X？"
- text 描述后果 + 撤销难度（"再改就要从图鉴里点进去编辑"）
- confirm 用动词 "收入"（terra 红色，提醒"这是个写动作"）
- dismiss "取消"（中性 sub 灰）

不写 "你确认要 ... 吗？" 这种啰嗦句式。

### 标题回退："这件 {品类}"

`draft.brand` 和 `draft.model` 都空时，标题拼出来就是空格。回退到 "这件 ${category.nameZh}" 让标题不空、语义不假（用户至少选了品类才能到这一步，category 一定有）。

## 验证

### 编译

```
cd android && ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# BUILD SUCCESSFUL
```

APK：`android/app/build/outputs/apk/debug/app-debug.apk`（14 MB）

### 手测

1. 装 APK 上 vivo，看 launcher 图标：应该明显比 cycle 0024 更扁更"戒指"，不再像"几个圈叠"
2. 录入页跑 AI → 草稿 → [采用] → 进 Refine 页 → 点 [确认收入]：应该弹 dialog "收入 {品牌 型号}？" + [收入] [取消]
3. dialog 点 [取消] → 回 Refine 页，没保存
4. dialog 点 [收入] → 保存到图鉴 + 跳新对话（与 cycle 0024 一致）
5. 草稿里 brand / model 都空时点 [确认收入] → 标题应该是 "收入 这件 {品类}？"
