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

    // Compatibility fallback for browsers below the wasmJs floor -- see :shared/build.gradle.kts
    // for why this is a genuine, Compose-supported second web target rather than a stub.
    js {
        browser()
        binaries.executable()
    }

    sourceSets {
        // Same custom-hierarchy reasoning as :shared/build.gradle.kts: every dependency below
        // is identical between the two browser targets, so it's declared once here instead of
        // duplicated onto both wasmJsMain and jsMain.
        val webMain by creating {
            dependsOn(commonMain.get())
            dependencies {
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
                // handle on its own; only the browser targets need either of these two, so they
                // live here rather than in :shared's own dependencies.
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor3)
            }
        }
        wasmJsMain.get().dependsOn(webMain)
        jsMain.get().dependsOn(webMain)
    }
}
