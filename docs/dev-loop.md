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

输出在 `android/app/build/outputs/apk/debug/app-debug.apk`（约 11 MB，debug 签名 → 直接装手机）。

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

1. **冷启动** → Portal 出现，能看到 4 扇门 + 三连计数 + Latest entry
2. 点一扇门（如"摄影"）→ Grid 出现，能看到 X-T5 / M6 卡片
3. 点 X-T5 → Detail，能看到博物馆相机线描 + 4 行 hero specs
4. **点 hero 卡片** → 600ms 翻面 → 看到 3 张空相框 + "尚未收录实拍"
5. 再点一次 → 翻回正面
6. **拉底部小条上滑** → 抽屉到 78% 屏高
7. 切换 4 个 tab：历史 / 参数 / 影集 / 设置 —— 抽屉**高度不变**
8. 历史 tab 看到时间轴 + ★Δ↻+− 字形
9. 设置 tab → 删除这件物品 → 弹确认对话框 → 取消（不删）
10. **back 按钮** → 加粗箭头无文字 → 滑回 Grid
11. **杀进程**（`adb shell am force-stop com.treasure`）重启 → 数据还在
12. `adb shell pm clear com.treasure` 重启 → 种子重新写入

## 常见踩坑

- **build 卡 dl.google.com**：偶尔某个 .aar TLS 抖一下挂掉。删那个目录重试：`rm -rf ~/.gradle/caches/modules-2/files-2.1/<group>/<artifact>/<version>; ./gradlew :app:assembleDebug`
- **schema 升 v3 后旧装的 app 数据丢了**：cycle 0001-0002 期使用 `fallbackToDestructiveMigration()`。cycle 0003 起需要写真 migration。
- **vivo 装非商店 APK 弹安全检测**：每装一次都会弹（"应用未经过 vivo 安全检测"），点继续安装。习惯就好。
- **覆盖装签名冲突**：debug 签名固定（Android Debug keystore），同包名同签名直接覆盖；除非你换了 keystore（不会发生）

## 推到 GitHub

```bash
cd /home/mi/workspace/treasure
git status
git add -A
git commit -m "your message"
git push
```

remote 已设：`git@github.com:kitacpt/Treasure.git`（main 分支）。开发机上 SSH key 已认证（`~/.ssh/id_rsa`）。
