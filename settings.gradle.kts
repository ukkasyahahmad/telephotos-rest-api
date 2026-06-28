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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // TDLib community repo from Dzakirana
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://dl.bintray.com/telegram/tdlib") }
    }
}

rootProject.name = "TelePhotos"
include(":app")