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

// Top-level build file. Plugins are declared here and applied per-module so
// that subprojects compose them via aliases without re-resolving versions.
// DAGP is the exception — its `:buildHealth` aggregator lives at the root,
// while per-subproject application happens through the convention plugin
// (root-only application doesn't reach subprojects whose Android plugin is
// applied via a precompiled script plugin).

// Force a Kotlin 2.3-compatible `kotlin-metadata-jvm` onto the buildscript
// classpath so DAGP's `ExplodeJarTask` can read 2.3-emitted bytecode metadata.
// DAGP 2.18.0 bundles `kotlin-metadata-jvm` 2.1.x; without this constraint it
// fails with "Provided Metadata instance has version 2.3.0, while maximum
// supported version is 2.1.0". Same trick the Hilt convention plugin uses for
// its KSP processor.
buildscript {
    dependencies {
        classpath(libs.kotlin.metadata)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.detekt) apply false
    // Firebase Crashlytics + Google Services plugins — Phase 15b. Registered
    // here `apply false` so app/build.gradle.kts can apply them conditionally
    // (only when `app/google-services.json` exists); the plugin classpath
    // resolution still has to happen at the root.
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.dependency.analysis)
}

// DAGP configuration. The "unused" / "runtime-only" / "compile-only" findings
// remain at fail severity (default); the "use transitive directly" finding is
// silenced because the Compose BOM is the version-of-record for the entire
// androidx.compose.* tree — declaring each transitive Compose internal
// individually would split versions away from the BOM. Same logic for the
// AndroidX / Hilt / Coil / kotlinx transitive layers that AGP pulls in.
dependencyAnalysis {
    issues {
        all {
            onUsedTransitiveDependencies {
                severity("ignore")
            }
            onUnusedDependencies {
                // PeriodicWorkRequestBuilder<T> is the inline-reified Kotlin
                // builder from work-runtime-ktx, used in AsteroidRadarApplication
                // — DAGP's bytecode walk doesn't see inline-reified call sites.
                exclude("androidx.work:work-runtime-ktx")
            }
        }
    }
}

// Spotless applied across every subproject so formatting + license-header
// enforcement is uniform without each module having to opt in. License header
// in `config/spotless/copyright.kt` is enforced as a literal byte-for-byte
// match — not rewritten — because the existing MIT block predates this
// tooling and shouldn't have its `Copyright (c) 2021` line drift to a
// `$YEAR` interpolation.
val ktlintVersion = libs.versions.ktlint.get()
val licenseHeader = file("config/spotless/copyright.kt")

subprojects {
    apply(plugin = "com.diffplug.spotless")

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**", "**/generated/**")
            ktlint(ktlintVersion)
            licenseHeaderFile(licenseHeader)
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            targetExclude("**/build/**")
            ktlint(ktlintVersion)
            // Default `package` delimiter doesn't apply to .gradle.kts files;
            // use the first script-level construct as the marker instead.
            licenseHeaderFile(licenseHeader, "(plugins|import|@file)")
        }
    }
}

// Detekt — static analysis for code-smell-level issues (cyclomatic complexity,
// long methods, magic numbers, exception swallowing, naming, etc.) — the things
// ktlint deliberately doesn't touch. Existing violations live in
// `config/detekt/baseline.xml`; new code is checked against the merged result
// of defaults + `config/detekt.yml` overrides.
val detektConfigFile = file("config/detekt.yml")
val detektBaselineFile = file("config/detekt/baseline.xml")

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(detektConfigFile)
        baseline = detektBaselineFile
        // Layer overrides on top of upstream defaults rather than replacing
        // them; smaller config file, fewer surprises on Detekt bumps.
        buildUponDefaultConfig = true
        // Don't auto-correct on `detekt` — only via `detektFormat` (out of
        // scope here; we already format via Spotless).
        autoCorrect = false
    }
}
