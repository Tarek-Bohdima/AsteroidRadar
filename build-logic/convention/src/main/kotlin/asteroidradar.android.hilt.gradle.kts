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

import dagger.hilt.android.plugin.HiltExtension
import org.gradle.api.artifacts.VersionCatalogsExtension

// Standalone Hilt convention. Applied alongside `asteroidradar.android.application`
// in :app today; future feature/library modules can opt into DI by adding this
// plugin alone (their `*.android.library` convention will bring KSP).
//
// `enableAggregatingTask = true` has been Hilt's default since 2.45 — kept
// explicit so the intent travels with the plugin rather than relying on an
// upstream default that could flip.
//
// KSP is re-declared here even though the application convention already
// applies it; Gradle's plugin manager dedupes by id, and keeping it here makes
// this convention work in a module that doesn't bring KSP itself.
//
// Catalog access uses `VersionCatalogsExtension` rather than the
// `LibrariesForLibs` typed accessor — the latter isn't generated for the
// build-logic compileKotlin classpath without an extra workaround, and the
// public API stays portable across Gradle minors.
plugins {
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

configure<HiltExtension> {
    enableAggregatingTask = true
}

private val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    "implementation"(libs.findLibrary("hilt-android").get())
    "ksp"(libs.findLibrary("hilt-compiler").get())
}
