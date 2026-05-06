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
    id("asteroidradar.android.compose")
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

// Kover filters + verification floor. The 60% target lives in
// docs/IMPROVEMENT_PLAN.md as the Phase 7 follow-up; the filters here scope
// it to JVM-testable production code (parser, repo, domain, ViewModel).
// Generated code (DataBinding, Hilt, Room *_Impl, safe-args) and pure-UI
// classes (Activity / Fragments / Adapter / Worker / BindingAdapters) are
// excluded — they need Espresso or on-device smoke, not unit tests, and
// drown the signal at unfiltered totals (~11% instructions otherwise).
kover {
    reports {
        filters {
            excludes {
                packages(
                    // DataBinding scaffold packages (generated alongside the @{} expressions).
                    "androidx.databinding",
                    "androidx.databinding.library.baseAdapters",
                    "com.tarek.asteroidradar.databinding",
                    "com.tarek.asteroidradar.generated.callback",
                    // DI bindings (DatabaseModule + generated). No logic to exercise.
                    "com.tarek.asteroidradar.di",
                    // UI not covered by JVM tests — Phase 8+ Espresso /
                    // Compose UI test territory. Phase 9b folded the surviving
                    // Composables into ui.detail; the temporary ui.compose
                    // package from 9a is gone.
                    "com.tarek.asteroidradar.ui.detail",
                    "com.tarek.asteroidradar.util",
                    "com.tarek.asteroidradar.work",
                    // Hilt aggregator packages. Each is exact — no wildcard support in packages().
                    "dagger.hilt.internal.aggregatedroot.codegen",
                    "hilt_aggregated_deps",
                )
                classes(
                    // Top-level Android entry points (in com.tarek.asteroidradar package alongside
                    // subpackages, so listed by FQN rather than via packages()).
                    "com.tarek.asteroidradar.AsteroidRadarApplication",
                    "com.tarek.asteroidradar.AsteroidRadarApplication\$*",
                    "com.tarek.asteroidradar.MainActivity",
                    "com.tarek.asteroidradar.MainActivity\$*",
                    "com.tarek.asteroidradar.DataBinderMapperImpl",
                    "com.tarek.asteroidradar.DataBindingTriggerClass",
                    // ui.main retains MainViewModel; everything else in the package is UI / generated.
                    "com.tarek.asteroidradar.ui.main.MainFragment",
                    "com.tarek.asteroidradar.ui.main.MainFragment\$*",
                    "com.tarek.asteroidradar.ui.main.AsteroidAdapter",
                    "com.tarek.asteroidradar.ui.main.AsteroidAdapter\$*",
                    "com.tarek.asteroidradar.ui.main.AsteroidListener",
                    // [sic] — typo in the source class name predates this PR.
                    "com.tarek.asteroidradar.ui.main.AsteriodDiffCallback",
                    // network: trim the un-unit-testable surfaces. The Retrofit service
                    // interface, the converter factory, and the DTO/POJO file all need
                    // MockWebServer or a wired Retrofit instance to exercise — out of scope
                    // for the JVM test bundle. Parser (NetworkUtilsKt) stays in.
                    "com.tarek.asteroidradar.network.AsteroidApi",
                    "com.tarek.asteroidradar.network.AsteroidApi\$*",
                    "com.tarek.asteroidradar.network.AsteroidApiService",
                    "com.tarek.asteroidradar.network.HandleScalarAndJsonConverterFactory",
                    "com.tarek.asteroidradar.network.HandleScalarAndJsonConverterFactory\$*",
                    "com.tarek.asteroidradar.network.ScalarResponse",
                    "com.tarek.asteroidradar.network.JsonResponse",
                    "com.tarek.asteroidradar.network.ImageOfTheDay",
                    "com.tarek.asteroidradar.network.DataTransferObjectsKt",
                    // Generated code patterns. `**` matches any chars including dots so the
                    // pattern catches cross-package classes; plain `*` only matches within a
                    // single FQN segment in Kover's globs (which is why `Hilt_*` alone misses
                    // `com.tarek.asteroidradar.Hilt_MainActivity`).
                    "**_Factory",
                    "**_Factory\$*",
                    "**_HiltModules*",
                    "**_HiltComponents*",
                    "**Hilt_*",
                    "**_GeneratedInjector",
                    "**_MembersInjector",
                    "**_Impl",
                    "**_Impl\$*",
                    "**BindingImpl",
                    "**BindingImpl\$*",
                    "**.BR",
                    "**Args",
                    "**Directions",
                    "**.BuildConfig",
                )
            }
        }
        verify {
            rule {
                // INSTRUCTION is the most refactor-resilient coverage metric
                // (line counts shift with formatting; method/class are too coarse).
                bound {
                    minValue = 60
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.INSTRUCTION
                }
            }
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
            isMinifyEnabled = true
            isShrinkResources = true
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

    implementation(libs.androidx.activity.ktx)
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

    // Compose runtime: BOM aligns the androidx.compose.* artifacts; the bundle
    // pulls in UI / Material 3 / activity-compose / lifecycle-runtime-compose /
    // navigation-compose / hilt-navigation-compose / coil-compose. The
    // ui-tooling sibling is debug-only (Layout Inspector + @Preview rendering).
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // ui-test-manifest hosts the empty test activity Compose tests render
    // into; lives on debugImplementation so it's only present on
    // androidTestDebug, never in release.
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Mans0n compose-rules ruleset for detekt — Compose-specific checks
    // (slot-table correctness, hoisting, parameter ordering, modifier handling).
    detektPlugins(libs.detekt.compose.rules)

    testImplementation(libs.bundles.test.shared)
    // arch-core-testing brings InstantTaskExecutorRule for synchronous LiveData
    // tests; only the repo + ViewModel tests need it, so keep it out of the
    // shared bundle.
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.bundles.android.test)
    // AsteroidDao integration tests observe LiveData (InstantTaskExecutorRule),
    // exercise a suspend `deletePreviousAsteroid` (runTest), and assert with
    // Truth. Mirrors the testImplementation entries above on the instrumented
    // classpath. Kept off the `android-test` bundle so the Espresso-only smoke
    // tests don't pull deps they don't need.
    androidTestImplementation(libs.androidx.arch.core.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.truth)
    // Compose UI tests — BOM-aligned, applied alongside the platform() pin so
    // the `ui-test-junit4` artifact stays in lockstep with the rest of the
    // androidx.compose.* dependencies.
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
