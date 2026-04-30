/*
 * MIT License Copyright (c) 2021. Tarek Bohdima
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * This project was submitted by Tarek Bohdima as part of the Android Kotlin
 * Developer Nanodegree At Udacity. As part of Udacity Honor code, your
 * submissions must be your own work, hence submitting this project as yours will
 * cause you to break the Udacity Honor Code and the suspension of your account.
 * I, the author of the project, allow you to check the code as a reference, but
 * if you submit it, it's your own responsibility if you get expelled.
 */

import java.util.Properties

// Plugin order matters: kotlin-android must be applied before
// androidx.navigation.safeargs.kotlin (the safe-args plugin checks for the
// kotlin plugin during apply and fails fast otherwise).
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.navigation.safeargs)
}

// Version components — bump these (not versionCode / versionName directly) when
// cutting a release. Classifier choices: INTERNAL, ALPHA, BETA, RC, RELEASE.
// .github/workflows/release.yml greps these names, so don't rename them.
val versionMajor = 1
val versionMinor = 3
val versionPatch = 5
val versionClassifier = "INTERNAL"

// SDK levels. Hoisted as named vals so they're easy to grep / bump.
val asteroidMinSdk = 26
val asteroidTargetSdk = 35
val asteroidCompileSdk = 35

// Modexa trick #7: read "X from environment, falling back to local.properties"
// was inlined four times in the Groovy script. Centralized here so adding a new
// secret in a later phase is a one-liner.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun env(name: String, default: String = ""): String =
    System.getenv(name) ?: localProperties.getProperty(name, default)

android {
    namespace = "com.tarek.asteroidradar"
    compileSdk = asteroidCompileSdk

    defaultConfig {
        applicationId = "com.tarek.asteroidradar"
        minSdk = asteroidMinSdk
        targetSdk = asteroidTargetSdk
        versionCode = asteroidMinSdk * 1_000_000 +
            versionMajor * 10_000 +
            versionMinor * 100 +
            versionPatch
        versionName = "$versionMajor.$versionMinor.$versionPatch-$versionClassifier"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = env("KEYSTORE_PATH")
            storeFile = if (keystorePath.isNotBlank()) file(keystorePath) else null
            storePassword = env("KEYSTORE_PASSWORD")
            keyAlias = env("KEY_ALIAS")
            keyPassword = env("KEY_PASSWORD")
            storeType = "PKCS12"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "NASA_API_KEY", "\"${env("NASA_API_KEY")}\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            buildConfigField("String", "NASA_API_KEY", "\"${env("NASA_API_KEY")}\"")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    sourceSets {
        named("main") {
            java.srcDir("build/generated/ksp/src/main/kotlin")
        }
    }

    androidResources {
        generateLocaleConfig = true
    }

    buildFeatures {
        buildConfig = true
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    implementation(fileTree("libs") { include("*.jar") })

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)

    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.navigation)
    implementation(libs.bundles.networking)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.picasso)
    implementation(libs.timber)

    testImplementation(libs.junit)
    androidTestImplementation(libs.bundles.android.test)
}
