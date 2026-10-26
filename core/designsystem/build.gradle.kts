plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.flashmorse.core.designsystem"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // api 而非 implementation：公开 Composable 的签名会暴露 Miuix 类型（规划文档 §7.3）
    api(project(":core:model"))
    api(platform(libs.androidx.compose.bom))
    api(libs.miuix.ui)
    api(libs.miuix.preference)
    api(libs.miuix.icons)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
