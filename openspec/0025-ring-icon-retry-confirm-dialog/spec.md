# Cycle 0025 · spec

## 1. 戒指图标 v3

Vector：
- viewport 108×108
- 外椭圆 rx=28, ry=10 (cycle 0024 是 24×14；现在 ry/rx=0.36 远比之前的 0.58 更扁)
- 内孔椭圆 rx=13, ry=4, cy=50 (外圈 cy=55 → 内孔上偏 5)
- 主体单一 evenOdd path：外椭圆 - 内椭圆 = 环面，金色 5-stop 渐变 #FAE9A8 → #22150A，方向**纯垂直** (startY=45, endY=65)
- 删掉 cycle 0024 的"前侧壁渐变带"第二个 path（误导观感）
- 删掉顶面 rune 雕刻（launcher 小尺寸下变污点）
- 装饰只留 4 条 stroke：
  - 左上高光：M28,53 a26,9 0 0,1 26,-9，strokeWidth 1.1，#FCEEC0
  - 右下暗弧：M80,57 a26,9 0 0,1 -26,9，strokeWidth 1.1，#110804
  - 内孔顶半暗线：M42,49.5 a12,3.5 0 0,1 24,0，strokeWidth 0.85
  - 内孔底半亮金线：M42.5,50.5 a11.5,3.5 0 0,0 23,0，strokeWidth 0.7
- 投影：M54,80 m-28,0 a28,3.5 0 1,0 56,0，fill #28000000，更长更扁配俯视角度

## 2. 确认收入二次确认

`AddPreview`：

```kotlin
var confirming by remember { mutableStateOf(false) }

// 头部 [确认收入] 按钮 onClick → confirming = true（不再直接 onConfirm）

if (confirming) {
    AlertDialog(
        onDismissRequest = { confirming = false },
        title = { Text("收入 ${draft.brand} ${draft.model}？") },
        text = { Text("确认后会作为一件 ${category.nameZh} 入图鉴，再改就要从图鉴里点进去编辑。") },
        confirmButton = { TextButton(onClick = { confirming = false; onConfirm(status) }) {
            Text("收入", color = colors.terra)
        }},
        dismissButton = { TextButton(onClick = { confirming = false }) {
            Text("取消")
        }},
        containerColor = colors.paper,
        titleContentColor = colors.ink,
        textContentColor = colors.sub,
    )
}
```

样式沿用 SettingsScreen 重置设置 dialog（cycle 0017）的 paper 背景 + terra 主按钮。

标题里 `{Brand Model}` 拼出来空白时回退 "这件 {品类}"。

## 3. Out of scope

- 真渲染 PNG 戒指图标
- 其它草稿页操作的二次确认
- cycle 0024 已记的死代码清理 / 撤销采用 / WebView headless
