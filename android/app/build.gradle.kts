plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.treasure"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.treasure"
        minSdk = 26
        targetSdk = 35
        versionCode = 14
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.coil.compose)
    implementation(libs.androidx.security.crypto)
    // Cycle 0036：AI 助手气泡 markdown 渲染（标题 / 加粗 / 列表 / 链接 / 代码
    // 块 / 表格 / 引用），只用 m3 核心 — image transformer 暂不接，AI 文本里
    // 出现图片的几率极低，节省 ~100KB 包体。
    implementation(libs.multiplatform.markdown.renderer.m3)
    // Cycle 0036 v2：客户端从 PDF 文件附件里提取文本，作为 user-turn 上下文
    // 喂给 AI（所有 provider 都吃）。约 +3MB 包体；纯文本 / 源代码类文件不
    // 经过它，直接 UTF-8 读。
    implementation(libs.pdfbox.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
