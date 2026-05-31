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

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Test
import java.time.Duration

private val TIMEOUT_20S_MS = Duration.ofSeconds(20).toMillis().toInt()

class NetworkModuleTest {
    @Test
    fun `provideOkHttpClient sets a 20-second connect timeout`() {
        // Given the module's OkHttp provider
        // When invoked
        val client = NetworkModule.provideOkHttpClient()

        // Then the connect timeout is 20 seconds (issue #166 — was 10s default)
        assertThat(client.connectTimeoutMillis).isEqualTo(TIMEOUT_20S_MS)
    }

    @Test
    fun `provideOkHttpClient sets a 20-second read timeout`() {
        // Given the module's OkHttp provider
        // When invoked
        val client = NetworkModule.provideOkHttpClient()

        // Then the read timeout is 20 seconds — covers NASA APOD's 3–5s+ responses
        // on degraded days that previously tripped the 10s default
        assertThat(client.readTimeoutMillis).isEqualTo(TIMEOUT_20S_MS)
    }

    @Test
    fun `provideRetrofit uses the provided OkHttp client`() {
        // Given a sentinel OkHttp client (distinct from the module's default)
        val sentinel = OkHttpClient.Builder().build()

        // When Retrofit is constructed via the module
        val retrofit = NetworkModule.provideRetrofit(sentinel, Json)

        // Then Retrofit's callFactory is the same sentinel instance — proves the
        // 20s-timeout client actually reaches the wire, no parallel default client
        assertThat(retrofit.callFactory()).isSameInstanceAs(sentinel)
    }
}
