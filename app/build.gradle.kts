plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {

    packaging {
        resources {
            excludes += ("/META-INF/{AL2.0,LGPL2.1}")
            excludes += ("/META-INF/INDEX.LIST")
            excludes += ("/META-INF/DEPENDENCIES")
            excludes += ("/META-INF/LICENSE")
            excludes += ("/META-INF/LICENSE.txt")
            excludes += ("/META-INF/NOTICE")
            excludes += ("/META-INF/NOTICE.txt")
        }
    }

    namespace = "com.example.silentguardapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.silentguardapp"
        minSdk = 26
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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Google Speech-to-Text API
    implementation(libs.google.cloud.speech)
    implementation(libs.grpc.okhttp)

    // For JSON credentials handling
    implementation(libs.google.auth.library.oauth2.http)

    // Material Design 3
    implementation (libs.material.v1110)

    // Navigation Components
    implementation (libs.androidx.navigation.fragment.ktx)
    implementation (libs.androidx.navigation.ui.ktx)

    // Fragment KTX
    implementation (libs.androidx.fragment.ktx)


}