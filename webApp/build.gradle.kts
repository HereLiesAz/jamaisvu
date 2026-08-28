import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(21)

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            // :shared depends on coil-compose too (as `implementation`, so it doesn't leak
            // here on its own) for AsyncImage itself; this module needs its own copy just
            // to call coil3.compose.setSingletonImageLoaderFactory() below. Android's
            // AsyncImage calls need no network engine at all -- every URI it's given is a
            // local file:///android_asset/... path. Web's are real HTTP fetches
            // (photoBaseUri()'s relative "photos/" path), which Coil's core Fetcher doesn't
            // handle on its own; only wasmJs needs either of these two, so they live here
            // rather than in :shared's own dependencies.
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }
    }
}
