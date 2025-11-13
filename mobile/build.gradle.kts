import java.util.Properties
import java.io.FileInputStream

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val GETSONG_API_KEY: String = localProperties.getProperty("GETSONG_API_KEY") ?: ""
val SPOTIFY_CLIENT_ID: String = localProperties.getProperty("SPOTIFY_CLIENT_ID") ?: ""
val SPOTIFY_CLIENT_SECRET: String = localProperties.getProperty("SPOTIFY_CLIENT_SECRET") ?: ""

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.pwr.yourrhythm"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
        compose = true
    }

    defaultConfig {
        manifestPlaceholders.put("redirectSchemeName", "com.pwr.yourrhythm")
        manifestPlaceholders.put("redirectHostName", "callback")
        applicationId = "com.pwr.yourrhythm"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GETSONG_API_KEY", "\"$GETSONG_API_KEY\"")
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"$SPOTIFY_CLIENT_ID\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"$SPOTIFY_CLIENT_SECRET\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // DATA LAYER API
    implementation(libs.play.services.wearable)

    // Spotify API
    implementation(files("../mobile/libs/spotify-app-remote-release-0.8.0.aar"))
    implementation(files("../mobile/libs/spotify-auth-release-2.1.0.aar"))
    implementation(libs.gson.v2101)

    // HTTP Requests
    implementation(libs.okhttp.v4110)

    // DATA ENCRYPTION
    implementation(libs.androidx.security.crypto)

    // COMPOSE
    val composeBom = platform("androidx.compose:compose-bom:2025.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Choose one of the following:
    // Material Design 3
    implementation(libs.androidx.compose.material3)
    // or skip Material Design and build directly on top of foundational components
    implementation(libs.androidx.foundation)
    // or only import the main APIs for the underlying toolkit systems,
    // such as input and measurement/layout
    implementation(libs.androidx.ui)

    // Android Studio Preview support
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // UI Tests
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Optional - Add window size utils
    implementation(libs.androidx.compose.adaptive)

    // Optional - Integration with activities
    implementation(libs.androidx.activity.compose)
    // Optional - Integration with ViewModels
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // Optional - Integration with LiveData
    implementation("androidx.compose.runtime:runtime-livedata")
    // Optional - Integration with RxJava
    implementation("androidx.compose.runtime:runtime-rxjava2")

    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    wearApp(project(":wear"))
}