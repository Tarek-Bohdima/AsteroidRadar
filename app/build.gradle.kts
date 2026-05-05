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

// All plugin application + SDK levels + JVM toolchain + default test runner
// + buildConfig flag come from the convention plugin in `build-logic/`. Only
// app-specific config stays here (namespace, applicationId, version, signing,
// buildTypes, sourceSets, dataBinding).
plugins {
    id("asteroidradar.android.application")
    id("asteroidradar.android.hilt")
    alias(libs.plugins.kover)
}

// Version components — bump these (not versionCode / versionName directly) when
// cutting a release. Classifier choices: INTERNAL, ALPHA, BETA, RC, RELEASE.
// .github/workflows/release.yml greps these names, so don't rename them.
val versionMajor = 1
val versionMinor = 3
val versionPatch = 5
val versionClassifier = "INTERNAL"

// versionCode formula uses minSdk as a high digit so a future minSdk bump
// auto-pushes versionCode forward without colliding with prior releases.
// Mirrors the value the convention plugin sets for `defaultConfig.minSdk`.
val versionCodeMinSdk = 26

// Modexa trick #7: read "X from environment, falling back to local.properties"
// was inlined four times in the Groovy script. Centralized here so adding a new
// secret in a later phase is a one-liner.
val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use(::load)
    }

fun env(
    name: String,
    default: String = "",
): String = System.getenv(name) ?: localProperties.getProperty(name, default)

// Fail Gradle (after configure, before execute) when a release task is in the
// graph and NASA_API_KEY is blank. Scoped to the task graph rather than the
// release build-type block so `assembleDebug` / `test` / docs-only invocations
// stay lenient — a missing key is only fatal when we're actually shipping.
gradle.taskGraph.whenReady {
    val releaseTasks = setOf("assembleRelease", "bundleRelease", "packageRelease")
    if (allTasks.any { it.name in releaseTasks }) {
        require(env("NASA_API_KEY").isNotBlank()) {
            "NASA_API_KEY is required for release builds. Set it via env var or local.properties."
        }
    }
}

android {
    namespace = "com.tarek.asteroidradar"

    defaultConfig {
        applicationId = "com.tarek.asteroidradar"
        versionCode = versionCodeMinSdk * 1_000_000 +
            versionMajor * 10_000 +
            versionMinor * 100 +
            versionPatch
        versionName = "$versionMajor.$versionMinor.$versionPatch-$versionClassifier"
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
            // R8 + resource shrinking ride along with Phase 6b (the AGP bump);
            // R8 from AGP 8.3.0 can't parse Kotlin 2.1 metadata that Retrofit
            // 3.0+ ships with. Slim `proguard-rules.pro` lands now so the
            // Phase 6b PR is just an `isMinifyEnabled` flip.
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
        dataBinding = true
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
    // androidx-hilt:hilt-work is a separate artifact group from the dagger-hilt
    // ones (which the convention plugin wires); the matching compiler is also
    // distinct and goes through KSP, not kapt.
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.coil)
    implementation(libs.timber)

    testImplementation(libs.bundles.test.shared)
    // arch-core-testing brings InstantTaskExecutorRule for synchronous LiveData
    // tests; only the repo + ViewModel tests need it, so keep it out of the
    // shared bundle.
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.bundles.android.test)
}
