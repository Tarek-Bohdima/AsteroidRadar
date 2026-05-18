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
// buildTypes).
plugins {
    id("asteroidradar.android.application")
    id("asteroidradar.android.compose")
    id("asteroidradar.android.hilt")
    // Baseline profile consumer side. Pairs with the `:benchmark` module's
    // `androidx.baselineprofile` plugin (Phase 14a) — the dependency
    // `baselineProfile(projects.benchmark)` below is what AGP wires through
    // when bundling the generated `baseline-prof.txt` into the release AAB.
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.kover)
}

// Phase 15b — Firebase plugins applied conditionally on `google-services.json`
// presence. Keeps debug builds working for contributors who haven't set up
// Firebase yet; release builds require the file (the fail-fast in the task
// graph below produces a clearer message than the plugin's own error). The
// plugin Maven coordinates are still on the buildscript classpath via the
// `apply false` aliases in the root build script.
val googleServicesJson = file("google-services.json")
if (googleServicesJson.exists()) {
    apply(
        plugin =
            libs.plugins.google.services
                .get()
                .pluginId,
    )
    apply(
        plugin =
            libs.plugins.firebase.crashlytics
                .get()
                .pluginId,
    )
}

// Version components — bump these (not versionCode / versionName directly) when
// cutting a release. Classifier choices: INTERNAL, ALPHA, BETA, RC, RELEASE.
// .github/workflows/release.yml greps these names, so don't rename them.
val versionMajor = 4
val versionMinor = 0
val versionPatch = 3
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
        // Phase 15b — release builds need google-services.json present so the
        // google-services plugin generates the per-variant resources Firebase
        // Crashlytics reads at SDK init. Fail at task-graph-ready time with a
        // pointer to the README rather than the plugin's terse default error.
        require(googleServicesJson.exists()) {
            "app/google-services.json is required for release builds. " +
                "Download from console.firebase.google.com or decode " +
                "GOOGLE_SERVICES_JSON_BASE64 via CI — see README."
        }
    }
}

// Kover filters + verification floor. The 60% target lives in
// docs/IMPROVEMENT_PLAN.md as the Phase 7 follow-up; the filters here scope
// it to JVM-testable production code (parser, repo, domain, ViewModel).
// Generated Hilt/Room code and pure-UI classes (Activity / Composables /
// Worker) are excluded — they need on-device smoke, not unit tests, and
// drown the signal at unfiltered totals otherwise.
kover {
    reports {
        filters {
            excludes {
                packages(
                    // DI bindings (DatabaseModule + generated). No logic to exercise.
                    "com.tarek.asteroidradar.di",
                    // UI not covered by JVM tests — Compose UI test territory.
                    "com.tarek.asteroidradar.ui.detail",
                    "com.tarek.asteroidradar.ui.theme",
                    "com.tarek.asteroidradar.util",
                    "com.tarek.asteroidradar.work",
                    // Hilt aggregator packages. Each is exact — no wildcard support in packages().
                    "dagger.hilt.internal.aggregatedroot.codegen",
                    "dagger.hilt.internal.processedrootsentinel.codegen",
                    "hilt_aggregated_deps",
                )
                classes(
                    // Top-level Android entry points (in com.tarek.asteroidradar package alongside
                    // subpackages, so listed by FQN rather than via packages()).
                    "com.tarek.asteroidradar.AsteroidRadarApplication",
                    "com.tarek.asteroidradar.AsteroidRadarApplication\$*",
                    "com.tarek.asteroidradar.MainActivity",
                    "com.tarek.asteroidradar.MainActivity\$*",
                    // Phase 9c typed Nav-Compose routes — kotlinx-serialization synthesises
                    // a companion + `$serializer` per @Serializable, inflating instruction
                    // count without a JVM-unit-testable surface. Round-trip is exercised on
                    // device by MainScreenTest + DetailScreenTest.
                    "com.tarek.asteroidradar.MainRoute",
                    "com.tarek.asteroidradar.MainRoute\$*",
                    "com.tarek.asteroidradar.DetailRoute",
                    "com.tarek.asteroidradar.DetailRoute\$*",
                    // ui.main retains MainViewModel; MainScreen + the typed-route NavType are
                    // Compose-only surfaces. Each @Composable / anonymous object emits inner
                    // classes (`*Kt$Foo$1`, `ComposableSingletons$*Kt`, NavType anon subclass)
                    // that the bare-FQN exclude misses — wildcard the children.
                    "com.tarek.asteroidradar.ui.main.MainScreenKt",
                    "com.tarek.asteroidradar.ui.main.MainScreenKt\$*",
                    "com.tarek.asteroidradar.ui.main.ComposableSingletons\$MainScreenKt",
                    "com.tarek.asteroidradar.ui.main.ComposableSingletons\$MainScreenKt\$*",
                    "com.tarek.asteroidradar.ui.main.AsteroidNavTypeKt",
                    "com.tarek.asteroidradar.ui.main.AsteroidNavTypeKt\$*",
                    // Compose's lambda-singleton holder for MainActivity's setContent block.
                    "com.tarek.asteroidradar.ComposableSingletons",
                    "com.tarek.asteroidradar.ComposableSingletons\$*",
                    // network: trim the un-unit-testable surfaces. The Retrofit service
                    // interface, the converter factory, and the DTO/POJO file all need
                    // MockWebServer or a wired Retrofit instance to exercise — out of scope
                    // for the JVM test bundle. Parser (NetworkUtilsKt) stays in.
                    "com.tarek.asteroidradar.network.AsteroidApi",
                    "com.tarek.asteroidradar.network.AsteroidApi\$*",
                    "com.tarek.asteroidradar.network.AsteroidService",
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
                    "**_ComponentTreeDeps",
                    "**Hilt_*",
                    "**_GeneratedInjector",
                    "**_MembersInjector",
                    "**_Impl",
                    "**_Impl\$*",
                    // kotlinx-serialization synthesises a `$$serializer` per @Serializable
                    // class — pure descriptor + encode/decode bytecode, no JVM-testable
                    // surface (round-trip is exercised by the on-device DetailScreenTest).
                    "**\$\$serializer",
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
            // SYMBOL_TABLE bundles function names from transitive Compose / AndroidX
            // .so libs into BUNDLE-METADATA/com.android.tools.build.debugsymbols/,
            // silencing Play Console's "no debug symbols" warning. FULL would also
            // include source-line info but bloats the AAB 5–20 MB for libraries we
            // don't own; symbol tables alone make native-frame crashes readable.
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
        }
    }

    androidResources {
        generateLocaleConfig = true
    }
}

dependencies {
    implementation(fileTree("libs") { include("*.jar") })

    implementation(libs.androidx.appcompat)

    implementation(libs.bundles.networking)

    // kotlinx-coroutines-android is the Dispatchers.Main Android impl, loaded
    // by ServiceLoader at runtime — no compile-time symbols are referenced
    // (Dispatchers.Main resolves through kotlinx-coroutines-core).
    runtimeOnly(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    // androidx-hilt:hilt-work is a separate artifact group from the dagger-hilt
    // ones (which the convention plugin wires); the matching compiler is also
    // distinct and goes through KSP, not kapt.
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Hilt's compiler + the kotlin-metadata-jvm pin are wired in the
    // `asteroidradar.android.hilt` convention plugin's `ksp(...)` block; no
    // module-side declaration needed.

    implementation(libs.timber)

    // Phase 15b — Firebase Crashlytics. Pulled with `implementation` (not
    // `releaseImplementation`) so `CrashlyticsLogger` + `FirebaseModule`
    // compile in both build types and unit tests in the default `src/test/`
    // source set can construct the logger with a mocked FirebaseCrashlytics.
    // `FirebaseCrashlytics.getInstance()` is never invoked at runtime in
    // debug because the @IntoSet binding for CrashlyticsLogger lives in
    // `LoggerReleaseModule` (in `app/src/release/...`), so the debug Hilt
    // graph never asks for it. APK size impact on debug is ~1-2 MB; the
    // release-only binding is the actual Gate 1 (build-type filtering).
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics.sdk)

    // Baseline profile producer. The `:benchmark` module generates
    // `baseline-prof.txt` via its `StartupBaselineProfile`; AGP wires the
    // output into this AAB at `assets/dexopt/baseline.prof` automatically
    // because of the `baselineprofile` plugin applied above. Phase 14b.
    baselineProfile(projects.benchmark)

    // Compose runtime: BOM aligns the androidx.compose.* artifacts; the bundle
    // pulls in UI / Material 3 / activity-compose / lifecycle-runtime-compose /
    // navigation-compose / hilt-navigation-compose / coil-compose. The
    // ui-tooling sibling is debug-only (Layout Inspector + @Preview rendering).
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // ui-test-manifest hosts the empty test activity Compose tests render
    // into; lives on debugRuntimeOnly because it's a manifest contributor
    // (no symbols are referenced from code) and shouldn't be on the compile
    // classpath.
    debugRuntimeOnly(libs.androidx.compose.ui.test.manifest)

    // Mans0n compose-rules ruleset for detekt — Compose-specific checks
    // (slot-table correctness, hoisting, parameter ordering, modifier handling).
    detektPlugins(libs.detekt.compose.rules)

    testImplementation(libs.bundles.test.shared)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.bundles.android.test)
    // AsteroidDao integration tests collect Flow (`first()`), exercise a
    // suspend `deletePreviousAsteroid` (runTest), and assert with Truth.
    // Mirrors the testImplementation entries above on the instrumented
    // classpath. Kept off the `android-test` bundle so the Espresso-only
    // smoke tests don't pull deps they don't need.
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.truth)
    // Compose UI tests — BOM-aligned, applied alongside the platform() pin so
    // the `ui-test-junit4` artifact stays in lockstep with the rest of the
    // androidx.compose.* dependencies.
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // Only Espresso surface still wired — MainScreenTest's ACTION_VIEW assertion.
    androidTestImplementation(libs.androidx.test.espresso.intents)
}
