plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.treasure.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Cycle 0011: 把 schema JSON 当作 androidTest assets 打进测试 APK，
    // MigrationTestHelper 才能读到 v5 / v6 / v7 的快照。
    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")
}

ksp {
    // Cycle 0010: 让 Room 把每个 schema 版本导出到 :core/schemas，
    // 对应 ADR-0006 的 schema migration 制度。schemas 是 source-of-truth，
    // 跟 git 走，不打包进 release APK。
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    api(libs.kotlinx.serialization.json)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.okhttp)

    // Cycle 0013：仅为了 @Immutable / @Stable 注解，让 :app 那边的
    // Compose 运行时能把 domain 数据类当作可跳过的 stable 参数。
    // BOM 让版本和 :app 对齐。
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)

    // Room migration tests (cycle 0011 / ADR-0006).
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
