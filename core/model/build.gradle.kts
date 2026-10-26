plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
}

// 纯 Kotlin(JVM) 模块：领域模型，零 Android 依赖（规划文档 §5.1）
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}

dependencies {
    testImplementation(libs.junit)
}
