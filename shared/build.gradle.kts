import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

// "Auto" (the default) apparently doesn't detect this module's resource usage -- forcing
// generation explicitly rather than relying on whatever heuristic Auto uses.
compose.resources {
    generateResClass = always
}

kotlin {
    jvmToolchain(21)

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        // Required even though :shared is a library, not an app: once it has Compose UI code
        // (the spike screen below), the Compose UI test runner needs the Skiko runtime, which
        // only loads from a bundled executable. Without this,
        // checkComposeUiTestConfigurationForWasmJs fails outright (CMP-4906).
        binaries.executable()
    }

    // Compatibility fallback for browsers below the wasmJs floor (WasmGC support: Chrome 119+/
    // Firefox 120+/Safari 18.2+). Compose Multiplatform's canvas-based Skiko backend still
    // supports plain Kotlin/JS -- confirmed via the Gradle plugin's own
    // checkJsMainComposeLibrariesCompatibility task -- it's just no longer the flagship web
    // target the way wasmJs is.
    js {
        browser()
        // Same CMP-4906 requirement as wasmJs above, confirmed the hard way: CI's allTests
        // failed with "Add binaries.executable() to the 'js' target" from
        // checkComposeUiTestConfigurationForJs without this.
        binaries.executable()
    }

    android {
        // Deliberately different from :androidApp's namespace (com.hereliesaz.lamplight) -- AGP
        // collides on the generated R class if a library and its consuming app share one.
        namespace = "com.hereliesaz.lamplight.shared"
        compileSdk = 37
        minSdk = 28

        withHostTest {
            isReturnDefaultValues = true
        }

        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.animation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            // Multiplatform since Lifecycle 2.8 -- ViewModel/viewModelScope no longer need the
            // Android-only lifecycle-viewmodel-ktx artifact, and this pulls both in for every
            // target, needed now that LamplightViewModel itself lives here. api, not
            // implementation: LamplightViewModel's own supertype is ViewModel, from this
            // dependency, so any module referencing LamplightViewModel (e.g. :webApp's main())
            // needs it on its own compile classpath too.
            api(libs.androidx.lifecycle.viewmodel.compose)
            // The Coil 3.x multiplatform rewrite -- AsyncImage itself needs no per-target setup,
            // now that LamplightApp.kt (and the photos it renders) lives here too.
            implementation(libs.coil.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.compose.foundation)
            implementation(libs.androidx.compose.animation)
            implementation(libs.androidx.compose.material3)
            implementation(libs.androidx.compose.material.icons.extended)
            implementation(libs.google.play.app.update.ktx)
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.junit)
            }
        }
        // A custom intermediate source set, not Kotlin's default-hierarchy-template "webMain"
        // (that one isn't reliably present yet when this block evaluates -- referencing it via
        // getByName() here fails with "KotlinSourceSet with name 'webMain' not found"). Explicit
        // dependsOn wiring below instead. Browser-interop code genuinely identical across both
        // leaf targets (kotlinx-browser's window/localStorage share one API across js and
        // wasmJs) lives here once, instead of duplicated per target.
        val webMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.kotlinx.browser)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        wasmJsMain.get().dependsOn(webMain)
        jsMain.get().dependsOn(webMain)
    }
}

// The Compose BOM is a platform() dependency, which KotlinDependencyHandler (the scope inside
// kotlin.sourceSets.*.dependencies{}) doesn't expose -- only the classic per-configuration
// DependencyHandler does, hence applying it here by the KMP-generated configuration's name
// instead of inside the sourceSets block above.
dependencies {
    add("androidMainImplementation", platform(libs.androidx.compose.bom))
}
