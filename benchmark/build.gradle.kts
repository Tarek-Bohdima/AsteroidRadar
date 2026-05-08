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
plugins {
    id("com.android.test")
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.tarek.asteroidradar.benchmark"
    compileSdk = 36

    defaultConfig {
        // Macrobenchmark requires API 23+; baseline-profile generation needs
        // API 28+ for compile-mode control. minSdk 28 stays well above
        // :app's minSdk 26 — the gap is irrelevant since this module never
        // ships to users.
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Managed Gradle Device — AGP provisions a Pixel 7 / API 34 / aosp AVD on
    // demand. Keeps benchmark numbers consistent across local + CI runs (the
    // operating-principle reason NIA does the same). Disable in `baselineProfile`
    // below to opt out of any connected device the developer might have plugged
    // in — GMD-only is the contract.
    testOptions.managedDevices.localDevices {
        create("pixel7Api34") {
            device = "Pixel 7"
            apiLevel = 34
            systemImageSource = "aosp"
        }
    }

    // Macrobenchmark + the baseline-profile generator both target :app at
    // runtime; AGP needs the path explicitly since `com.android.test` modules
    // don't auto-discover their target.
    targetProjectPath = ":app"

    // Self-instrumenting lets the test process run in the same APK install as
    // the target — required by Macrobenchmark since API 30 to avoid a separate
    // test-only APK on the device. Unstable AGP API as of 9.x; flagged here so
    // a future AGP bump that promotes this can drop the marker.
    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

baselineProfile {
    // GMD-only execution. Avoids the developer's connected device (if any)
    // from skewing the profile output across machines.
    managedDevices.clear()
    managedDevices += "pixel7Api34"
    useConnectedDevices = false
}

dependencies {
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.rules)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
}
