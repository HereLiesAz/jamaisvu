import java.io.File

plugins {
    alias(libs.plugins.android.application)
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

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":shared"))

    // Only :androidApp has real debug/release build types, so this dev-only tooling artifact
    // (Compose preview rendering support) is scoped here rather than in :shared, matching how
    // it was scoped before the module split. It's BOM-managed (no explicit version in the
    // catalog), and this module's own debugCompileClasspath has no visibility into :shared's
    // separately-applied BOM, so it needs its own.
    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)
}
