plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.flashmorse.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.flashmorse.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            // 个人项目直接用 debug 签名出 release 包：与本机调试包同签名，
            // adb install -r 可原地升级且保留设置/短语/历史数据。
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        // 关于页与设置页显示版本号时读 BuildConfig，避免和 manifest 里的 versionName 各写一份
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.miuix.nav)

    implementation(project(":core:model"))
    implementation(project(":core:morse"))
    implementation(project(":core:torch"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))

    implementation(project(":feature:compose"))
    implementation(project(":feature:manual"))
    implementation(project(":feature:library"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:learn"))

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
