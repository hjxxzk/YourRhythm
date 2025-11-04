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
}

android {
    namespace = "com.pwr.yourrhythm"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
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

    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    wearApp(project(":wear"))
}