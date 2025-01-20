plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)


}



val kotlinVersion = "2.1.0"

android {
    namespace = "com.example.traincheckinapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.traincheckinapp.ui.theme"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom)) // BOM for Compose versions
    implementation(libs.material3)
    implementation(libs.androidx.biometric.v110)
    implementation(libs.play.services.location)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.ui)
    implementation(libs.androidx.animation)
    implementation(libs.androidx.material3.v100) // Ensure this doesn't conflict with the main material3 dependency
    implementation(libs.androidx.foundation)
    implementation (libs.okhttp.v493)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.androidx.biometric)
    implementation(libs.okhttp3.okhttp)
    implementation(libs.exoplayer)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.jetbrains.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.animation.core.lint)
    implementation(libs.ui.tooling.preview.android)
    implementation (libs.androidx.material.icons.extended.v140)
    implementation(libs.androidx.games.activity)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation (libs.google.firebase.auth.ktx.v2101) // Check for the latest version
    implementation (libs.play.services.auth)
    implementation(libs.firebase.auth) // Check for the latest version
    implementation(platform(libs.firebase.bom))
    implementation(libs.google.firebase.analytics)


    // Testing libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom)) // Compose BOM for testing
}


