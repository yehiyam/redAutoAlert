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
            create("release") {
                val keystoreFile = file(keystorePath)
                storeFile = keystoreFile
                storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val releaseSigning = signingConfigs.findByName("release")
            if (releaseSigning != null) {
                signingConfig = releaseSigning
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
