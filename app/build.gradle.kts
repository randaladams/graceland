plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.adamselite.gracelandmiles"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.adamselite.gracelandmiles"
        minSdk = 24
        targetSdk = 36
        versionCode = 13
        versionName = "13"
    }

    // Release signing: reads from environment variables so no secret ever lives
    // in this file or in git. Locally, unset vars just skip signing (debug builds
    // are unaffected). In GitHub Actions, these are populated from repo secrets.
    signingConfigs {
        create("release") {
            val ksPath = System.getenv("RELEASE_KEYSTORE_PATH")
            if (ksPath != null) {
                storeFile = file(ksPath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (System.getenv("RELEASE_KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.gms:play-services-ads:23.3.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.android.billingclient:billing-ktx:8.0.0")
}
