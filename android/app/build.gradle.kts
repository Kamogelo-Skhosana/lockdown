plugins {
    id("com.android.application") version "8.9.2"
    id("org.jetbrains.kotlin.android") version "2.1.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
}

val releaseStore = providers.environmentVariable("LOCKDOWN_KEYSTORE").orNull
val releaseStorePassword = providers.environmentVariable("LOCKDOWN_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("LOCKDOWN_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("LOCKDOWN_KEY_PASSWORD").orNull

android {
    if (releaseStore != null && releaseStorePassword != null &&
        releaseKeyAlias != null && releaseKeyPassword != null
    ) {
        signingConfigs {
            create("lockdownRelease") {
                storeFile = rootProject.file(releaseStore)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
        buildTypes.named("release") {
            signingConfig = signingConfigs.getByName("lockdownRelease")
        }
    }

    namespace = "com.lockdown.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lockdown.android"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.05.00"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
}

kotlin {
    jvmToolchain(17)
}
