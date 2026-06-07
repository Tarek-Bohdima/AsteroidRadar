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
package com.tarek.asteroidradar.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

// Issue #178: NASA's APIs (especially planetary/apod) intermittently return
// transient 5xx / drop the connection — observed 503 then 200 on the very next
// call. A single-attempt fetch leaves the user on the empty-cache placeholder
// (no photo) until the next cold start. Retrying here on the shared OkHttpClient
// covers both endpoints and both call sites (MainViewModel + RefreshDataWorker)
// in one place.
//
// 429 (rate limit) and other 4xx are deliberately NOT retried — hammering a
// rate-limited or client-error request only makes it worse. Both NASA endpoints
// are idempotent GETs, so replaying the request is safe.
internal const val DEFAULT_MAX_RETRIES = 3
internal const val RETRY_BACKOFF_STEP_MS = 300L
private val RETRYABLE_CODES = setOf(500, 502, 503, 504)

class RetryInterceptor(
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
    // Injected so unit tests can run without real sleeps; production uses
    // Thread.sleep on OkHttp's dispatcher thread (calls are async/enqueued for
    // Retrofit suspend functions, so this never blocks the main thread).
    private val backoffMs: (attempt: Int) -> Long = { attempt -> attempt * RETRY_BACKOFF_STEP_MS },
    private val sleep: (Long) -> Unit = { millis -> Thread.sleep(millis) },
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempt = 0
        while (true) {
            try {
                val response = chain.proceed(request)
                if (response.code in RETRYABLE_CODES && attempt < maxRetries) {
                    // Must close the body before re-issuing or the connection leaks.
                    response.close()
                    attempt++
                    sleep(backoffMs(attempt))
                    continue
                }
                return response
            } catch (e: IOException) {
                if (attempt >= maxRetries) throw e
                attempt++
                sleep(backoffMs(attempt))
            }
        }
    }
}
