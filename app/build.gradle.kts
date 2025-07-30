import java.util.Properties
import java.io.FileInputStream

val isCiBuild = System.getenv("CI") == "true"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.compose") version libs.versions.kotlin.get()
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.performance)
}
android {
    namespace = "com.tk.pennywise"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tk.pennywise"
        minSdk = 24
        targetSdk = 35
        versionCode = 9
        versionName = "1.07"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }
    signingConfigs {
        create("release") {
            val storeFileProperty = project.findProperty("ANDROID_SIGNING_STORE_FILE")?.toString()
            val envVarStoreFile = System.getenv("ANDROID_SIGNING_STORE_FILE")

            // Determine the path string to use for the keystore
            // Priority: gradle.properties -> Environment Variable -> Default
            val keystorePathString = storeFileProperty ?: envVarStoreFile ?: "release-keystore.jks" // Keystore is expected to be in 'app' module dir by default if not specified elsewhere

            val storePasswordValue = project.findProperty("ANDROID_SIGNING_STORE_PASSWORD")?.toString()
                ?: System.getenv("ANDROID_SIGNING_STORE_PASSWORD")

            val keyAliasValue = project.findProperty("ANDROID_SIGNING_KEY_ALIAS")?.toString()
                ?: System.getenv("ANDROID_SIGNING_KEY_ALIAS")

            val keyPasswordValue = project.findProperty("ANDROID_SIGNING_KEY_PASSWORD")?.toString()
                ?: System.getenv("ANDROID_SIGNING_KEY_PASSWORD")

            // Use Gradle's 'file()' method to resolve the path relative to the current project (the 'app' module)
            // This is where 'release-keystore.jks' (if that's the value of keystorePathString) should exist.
            val resolvedKeystoreFile = project.file(keystorePathString) // project.file() here will resolve relative to the app module.
            // If keystorePathString was "app/release-keystore.jks", you'd use rootProject.file(keystorePathString)

            if (resolvedKeystoreFile.exists() && // Check existence using the Gradle-resolved file object
                storePasswordValue != null &&
                keyAliasValue != null &&
                keyPasswordValue != null
            ) {
                storeFile = resolvedKeystoreFile // Assign the Gradle-resolved file object
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            } else {
                if (!isCiBuild) {

                }
                // If essential details are missing, you might want to prevent the signingConfig from being fully set up
                // which will lead to a build failure if signing is required, as it is now.
                // For example, by not assigning to storeFile, storePassword etc. if they are null.
                // The current error "missing required property" happens because storeFile isn't being set.
            }
        }
    }


    buildTypes {
        debug {
            // Define Ad Unit IDs for DEBUG builds (Test IDs)
            buildConfigField("String", "APP_OPEN_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            // Add other test ad unit IDs if you have them (Banner, Interstitial)
            // buildConfigField("String", "BANNER_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            // buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/1033173712\"")

            applicationIdSuffix = ".debug" // Optional: makes debug app install alongside release
            versionNameSuffix = "-debug"   // Optional: helps distinguish debug builds
            isMinifyEnabled = false        // Typically false for debug
        }
        release {
            // Define REAL Ad Unit IDs for RELEASE builds
            buildConfigField("String", "APP_OPEN_AD_UNIT_ID", "\"ca-app-pub-5040172786842435/7708652322\"")
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"ca-app-pub-5040172786842435/4324775236\"")
            // buildConfigField("String", "BANNER_AD_UNIT_ID", "\"YOUR_REAL_BANNER_AD_UNIT_ID_HERE\"")
            // buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"ca-app-pub-5040172786842435/5658586949\"")

            isMinifyEnabled = true // Enable R8/ProGuard for release
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro" // Make sure this file exists and has rules for Firebase/Ads
            )
            // For Crashlytics, mapping file upload is typically enabled by default for release
            // to deobfuscate crash reports.
            // firebaseCrashlytics {
            //    mappingFileUploadEnabled = true
            // }
            signingConfig = signingConfigs.getByName("release") // Always apply for release builds
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true // Enable Compose
        viewBinding = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14" // Match the version from the error
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(platform(libs.firebase.bom))

    // Firebase
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.crashlytics.ktx)
    // Firebase App Check (Play Integrity provider)
    implementation(libs.firebase.appcheck.playintegrity)

    // Firebase Performance Monitoring
    implementation(libs.firebase.perf.ktx)

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
    implementation(libs.jetbrains.kotlinx.coroutines.play.services)
    implementation(libs.play.services.ads)
    implementation(libs.kotlinx.coroutines.play.services.v173)

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

    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime)

    implementation(libs.androidx.multidex)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
// AndroidX Lifecycle (Essential for AppOpenAdManager and modern Android development)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Now you don't need to specify versions for individual Compose libraries
    implementation(libs.androidx.ui)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)

    implementation(platform(libs.firebase.bom.vlatestbomversion)) // Ensure you have the BOM
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.analytics)
}
apply(plugin = libs.plugins.google.gms.google.services.get().pluginId)