import org.gradle.api.JavaVersion

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.famekodriver.core"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    api(project(":shared-models"))
    
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.postgresql.android) // Downgraded for Android compatibility (ManagementFactory issue)
    api(libs.kotlinx.coroutines.android)
    
    // Networking
    api(libs.retrofit)
    api(libs.retrofit.converter.gson)
    api(libs.okhttp)
    api(libs.gson)
    api(libs.webrtc)

    // Room
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    api(libs.androidx.work.runtime.ktx)
    
    api(libs.androidx.viewpager2)
    api(libs.coil)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockito.core)
}
