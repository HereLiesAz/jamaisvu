import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose.compiler)
}

android {
    namespace = "com.hereliesaz.lamplight"
    compileSdk = 37

    val releaseStoreFile = System.getenv("LAMPLIGHT_KEYSTORE_FILE")?.takeIf { it.isNotBlank() }
    val releaseStorePassword = System.getenv("LAMPLIGHT_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
    val releaseKeyAlias = System.getenv("LAMPLIGHT_KEY_ALIAS")?.takeIf { it.isNotBlank() }
    val releaseKeyPassword = System.getenv("LAMPLIGHT_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
    val releaseStoreType = System.getenv("LAMPLIGHT_KEYSTORE_TYPE")?.takeIf { it.isNotBlank() }
    val hasReleaseSigning = releaseStoreFile != null && releaseStorePassword != null &&
        releaseKeyAlias != null && releaseKeyPassword != null
    val requireSigning = System.getenv("LAMPLIGHT_REQUIRE_SIGNING")?.equals("true", ignoreCase = true) == true

    if (requireSigning && !hasReleaseSigning) {
        val missing = buildList {
            if (releaseStoreFile == null) add("LAMPLIGHT_KEYSTORE_FILE")
            if (releaseStorePassword == null) add("LAMPLIGHT_KEYSTORE_PASSWORD")
            if (releaseKeyAlias == null) add("LAMPLIGHT_KEY_ALIAS")
            if (releaseKeyPassword == null) add("LAMPLIGHT_KEY_PASSWORD")
        }
        throw GradleException("Release signing is required but incomplete: ${missing.joinToString(", ")}")
    }

    defaultConfig {
        applicationId = "com.hereliesaz.lamplight"
        minSdk = 28
        targetSdk = 37
        versionCode = (project.findProperty("versionBuild") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "1.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = File(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                if (releaseStoreType != null) storeType = releaseStoreType
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)

    implementation(composeBom)
    androidTestImplementation(composeBom)

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

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
