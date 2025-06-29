plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.pennywise"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.pennywise"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.00"

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
}

dependencies {
    implementation(libs.firebase.bom)

    // Firebase
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore.ktx)

    // AndroidX and UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Libraries
    implementation(libs.lottie)
    implementation(libs.mpandroidchart)
    implementation(libs.threetenabp)
    implementation(libs.glide)
    implementation(libs.dotsindicator)
    implementation(libs.firebase.firestore)
    implementation (libs.jetbrains.kotlinx.coroutines.play.services)
    implementation (libs.play.services.ads)
    implementation (libs.kotlinx.coroutines.play.services.v173)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Google Sign-In
    implementation(libs.play.services.auth) // Check for the latest version

    // Room
    implementation(libs.androidx.room.runtime.v261)
    kapt(libs.androidx.room.compiler.v261)
    implementation(libs.androidx.room.ktx.v261)
}
apply(plugin = libs.plugins.google.gms.google.services.get().pluginId)