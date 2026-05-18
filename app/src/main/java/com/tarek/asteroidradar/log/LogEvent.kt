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

// Typed event taxonomy. Each concrete event carries everything a sink needs to
// render the call without the caller having to think about formatting: tag,
// priority, message, optional structured attributes, and an optional cause.
// New domains add nested sealed groups; new events in a domain add concrete
// data classes / objects to that group.
sealed class LogEvent {
    abstract val tag: String
    abstract val priority: LogPriority
    abstract val message: String
    open val attributes: Map<String, Any> = emptyMap()
    open val throwable: Throwable? = null

    sealed class Network : LogEvent() {
        override val tag: String = "Network"

        data class RefreshAsteroidsFailed(
            val cause: Throwable,
        ) : Network() {
            override val priority: LogPriority = LogPriority.Warn
            override val message: String = "refreshAsteroids() failed"
            override val attributes: Map<String, Any> = causeAttributes(cause)
            override val throwable: Throwable? = cause
        }

        data class RefreshPictureOfDayFailed(
            val cause: Throwable,
        ) : Network() {
            override val priority: LogPriority = LogPriority.Warn
            override val message: String = "refreshPictureOfDay() failed"
            override val attributes: Map<String, Any> = causeAttributes(cause)
            override val throwable: Throwable? = cause
        }
    }

    sealed class App : LogEvent() {
        override val tag: String = "App"

        data object Launched : App() {
            override val priority: LogPriority = LogPriority.Info
            override val message: String = "application onCreate"
        }
    }

    sealed class Work : LogEvent() {
        override val tag: String = "Work"

        // Failure here means doWork() returned Result.failure() — a non-retryable
        // outcome. Retry is the WorkManager-managed back-off path; Success is the
        // happy path. The enum is local to Work because no other domain consumes
        // it today.
        enum class Outcome { Success, Retry, Failure }

        data object RefreshDataWorkerStarted : Work() {
            override val priority: LogPriority = LogPriority.Info
            override val message: String = "RefreshDataWorker started"
        }

        data class RefreshDataWorkerFinished(
            val durationMs: Long,
            val outcome: Outcome,
        ) : Work() {
            override val priority: LogPriority =
                if (outcome == Outcome.Success) LogPriority.Info else LogPriority.Warn
            override val message: String = "RefreshDataWorker finished outcome=${outcome.name}"
            override val attributes: Map<String, Any> = mapOf("durationMs" to durationMs)
        }
    }

    private companion object {
        // Pulled out of the Network classes so both events render the same
        // `cause=<message-or-classname>` attribute for downstream sinks.
        fun causeAttributes(cause: Throwable): Map<String, Any> {
            val text = cause.message ?: cause::class.simpleName.orEmpty()
            return mapOf("cause" to text)
        }
    }
}
