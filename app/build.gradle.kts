plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.redautoalert"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.redautoalert"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("BUILD_VERSION_CODE")?.toIntOrNull()) ?: 1
        versionName = System.getenv("BUILD_VERSION_NAME") ?: "1.0.0"
    }

    signingConfigs {
        val keystorePath = System.getenv("SIGNING_KEYSTORE_PATH")
        if (!keystorePath.isNullOrBlank()) {
            val keystoreFile = file(keystorePath)
            val storePasswordEnv = System.getenv("SIGNING_KEYSTORE_PASSWORD")
            val keyAliasEnv = System.getenv("SIGNING_KEY_ALIAS")
            val keyPasswordEnv = System.getenv("SIGNING_KEY_PASSWORD")

            if (!keystoreFile.exists()) {
                error("SIGNING_KEYSTORE_PATH is set to '$keystorePath' but the keystore file does not exist.")
            }

            if (storePasswordEnv.isNullOrBlank() || keyAliasEnv.isNullOrBlank() || keyPasswordEnv.isNullOrBlank()) {
                error(
                    "Signing configuration is incomplete. " +
                        "Ensure SIGNING_KEYSTORE_PASSWORD, SIGNING_KEY_ALIAS, and SIGNING_KEY_PASSWORD are all set."
                )
            }

            create("release") {
                storeFile = keystoreFile
                storePassword = storePasswordEnv
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val releaseSigning = signingConfigs.findByName("release")
            val isReleaseTaskRequested = gradle.startParameter.taskNames.any { it.endsWith("Release") }
            if (releaseSigning != null) {
                signingConfig = releaseSigning
            } else if (isReleaseTaskRequested) {
                error(
                    "Requested a release build (e.g. assembleRelease/bundleRelease), " +
                            "but no 'release' signingConfig is configured. " +
                            "Ensure SIGNING_KEYSTORE_PATH and related signing env vars are set."
                )
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Car App Library (for CarAppExtender on notifications)
    implementation("androidx.car.app:app:1.4.0")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
