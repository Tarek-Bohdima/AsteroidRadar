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
 */

import com.android.build.api.dsl.ApplicationExtension

// Composed conventions for any Android application module. SDK / JVM / plugin
// application lives here so feature module #2 can opt in with
// `plugins { id("asteroidradar.android.application") }` and skip 30 lines of
// boilerplate. Module-specific config (namespace, applicationId, version,
// signing, buildTypes) stays in the consuming script.
//
// Phase 9c retired Data Binding + `androidx.navigation.safeargs.kotlin`
// (replaced by Nav-Compose typed routes). The kotlinx-serialization compiler
// plugin is the new entry — required by the @Serializable Asteroid that
// Nav-Compose encodes into route arguments.
//
// `kotlin-kapt` stays applied even though no @BindingAdapter survives, so
// that `kapt(libs.hilt.compiler)` in :app routes Hilt's aggregating step
// through kapt instead of the broken Gradle worker. Hilt 2.52's KSP
// aggregator (`hiltAggregateDeps*`) hits a NoSuchMethodError on
// `ClassName.canonicalName()` once kapt isn't there to pin JavaPoet 1.13
// indirectly: AGP brings JavaPoet 1.10 onto the daemon classloader, Gradle's
// classloader-resolution race lets 1.10 win, and Hilt's bytecode (compiled
// against 1.13) blows up. Hilt's switch to Palantir's javapoet fork lands in
// 2.55, but the 2.55+ classes ship with Kotlin 2.1 metadata that even Gradle
// 8.13's embedded Kotlin (2.0.21) won't read. Until that toolchain bump is
// taken as its own scoped step, kapt's presence quietly steers Hilt around
// the bug — there are no other kapt users left, so this is otherwise a no-op.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

extensions.configure<ApplicationExtension> {
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    // Android Lint runs on every release build and is gated in CI. Existing
    // warnings live in each module's `lint-baseline.xml` (regenerate via
    // `./gradlew :<module>:updateLintBaseline`); new warnings fail the build.
    lint {
        baseline = file("lint-baseline.xml")
        checkReleaseBuilds = true
        abortOnError = true
    }
}

// `kotlinOptions` lives on the Kotlin plugin extension, not the Android one;
// configure it via the project's `tasks.withType<KotlinCompile>()` API so this
// works on Kotlin 1.6.x as well as later majors that move toward
// `compilerOptions {}`. Phase 4's Kotlin bump can swap this out cleanly.
tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}
