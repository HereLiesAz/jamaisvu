pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // PREFER_PROJECT, not FAIL_ON_PROJECT_REPOS: the Kotlin/Wasm plugin registers its own
    // repository for the Node.js/Yarn toolchain it downloads, and FAIL_ON_PROJECT_REPOS
    // rejects that outright. PREFER_SETTINGS looks like it would work but doesn't -- it
    // silently never searches the project-added repo at all.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Lamplight"
include(":shared")
include(":androidApp")
include(":webApp")
