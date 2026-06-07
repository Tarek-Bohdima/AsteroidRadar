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
package com.tarek.asteroidradar.work

import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.common.truth.Truth.assertThat
import com.tarek.asteroidradar.log.Logger
import com.tarek.asteroidradar.repository.AsteroidRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

class RefreshDataWorkerTest {
    private lateinit var repository: AsteroidRepository
    private lateinit var logger: Logger
    private lateinit var worker: RefreshDataWorker

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        // doWork() only touches the injected repository + logger; the Context and
        // WorkerParameters are never dereferenced by our override, so relaxed
        // mocks let us exercise the worker on the JVM without a WorkManager runtime.
        worker =
            RefreshDataWorker(
                appContext = mockk(relaxed = true),
                params = mockk<WorkerParameters>(relaxed = true),
                repository = repository,
                logger = logger,
            )
    }

    @Test
    fun `doWork refreshes the asteroid feed and the picture of the day`() =
        runTest {
            // Given a worker run where every refresh succeeds
            // When doWork executes
            val result = worker.doWork()

            // Then it purges stale asteroids, refreshes the feed, AND refreshes the
            // APOD (issue #176: the worker previously skipped the picture of day),
            // and reports success.
            coVerify(exactly = 1) { repository.deletePastAsteroids() }
            coVerify(exactly = 1) { repository.refreshAsteroids() }
            coVerify(exactly = 1) { repository.refreshPictureOfDay() }
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }

    @Test
    fun `doWork still succeeds when the picture-of-day refresh fails internally`() =
        runTest {
            // Given refreshPictureOfDay encounters a network problem — the repository
            // swallows and logs it, so from the worker's view it simply returns
            coEvery { repository.refreshPictureOfDay() } returns Unit

            // When doWork executes with a healthy asteroid refresh
            val result = worker.doWork()

            // Then the APOD refresh does not gate the worker's outcome
            coVerify(exactly = 1) { repository.refreshPictureOfDay() }
            assertThat(result).isEqualTo(ListenableWorker.Result.success())
        }

    @Test
    fun `doWork retries and skips the APOD refresh when the feed throws HttpException`() =
        runTest {
            // Given the asteroid feed refresh fails with a retryable HTTP error
            coEvery { repository.refreshAsteroids() } throws mockk<HttpException>(relaxed = true)

            // When doWork executes
            val result = worker.doWork()

            // Then the worker retries and never reaches the APOD refresh (existing
            // retry behavior is preserved after adding the APOD call)
            assertThat(result).isEqualTo(ListenableWorker.Result.retry())
            coVerify(exactly = 0) { repository.refreshPictureOfDay() }
        }
}
