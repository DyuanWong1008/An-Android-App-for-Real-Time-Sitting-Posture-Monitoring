plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

apply(plugin = "com.google.gms.google-services")

android {
    namespace = "com.example.postureguard"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.postureguard"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // TensorFlow Lite dependencies
    implementation("org.tensorflow:tensorflow-lite:2.8.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.8.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.3.1")
    // MPAndroidChart library
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth:21.0.8")  // For Firebase Auth
    implementation("com.google.firebase:firebase-firestore:24.0.0")  // For Firebase Firestore
    implementation("com.google.firebase:firebase-core:21.1.1")  // Core Firebase functionality
    implementation("com.google.firebase:firebase-firestore-ktx:24.0.0")  // For Firebase Firestore KTX

    implementation("com.google.android.gms:play-services-auth:20.3.0")
    implementation("com.google.android.gms:play-services-base:18.1.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.0")

}