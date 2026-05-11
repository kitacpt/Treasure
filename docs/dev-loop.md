# Dev Loop · 开发 → 验证全流程

新人 / 新一轮 agent 看完这一份，能从 0 起步把改动跑到手机上。

## 必备工具

| 工具 | 作用 | Linux 装法 |
|---|---|---|
| **JDK 17+** | Compose 编译要 JVM 17 | `sudo apt install openjdk-17-jdk-headless` |
| **Android SDK** (cmdline-tools, platform-tools, build-tools 35, android-35) | 编 APK / `adb` | 见下方一次性安装 |
| **adb** | 装 APK / logcat / clear data | apt 自带版本太老，用 SDK 里的 |
| **git** | 版本管理 | apt |

**不需要**：Gradle（项目自带 `gradlew` wrapper）、Android Studio（可选，详见末尾）。

## 一次性安装（Linux 开发机）

```bash
# JDK
sudo apt install -y openjdk-17-jdk-headless unzip wget

# Android SDK
ANDROID_HOME="$HOME/Android/Sdk"
mkdir -p "$ANDROID_HOME/cmdline-tools"
cd /tmp
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip
unzip -q cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools"
mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"

export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

写到 `~/.bashrc` / `~/.zshrc`：

```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
```

跑 `./scripts/bootstrap.sh` 自检。

## 构建一个 APK

```bash
cd android
./gradlew :app:assembleDebug
```

输出在 `android/app/build/outputs/apk/debug/app-debug.apk`（cycle 0030 ~14 MB，debug 签名 → 直接装手机）。

第一次构建会 download AGP / Kotlin / Compose / Room / KSP 等依赖（~5 min）；后续增量构建 ~10–30s。

常用 task：

```bash
./gradlew :app:assembleDebug          # 编译
./gradlew :app:installDebug           # 编译 + 通过 adb 装到当前连着的设备
./gradlew :core:test                  # 运行 :core 的 JVM 单元测试（目前没写）
./gradlew :app:connectedDebugAndroidTest  # 在连着的设备上跑 UI 测试（目前没写）
./gradlew clean                       # 清编译输出（不清依赖缓存）
```

## 把 APK 装到 vivo X200 Pro mini

### 一次性手机设置

vivo OriginOS 把开发权限藏得比 AOSP 深，按这个顺序开：

1. 设置 → 我的设备 → 全部参数 → 连点 **软件版本号** 7 次
2. **登录 vivo 账号**（这一步不做，下面那条 USB 调试是灰的）
3. 设置 → 系统管理 → 开发者选项：
   - 打开 **USB 调试**
   - 打开 **USB 安装**（最关键，否则 `adb install` 会被弹窗拦）
   - 打开 **USB 调试（安全设置）**
   - 关闭"应用验证"
4. 数据线接电脑 → 弹"是否允许 USB 调试" → 勾**始终允许这台计算机**

### 同 WiFi 时（推荐）

手机：开发者选项 → 无线调试 → 配对设备 → 看到 IP:port + 6 位码

```bash
adb pair <ip>:<port>          # 输 6 位码
adb connect <ip>:5555
adb devices                   # 应该看到设备 device 状态
```

### 装 APK 三种姿势

**姿势 A · 局域网 HTTP**（手机 / 开发机同 WiFi 但 ADB 没通）

```bash
./scripts/serve-apk.sh
# → http://<dev-machine-ip>:8000/treasure.apk
# 手机浏览器打开下载，点装
```

vivo 第一次会弹三道关：
1. "此类文件可能危害设备" → 仍然下载
2. "出于安全 此应用被禁止安装" → 跳设置 → 给浏览器开"安装外部来源应用"
3. "应用未经过 vivo 安全检测" → 继续安装

**姿势 B · adb install**（ADB 通了之后最快）

```bash
adb install android/app/build/outputs/apk/debug/app-debug.apk
# 覆盖装：
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

**姿势 C · 微信 / QQ / 邮件传文件**（最绕，但跨网络也行）

发给自己 → 手机端打开附件 → 装。

## 内循环（每秒在用）

| 想做的事 | 命令 / 工具 |
|---|---|
| 编 + 装到设备 | `./gradlew :app:installDebug && adb shell am start -n com.treasure/.MainActivity` |
| 看 log（只看 Treasure 标签） | `adb logcat -s Treasure:V *:E` |
| 看运行时崩溃栈 | `adb logcat -d *:E AndroidRuntime:E` |
| 杀进程模拟启动 | `adb shell am force-stop com.treasure` |
| 清数据回到首次启动 | `adb shell pm clear com.treasure`（会重新 seed） |
| 截图存到电脑 | `adb exec-out screencap -p > /tmp/shot.png` |
| 看权限当前状态 | `adb shell dumpsys package com.treasure \| grep -A2 permission` |
| 撤销某权限模拟拒绝 | `adb shell pm revoke com.treasure android.permission.RECORD_AUDIO` |
| 授某权限免弹框 | `adb shell pm grant com.treasure android.permission.RECORD_AUDIO` |

## 编辑器选择（SSH 开发机场景）

开发机在远端、你在本地 SSH 时：

| 选项 | Compose Preview | 综合体验 |
|---|---|---|
| **VS Code Remote-SSH** | ❌ | 现在用的，编辑流畅，build 走 CLI；视觉调试得装机 |
| **JetBrains Gateway → Android Studio** | ✅ | "AS over SSH"，能 Preview，但 AS 远程支持不如 IntelliJ 顺 |
| **本地装 Android Studio**，本地 clone | ✅ | 笔电够格直接本地开发，开发机只在重型 build 时用 |

视觉重的屏幕（如插画 / 抽屉细节）尽量靠真机验证或 Preview；纯逻辑（Repository / ViewModel）VS Code + CLI 完全够。

## 验证一次端到端

新装 APK 后这套 smoke test 跑一遍：

**浏览路径**

1. **冷启动** → Portal 出现，4 扇门 + 三连计数 + Latest entry
2. 点一扇门（如"摄影"）→ Grid 出现，能看到 X-T5 / M6 卡片
3. 点 X-T5 → Detail，博物馆相机线描 + 4 行 hero specs
4. **点 hero 卡片** → 600ms 翻面 → 真实照片或"尚未收录实拍"
5. **拉底部小条上滑** → 抽屉到 78% 屏高，切 3 tab 历史 / 参数 / 影集
6. **back 按钮** → 加粗箭头无文字 → 滑回 Grid

**编辑路径**

7. Detail 右上 **·** → Edit 单页
8. 改昵称 / 拖动 spec 改 hero 顺序 / 上传一张实拍 → 保存 → 回 Detail 看更新
9. DANGER ZONE 删除 → 弹确认 → 取消（不删）

**录入路径**

10. 控制岛 ⊕ 录入 → 看到 **RECORD** header（副标 = 当前对话标题，cycle 0018 起；进入默认续上次对话，cycle 0022 起）
11. 点 📷 → （首次）弹 READ_MEDIA_IMAGES 权限 → 选一张图 → AI 解析 → DraftCta 卡片（Pending 状态）
12. （cycle 0017 暂去掉了麦克风按钮，云端 STT 兜底是 cycle 0031 候选）
13. DraftCta 卡片：[采用]/[不要]（cycle 0024）；[采用] → 卡片置灰 + 下方出现 "✓ 已采用 · N 字段" 行 + confirmedDraft 升格
14. 继续聊："颜色是红色" → AI 在 confirmedDraft 上叠加，新 DraftCta 出现，旧的标 Rejected
15. 点 [手动] → push Refine 页（confirmedDraft 编辑器）→ 改字段 → [确认收入] → AlertDialog 二次确认 → 写 Room → 新对话
16. composer 多行输入 4 行 → 不溢出，不压控制岛胶囊
17. 头部 🕐 → 历史 ModalBottomSheet（cycle 0018）：列最近 20 段，点旧的 reload
18. 发 URL（如京东商品链接）→ 聊天里 SystemNote "正在抓取 jd.com…" → "✓ 已抓取 jd.com · 1.2K 字" / "⚠ 防爬挡住"

**搜索 + 分类管理（cycle 0026-0030）**

19. 图鉴页右上 [🔍][小红点] 两个图标；点 🔍 → SearchRoute → 输入"yon" → 立刻 2 列结果，标题里 "yon" terra 加粗
20. 点小红点 → ModalBottomSheet Manager：6 内建分类显示中 + divider + 已隐藏（空）
21. 长按某行 ≡ 拖动跨过 divider → 实时进入"已隐藏"段 → 回 Portal / Grid 它消失
22. 点行右侧小红点 → push CategoryEditor 全屏：内建有插画兜底；自定义必须从相册挑图才能 [新建]
23. 自定义"图书"创建后 → 录入页对 AI 说"《时间简史》" → AI 应自动选 category = custom-xxx → DraftCta → [采用] → [确认收入] → 物品落到 "图书" 分类

**设置 + AI**

24. 控制岛 ⊕ 设置 → 切 Provider chips（Anthropic / OpenAI / Custom 等 8 个 preset）→ 填 Model + Base URL（按 provider 显隐）+ API Key → 摘要卡显示 🖼 多模态 / 纯文本 pill（按 model 名启发式）→ 保存 → 测试连接（应返回 OK）
25. 清除 key → 录入页再点 📷 → 草稿不出现，AI 提示未配置

**持久化 / 图标 / Schema**

26. **杀进程**（`adb shell am force-stop com.treasure`）重启 → 数据还在
27. 桌面查看图标：cycle 0026 回到的平面圆环 + gold gradient + 顶/底 paper-color rune + 两侧 tick
28. `adb shell pm clear com.treasure` 重启 → 种子重新写入（v10 schema 直接 create，含 category_prefs 表 6 内建种子）
29. （可选）装 cycle 0030 之前的旧 APK 跑一次再覆盖装新版 → migration 5_6 / 6_7 / 7_8 / 8_9 / 9_10 链按需触发，数据不丢

## 常见踩坑

- **build 卡 dl.google.com**：偶尔某个 .aar TLS 抖一下挂掉。删那个目录重试：`rm -rf ~/.gradle/caches/modules-2/files-2.1/<group>/<artifact>/<version>; ./gradlew :app:assembleDebug`
- **schema 升级丢数据**：cycle 0010 起切了真 migration（[ADR-0006](adr/0006-schema-migrations.md)），当前 v10，链路完整：5→6→7→8→9→10。`fallbackToDestructiveMigrationOnDowngrade()` 还开着，**只**在用户从更高版本降回老 APK 时才会清库（dev 环境 OK，线上几乎不会发生）。每加 schema 改动必 bump + 写 Migration + 提交 schemas/N.json。
- **vivo 装非商店 APK 弹安全检测**：每装一次都会弹（"应用未经过 vivo 安全检测"），点继续安装。习惯就好。
- **覆盖装签名冲突**：debug 签名固定（Android Debug keystore），同包名同签名直接覆盖；除非你换了 keystore（不会发生）
- **真 STT 在国行 ROM 不可用**：vivo / 华为部分机型没装 Google App，`SpeechRecognizer.isRecognitionAvailable` 返回 false → 走 `onUnavailable` 回退（占位语音消息）。这是预期行为，不要拆掉 fallback。
- **PickVisualMedia 权限**：理论上不需要 READ_MEDIA_IMAGES（系统 picker），但 vivo / 华为某些 ROM 卡得严，所以仍主动请求一次更稳。拒绝后照样 launch picker —— 让 OS 自己决定。
- **AI 测试连接超时**：Anthropic / OpenAI 直连国内可能要代理。Custom provider 可以填国内兼容端点（DeepSeek / 月之暗面等 OpenAI 兼容）。Key 存在 `EncryptedSharedPreferences`，`adb shell pm clear` 会清。

## 推到 GitHub

```bash
cd /home/mi/workspace/treasure
git status
git add -A
git commit -m "your message"
git push
```

remote 已设：`git@github.com:kitacpt/Treasure.git`（main 分支）。开发机上 SSH key 已认证（`~/.ssh/id_rsa`）。
