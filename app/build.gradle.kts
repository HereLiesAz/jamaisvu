import java.io.File

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:2.7.0")

    testImplementation("junit:junit:4.13.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
