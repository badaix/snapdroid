import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "de.badaix.snapcast"

    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "de.badaix.snapcast"
        minSdk = 21
        targetSdk = 36
        versionCode = 2900
        versionName = "0.29.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++14"
                arguments += "-DANDROID_STL=c++_static"
                arguments += "-DBUILD_SERVER=OFF"
                arguments += "-DBUILD_TESTS=OFF"
            }
        }
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }

    buildFeatures {
        compose = true
        prefab = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/snapcast/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.material:material:1.13.0")

    implementation("com.github.badaix:oboe:1.9.0@aar")
    implementation("com.github.badaix:boost:1.85.0@aar")
    implementation("com.github.badaix:flac:1.4.2@aar")
    implementation("com.github.badaix:ogg:1.3.5@aar")
    implementation("com.github.badaix:opus:1.1.2@aar")
    implementation("com.github.badaix:soxr:0.1.3@aar")
    implementation("com.github.badaix:tremor:1.0.1@aar")
    implementation("com.github.badaix:vorbis:1.3.7@aar")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
