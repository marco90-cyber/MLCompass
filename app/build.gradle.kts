plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.marco.bussola"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.marco.bussola"
        minSdk = 26
        targetSdk = 35
        // In CI il numero cresce a ogni build (variabile VERSION_CODE)
        val build = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionCode = build
        versionName = "1.0.$build"
    }

    // Chiave fissa inclusa nel progetto: tutte le build (anche da GitHub Actions)
    // hanno la stessa firma, quindi l'APK si aggiorna sopra la versione già installata.
    signingConfigs {
        create("release") {
            storeFile = file("bussola.keystore")
            storePassword = "bussola"
            keyAlias = "bussola"
            keyPassword = "bussola"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
}
