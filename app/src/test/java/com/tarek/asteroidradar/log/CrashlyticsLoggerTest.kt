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
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.io.IOException

// Concrete LogEvent subtypes only: same constraint as TimberLoggerTest
// (sealed-class hierarchies can't be extended from the unit-test source set —
// Kotlin treats it as a different module). The `Error` priority `when` arm
// is therefore not covered here — no concrete event uses it yet. The mapping
// is trivial and will be covered organically the first time an Error event
// lands.
class CrashlyticsLoggerTest {
    private lateinit var crashlytics: FirebaseCrashlytics
    private lateinit var logger: CrashlyticsLogger

    @Before
    fun setUp() {
        crashlytics = mockk(relaxed = true)
        logger = CrashlyticsLogger(crashlytics)
    }

    @Test
    fun `Info-level events are dropped (severity floor at Warn)`() {
        logger.log(LogEvent.App.Launched)

        verify(exactly = 0) { crashlytics.log(any()) }
        verify(exactly = 0) { crashlytics.recordException(any()) }
        verify(exactly = 0) { crashlytics.setCustomKey(any(), any<String>()) }
    }

    @Test
    fun `Warn events emit a breadcrumb via log()`() {
        logger.log(LogEvent.Network.RefreshAsteroidsFailed(IOException("network down")))

        verify { crashlytics.log("[Network] refreshAsteroids() failed") }
    }

    @Test
    fun `Warn events do not call recordException`() {
        logger.log(LogEvent.Network.RefreshAsteroidsFailed(IOException("network down")))

        verify(exactly = 0) { crashlytics.recordException(any()) }
    }

    @Test
    fun `Warn events with attributes call setCustomKey for each typed value`() {
        logger.log(
            LogEvent.Work.RefreshDataWorkerFinished(
                durationMs = 1234L,
                outcome = LogEvent.Work.Outcome.Failure,
            ),
        )

        // `outcome` arrives as String, `durationMs` as Long — the when block
        // in CrashlyticsLogger routes each to the matching setCustomKey overload.
        verify { crashlytics.setCustomKey("outcome", "Failure") }
        verify { crashlytics.setCustomKey("durationMs", 1234L) }
    }

    @Test
    fun `tag is wrapped in brackets and prepended to the message`() {
        logger.log(LogEvent.Network.RefreshPictureOfDayFailed(IOException("dns fail")))

        verify { crashlytics.log("[Network] refreshPictureOfDay() failed") }
    }
}
