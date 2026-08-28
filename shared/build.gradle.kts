plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose.compiler)
}

kotlin {
    jvmToolchain(21)

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
    }
}

// The Compose BOM is a platform() dependency, which KotlinDependencyHandler (the scope inside
// kotlin.sourceSets.*.dependencies{}) doesn't expose -- only the classic per-configuration
// DependencyHandler does, hence applying it here by the KMP-generated configuration's name
// instead of inside the sourceSets block above.
dependencies {
    add("androidMainImplementation", platform(libs.androidx.compose.bom))
}
