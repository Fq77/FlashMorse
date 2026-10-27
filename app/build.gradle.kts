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
        versionCode = 3
        versionName = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // 与 release 使用不同的 applicationId：两个包可以同时存在，
            // 正式签名版与调试版互不覆盖，装调试卷时不必卸载正式版，也就不会丢数据。
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            optimization {
                enable = true
            }
            // 默认沿用 debug 签名，方便本机直接安装调试。
            // 如需正式签名，请在本机配置 keystore.properties 并在此处读取，
            // 不要把密钥与密码提交进版本库。
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
