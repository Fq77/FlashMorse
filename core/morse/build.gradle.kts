plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
}

// 纯 Kotlin(JVM) 模块：摩斯编解码 + 绝对时间轴编译，零 Android 依赖
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
    api(project(":core:model"))
    testImplementation(libs.junit)
}
