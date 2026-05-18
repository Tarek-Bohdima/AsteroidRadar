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
package com.tarek.asteroidradar.di

import com.tarek.asteroidradar.log.CrashlyticsLogger
import com.tarek.asteroidradar.log.Logger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

// Phase 15b — Gate 1 (build-type filtering). This module only lives in the
// `release/` source set, so AGP's source-set merging means Hilt only sees it
// in release builds. The debug Hilt graph keeps the Logger set at just
// TimberLogger (bound in the shared `LoggerModule`); the release Hilt graph
// adds CrashlyticsLogger via the `@IntoSet` contribution below.
//
// No call-site change is required when this module appears — `CompositeLogger`
// injects `Set<@JvmSuppressWildcards Logger>` and iterates whatever the Hilt
// graph supplies for the active build type.
@Module
@InstallIn(SingletonComponent::class)
abstract class LoggerReleaseModule {
    @Binds
    @IntoSet
    abstract fun bindsCrashlyticsLoggerIntoSet(impl: CrashlyticsLogger): Logger
}
