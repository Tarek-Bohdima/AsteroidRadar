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

import com.tarek.asteroidradar.util.Constants
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit service for the NASA NEO + APOD endpoints.
 *
 * `getAsteroids` returns the raw JSON body as `String` (the `/neo/rest/v1/feed`
 * payload is parsed manually in `parseAsteroidsJsonResult` because of its
 * nested-by-date shape). `getImageOfDay` returns a kotlinx-serialization-decoded
 * [ImageOfTheDay].
 *
 * Construction (Retrofit instance, JSON config, converter ordering) lives in
 * `di/NetworkModule.kt`; the service is provided as a `@Singleton` Hilt binding
 * and injected wherever it is consumed.
 */
interface AsteroidService {
    @GET("neo/rest/v1/feed")
    suspend fun getAsteroids(
        @Query(Constants.PARAMETER_API_KEY) key: String,
    ): String

    @GET("planetary/apod")
    suspend fun getImageOfDay(
        @Query(Constants.PARAMETER_API_KEY) key: String,
    ): ImageOfTheDay
}
