import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}


// Load local.properties
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}

android {
    namespace = "com.example.myapplication2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapplication2"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject API key from local.properties into BuildConfig
        val apiKey: String = localProperties.getProperty("TOMORROW_API_KEY") ?: ""
        buildConfigField("String", "TOMORROW_API_KEY", "\"$apiKey\"")
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
        viewBinding = true
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.0"
    }
}

dependencies {
    // Network + JSON
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")

    // MPAndroidChart (via JitPack)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Material Design
    implementation("com.google.android.material:material:1.12.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Preferences
    implementation("androidx.preference:preference-ktx:1.2.0")

    // ConstraintLayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // FlexboxLayout
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // osmdroid
    implementation("org.osmdroid:osmdroid-android:6.1.16")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // AndroidX AppCompat
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Google Play Services - Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Jetpack Compose
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui:1.6.7")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.7")

    // Room
    implementation("androidx.room:room-runtime:2.7.2")
    kapt("androidx.room:room-compiler:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")

    // Compose Material3
    implementation("androidx.compose.material3:material3:1.2.1")

    // Lottie animations
    implementation("com.airbnb.android:lottie:6.4.0")

    // Navigation (hardcoded versions assumed, replace if you want specific versions)
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")

    // Core KTX
    implementation("androidx.core:core-ktx:1.10.1")

    // Core Animation (if you mean androidx.core:core-animation)
    implementation("androidx.core:core-animation:1.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

