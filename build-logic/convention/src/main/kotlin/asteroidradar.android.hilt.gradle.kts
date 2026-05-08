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

import org.gradle.api.artifacts.VersionCatalogsExtension

// Standalone Hilt convention. Applied alongside `asteroidradar.android.application`
// in :app today; future feature/library modules can opt into DI by adding this
// plugin alone (their `*.android.library` convention will bring KSP).
//
// Phase 13a aligned the Hilt setup with Now in Android: compiler wired through
// KSP (kapt is gone), and `kotlin-metadata-jvm` is pinned explicitly on the
// KSP classpath so Hilt's processor reads Kotlin 2.3-emitted bytecode metadata
// (the bundled metadata library inside Hilt is older). `enableAggregatingTask`
// stays at its default `true` — Hilt 2.59+ uses Palantir's javapoet fork, so
// the JavaPoet 1.10 NoSuchMethodError that Phase 9c worked around is gone.
//
// Catalog access uses `VersionCatalogsExtension` rather than the
// `LibrariesForLibs` typed accessor — the latter isn't generated for the
// build-logic compileKotlin classpath without an extra workaround, and the
// public API stays portable across Gradle minors.
plugins {
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

private val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    "implementation"(libs.findLibrary("hilt-android").get())
    "ksp"(libs.findLibrary("hilt-compiler").get())
    "ksp"(libs.findLibrary("kotlin-metadata").get())
}

