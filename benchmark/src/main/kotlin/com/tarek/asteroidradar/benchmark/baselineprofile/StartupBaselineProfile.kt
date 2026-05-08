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
package com.tarek.asteroidradar.benchmark.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TARGET_PACKAGE = "com.tarek.asteroidradar"
private const val FIRST_FRAME_TIMEOUT_MS = 5_000L

/**
 * Generates a Baseline Profile for cold-start performance.
 *
 * Running this test on a device (or AGP-managed Gradle device) produces a
 * `baseline-prof.txt` file under
 * `app/build/outputs/managed_device_android_test_additional_output/.../`,
 * which AGP then bundles into the release AAB at `assets/dexopt/baseline.prof`.
 * Play Store delivers the AOT-compiled hot paths to users on first install.
 *
 * The generator follows NIA's minimal `StartupBaselineProfile` pattern: cold
 * start, wait for content to render. Richer per-screen profiles (e.g. detail
 * navigation, scroll) can be added as separate `*BaselineProfile.kt` files
 * if their hot paths warrant it.
 *
 * `includeInStartupProfile = true` enables AGP's [Dex Layout Optimizations]
 * (https://developer.android.com/topic/performance/baselineprofiles/dex-layout-optimizations),
 * which cluster the hottest classes together in the resulting DEX file for
 * faster cold-start class loading.
 */
@RunWith(AndroidJUnit4::class)
class StartupBaselineProfile {
    @get:Rule
    val rule = BaselineProfileRule()

    /** Generates the cold-start baseline profile. Method named `generate` to
     * match NIA's pattern (single entry-point per profile class) and to dodge
     * the same D8-no-spaces-in-method-names constraint documented in
     * `StartupBenchmark`'s KDoc. */
    @Test
    fun generate() =
        rule.collect(
            packageName = TARGET_PACKAGE,
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
            // Wait for the LazyColumn to paint at least one asteroid row before
            // ending the profile run — without this, the profile only captures
            // up to the activity's first frame, missing the Compose render path
            // that's the actual hot surface we want to AOT-compile.
            device.wait(Until.hasObject(By.descContains("Asteroid")), FIRST_FRAME_TIMEOUT_MS)
        }
}
