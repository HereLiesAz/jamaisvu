import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
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
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.viewmodel.ktx)
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.compose.foundation)
            implementation(libs.androidx.compose.animation)
            implementation(libs.androidx.compose.material3)
            implementation(libs.androidx.compose.material.icons.extended)
            implementation(libs.coil.compose)
            implementation(libs.google.play.app.update.ktx)
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.junit)
            }
        }
        wasmJsMain.dependencies {
            implementation(libs.kotlinx.browser)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

// The Compose BOM is a platform() dependency, which KotlinDependencyHandler (the scope inside
// kotlin.sourceSets.*.dependencies{}) doesn't expose -- only the classic per-configuration
// DependencyHandler does, hence applying it here by the KMP-generated configuration's name
// instead of inside the sourceSets block above.
dependencies {
    add("androidMainImplementation", platform(libs.androidx.compose.bom))
}
