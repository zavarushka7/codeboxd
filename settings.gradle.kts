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

rootProject.name = "codeboxd"
include(":app")
include(":core:model")
include(":core:network")
include(":core:data")
include(":core:designsystem")
include(":feature:auth")
include(":feature:onboarding")
include(":feature:profile")
include(":feature:settings")
include(":core:navigation")
include(":core:common")
include(":core:domain")
