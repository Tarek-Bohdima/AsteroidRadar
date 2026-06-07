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

import android.content.Context
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import com.tarek.asteroidradar.network.AsteroidService
import com.tarek.asteroidradar.network.RetryInterceptor
import com.tarek.asteroidradar.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.time.Duration
import javax.inject.Singleton

// Issue #166: OkHttp's 10s default connect/read timeout was firing on APOD
// fetches when api.nasa.gov was slow (3–5s responses on degraded days). Bumped
// to 20s so flaky-but-reachable responses land in Room instead of failing into
// the cache-less broken-image path.
private val NETWORK_TIMEOUT: Duration = Duration.ofSeconds(20)

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // `ignoreUnknownKeys = true` lets the APOD response evolve (NASA frequently
    // adds fields like `copyright`, `date`, `explanation`, `hdurl`, `service_version`)
    // without us recompiling — only the three fields annotated on `ImageOfTheDay`
    // are required to be present.
    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(NETWORK_TIMEOUT)
            .readTimeout(NETWORK_TIMEOUT)
            // Issue #178: bounded retry on transient NASA 5xx / connection drops
            // (the APOD endpoint flaps 503 → 200). Added as an application
            // interceptor so it sees the final response code and covers both
            // endpoints + both refresh call sites in one place.
            .addInterceptor(RetryInterceptor())
            .build()

    // Builder order matters: Scalars first matches `String` returns and yields
    // the raw response body (for `getAsteroids`, where the parser walks the
    // nested-by-date payload manually); kotlinx-serialization second handles
    // `@Serializable` DTOs (`ImageOfTheDay`). Phase 11 made this the only
    // serialization library across both nav routes and HTTP — fully
    // reflection-free at runtime.
    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        json: Json,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(Constants.BASE_URL)
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAsteroidService(retrofit: Retrofit): AsteroidService = retrofit.create(AsteroidService::class.java)

    // Issue #123: register VideoFrameDecoder so Coil extracts frame 0 from
    // direct .mp4/.webm URLs (e.g. NASA's APOD video days). The application
    // exposes this loader via ImageLoaderFactory, so every AsyncImage call
    // site picks it up without needing per-call wiring.
    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
    ): ImageLoader =
        ImageLoader
            .Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
}
