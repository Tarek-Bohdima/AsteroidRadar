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
package com.tarek.asteroidradar.log

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

// The Logcat sink. Routes every event through Timber so the existing
// DebugTree planted in AsteroidRadarApplication.onCreate still picks the
// calls up. The TimberLogger is the only place left in `main` that should
// reference `timber.log.Timber.<level>(...)` — all other call sites go
// through Logger.log(LogEvent.*).
@Singleton
class TimberLogger
    @Inject
    constructor() : Logger {
        override fun log(event: LogEvent) {
            val tree = Timber.tag(event.tag)
            val rendered = event.message + event.attributes.toSuffix()
            when (event.priority) {
                LogPriority.Verbose -> tree.v(event.throwable, rendered)
                LogPriority.Debug -> tree.d(event.throwable, rendered)
                LogPriority.Info -> tree.i(event.throwable, rendered)
                LogPriority.Warn -> tree.w(event.throwable, rendered)
                LogPriority.Error -> tree.e(event.throwable, rendered)
            }
        }

        private fun Map<String, Any>.toSuffix(): String =
            if (isEmpty()) {
                ""
            } else {
                entries.joinToString(prefix = " ", separator = " ") { "${it.key}=${it.value}" }
            }
    }
