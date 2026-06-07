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

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.junit.function.ThrowingRunnable
import java.io.IOException

class RetryInterceptorTest {
    private val request = Request.Builder().url("https://example.invalid/apod").build()

    // sleep is a no-op so the test never actually waits out the backoff.
    private val interceptor = RetryInterceptor(maxRetries = 3, sleep = {})

    private fun response(code: Int): Response =
        Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test-$code")
            .body("".toResponseBody(null))
            .build()

    private fun chainReturning(answer: (Int) -> Response): Interceptor.Chain {
        val chain = mockk<Interceptor.Chain>(relaxed = true)
        every { chain.request() } returns request
        var call = 0
        every { chain.proceed(any()) } answers { answer(++call) }
        return chain
    }

    @Test
    fun `retries on a retryable 5xx then returns the successful response`() {
        // Given the endpoint returns 503 once, then 200 (the observed APOD flap)
        val chain = chainReturning { call -> if (call == 1) response(503) else response(200) }

        // When the request is intercepted
        val result = interceptor.intercept(chain)

        // Then it retried once and surfaced the 200
        assertThat(result.code).isEqualTo(200)
        verify(exactly = 2) { chain.proceed(any()) }
    }

    @Test
    fun `retries on IOException then returns the successful response`() {
        // Given the first attempt drops the connection, the second succeeds
        val chain = chainReturning { call -> if (call == 1) throw IOException("reset") else response(200) }

        // When the request is intercepted
        val result = interceptor.intercept(chain)

        // Then the transient IOException was retried and the 200 surfaced
        assertThat(result.code).isEqualTo(200)
        verify(exactly = 2) { chain.proceed(any()) }
    }

    @Test
    fun `does not retry on 429 rate limit`() {
        // Given the endpoint rate-limits the request
        val chain = chainReturning { response(429) }

        // When the request is intercepted
        val result = interceptor.intercept(chain)

        // Then the 429 is returned as-is with no retry (retrying would worsen it)
        assertThat(result.code).isEqualTo(429)
        verify(exactly = 1) { chain.proceed(any()) }
    }

    @Test
    fun `does not retry on a 4xx client error`() {
        // Given a client error
        val chain = chainReturning { response(404) }

        // When the request is intercepted
        val result = interceptor.intercept(chain)

        // Then it is returned as-is with no retry
        assertThat(result.code).isEqualTo(404)
        verify(exactly = 1) { chain.proceed(any()) }
    }

    @Test
    fun `gives up after the retry cap and returns the last 5xx`() {
        // Given every attempt returns 503
        val chain = chainReturning { response(503) }

        // When the request is intercepted
        val result = interceptor.intercept(chain)

        // Then it tried 1 + maxRetries(3) = 4 times and returned the final 503
        assertThat(result.code).isEqualTo(503)
        verify(exactly = 4) { chain.proceed(any()) }
    }

    @Test
    fun `rethrows the IOException after exhausting retries`() {
        // Given every attempt throws
        val chain = chainReturning { throw IOException("always down") }

        // When the request is intercepted, Then the IOException propagates after
        // 1 + maxRetries(3) = 4 attempts
        org.junit.Assert.assertThrows(
            IOException::class.java,
            ThrowingRunnable { interceptor.intercept(chain) },
        )
        verify(exactly = 4) { chain.proceed(any()) }
    }
}
