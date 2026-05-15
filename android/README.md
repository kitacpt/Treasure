# android/ · Android app

Kotlin + Jetpack Compose 多模块工程；自带 `gradlew`（wrapper），不需要 Android Studio。

```
android/
├── app/                # :app —— Compose 屏幕 / 导航 / 主题 / 插画 / 数据存储 / 音频 / 后台 service
├── core/               # :core —— 域模型 / Room / Repository / AI 客户端 / PageFetcher（纯 Kotlin/JVM）
├── core/schemas/       # exportSchema 的 JSON（v5–v16）
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml
└── gradlew
```

完整目录树（每个文件干啥）见 [`../docs/architecture.md`](../docs/architecture.md)。

## 跑一次

```bash
cd android
ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

构建 / 装机 / vivo 调试细节 → [`../docs/dev-loop.md`](../docs/dev-loop.md)。
