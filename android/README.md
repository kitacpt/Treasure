# android/ · Android app（待建工程）

cycle 0001 的第一步是在这里 `gradle init` 出 `:app` + `:core` 多模块工程。

预期结构（[`../docs/architecture.md`](../docs/architecture.md) 里有完整版）：

```
android/
├── app/                # :app —— Compose 屏幕、导航、主题
├── core/               # :core —— 领域 + Room + Repository（纯 Kotlin/JVM）
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml
└── gradlew
```

工作清单见 [`../openspec/0001-mvp-portal-grid-detail-add/spec.md`](../openspec/0001-mvp-portal-grid-detail-add/spec.md)。
