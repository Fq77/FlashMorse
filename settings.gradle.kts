pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FlashMorse"

// 组装层
include(":app")

// 核心层：依赖方向严格单向 core/* -> feature/* -> app
include(":core:model")
include(":core:morse")
include(":core:torch")
include(":core:data")
include(":core:designsystem")

// 功能层
include(":feature:compose")
include(":feature:manual")
include(":feature:library")
include(":feature:settings")
include(":feature:learn")
