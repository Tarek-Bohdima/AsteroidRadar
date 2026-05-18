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

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

// The Firebase Crashlytics sink (Phase 15b). Bound into `Set<Logger>` only
// in release builds via `LoggerReleaseModule` (Gate 1 — build-type filtering);
// debug builds never reach this code path.
//
// Severity mapping (Gate 2 — filter floor):
//  - Below `LogPriority.Warn` (Verbose / Debug / Info): drop entirely.
//    Keeps chatter out of Crashlytics's per-session non-fatal cap
//    (~8/session) and 64 KB breadcrumb buffer.
//  - Warn: `Crashlytics.log(...)` breadcrumb. Attached to the next crash
//    or non-fatal report from this session.
//  - Error with throwable: `recordException(...)` (counts as a non-fatal)
//    plus a breadcrumb capturing the rendered tag/message.
//
// Attributes flow to `setCustomKey` so each pair is indexed as a searchable
// field on the next crash. `setCustomKey` accepts the six primitives below
// natively; anything else gets stringified so the call still succeeds.
@Singleton
class CrashlyticsLogger
    @Inject
    constructor(
        private val crashlytics: FirebaseCrashlytics,
    ) : Logger {
        override fun log(event: LogEvent) {
            if (event.priority < LogPriority.Warn) return

            event.attributes.forEach { (key, value) ->
                when (value) {
                    is String -> crashlytics.setCustomKey(key, value)
                    is Int -> crashlytics.setCustomKey(key, value)
                    is Long -> crashlytics.setCustomKey(key, value)
                    is Float -> crashlytics.setCustomKey(key, value)
                    is Double -> crashlytics.setCustomKey(key, value)
                    is Boolean -> crashlytics.setCustomKey(key, value)
                    else -> crashlytics.setCustomKey(key, value.toString())
                }
            }

            val line = "[${event.tag}] ${event.message}"
            crashlytics.log(line)
            if (event.priority == LogPriority.Error) {
                event.throwable?.let { crashlytics.recordException(it) }
            }
        }
    }
