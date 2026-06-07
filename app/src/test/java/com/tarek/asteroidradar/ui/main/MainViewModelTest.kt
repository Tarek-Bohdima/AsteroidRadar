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
package com.tarek.asteroidradar.ui.main

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tarek.asteroidradar.domain.Asteroid
import com.tarek.asteroidradar.domain.PictureOfDay
import com.tarek.asteroidradar.log.Logger
import com.tarek.asteroidradar.repository.AsteroidRepository
import com.tarek.asteroidradar.repository.AsteroidRepository.AsteroidsFilter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AsteroidRepository
    private lateinit var logger: Logger
    private lateinit var pictureOfDayFlow: MutableStateFlow<PictureOfDay?>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        pictureOfDayFlow = MutableStateFlow(null)
        // The init block kicks off both refresh paths; default both to no-op
        // so each test only stubs what it cares about. getPictureOfDay() is
        // the cache read the UI subscribes to.
        coEvery { repository.refreshAsteroids() } returns Unit
        coEvery { repository.refreshPictureOfDay() } returns Unit
        every { repository.getPictureOfDay() } returns pictureOfDayFlow
        every { repository.getAsteroidSelection(any()) } returns MutableStateFlow(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init triggers parallel refresh of asteroids and APOD`() =
        runTest(testDispatcher) {
            val viewModel = MainViewModel(repository, logger)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.refreshAsteroids() }
            coVerify(exactly = 1) { repository.refreshPictureOfDay() }
            // Initial value before the cache emits.
            assertThat(viewModel.imageOfTheDay.value).isNull()
        }

    @Test
    fun `imageOfTheDay mirrors the cached APOD flow`() =
        runTest(testDispatcher) {
            val viewModel = MainViewModel(repository, logger)

            viewModel.imageOfTheDay.test {
                assertThat(awaitItem()).isNull()

                pictureOfDayFlow.value = SAMPLE_IMAGE
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(SAMPLE_IMAGE)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refreshPictureOfDay error path is swallowed`() =
        runTest(testDispatcher) {
            coEvery { repository.refreshPictureOfDay() } throws RuntimeException("boom")

            val viewModel = MainViewModel(repository, logger)
            advanceUntilIdle()

            // Failure must not propagate; the cache flow is the source of truth.
            assertThat(viewModel.imageOfTheDay.value).isNull()
        }

    @Test
    fun `updateFilters propagates to filter and re-subscribes asteroids`() =
        runTest(testDispatcher) {
            val today = MutableStateFlow<List<Asteroid>>(emptyList())
            val week = MutableStateFlow<List<Asteroid>>(emptyList())
            val stored = MutableStateFlow<List<Asteroid>>(emptyList())
            every { repository.getAsteroidSelection(AsteroidsFilter.STORED) } returns stored
            every { repository.getAsteroidSelection(AsteroidsFilter.TODAY) } returns today
            every { repository.getAsteroidSelection(AsteroidsFilter.WEEK) } returns week

            val viewModel = MainViewModel(repository, logger)
            advanceUntilIdle()

            // Collect the StateFlow so flatMapLatest re-evaluates on each filter
            // flip — the upstream stays unsubscribed otherwise. Turbine's `test`
            // takes care of subscribe/unsubscribe lifecycle.
            viewModel.asteroids.test {
                assertThat(awaitItem()).isEmpty()

                viewModel.updateFilters(AsteroidsFilter.TODAY)
                today.value = listOf(SAMPLE_ASTEROID)
                advanceUntilIdle()
                assertThat(viewModel.filter.value).isEqualTo(AsteroidsFilter.TODAY)
                assertThat(awaitItem()).containsExactly(SAMPLE_ASTEROID)

                viewModel.updateFilters(AsteroidsFilter.WEEK)
                week.value = emptyList()
                advanceUntilIdle()
                assertThat(viewModel.filter.value).isEqualTo(AsteroidsFilter.WEEK)
                // flatMapLatest emits the new flow's first value on switch.
                assertThat(awaitItem()).isEmpty()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refresh toggles isRefreshing true then false`() =
        runTest(testDispatcher) {
            // Gate the asteroid refresh so the spinner stays on long enough to
            // observe — without it the StateFlow would conflate true→false.
            val gate = CompletableDeferred<Unit>()
            coEvery { repository.refreshAsteroids() } coAnswers { gate.await() }
            val viewModel = MainViewModel(repository, logger)
            advanceUntilIdle()

            viewModel.isRefreshing.test {
                assertThat(awaitItem()).isFalse() // idle

                viewModel.refresh()
                advanceUntilIdle()
                assertThat(awaitItem()).isTrue() // spinner on, refresh in flight

                gate.complete(Unit)
                advanceUntilIdle()
                assertThat(awaitItem()).isFalse() // spinner cleared when both finish

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `refresh reloads both the picture of the day and the asteroid feed`() =
        runTest(testDispatcher) {
            val viewModel = MainViewModel(repository, logger)
            advanceUntilIdle() // init refreshes each once

            viewModel.refresh()
            advanceUntilIdle()

            // Once from init + once from the manual refresh = twice each.
            coVerify(exactly = 2) { repository.refreshAsteroids() }
            coVerify(exactly = 2) { repository.refreshPictureOfDay() }
        }

    @Test
    fun `refresh still clears isRefreshing when a refresh fails`() =
        runTest(testDispatcher) {
            // The VM swallows the failure internally, so the finally still runs.
            coEvery { repository.refreshAsteroids() } throws RuntimeException("boom")
            val viewModel = MainViewModel(repository, logger)
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(viewModel.isRefreshing.value).isFalse()
        }

    private companion object {
        val SAMPLE_IMAGE =
            PictureOfDay(
                mediaType = "image",
                title = "Test image",
                url = "https://example.invalid/apod.jpg",
            )
        val SAMPLE_ASTEROID =
            Asteroid(
                id = 42L,
                codename = "(2024 ZZ1)",
                closeApproachDate = "2024-01-01",
                absoluteMagnitude = 19.5,
                estimatedDiameter = 0.42,
                relativeVelocity = 11.7,
                distanceFromEarth = 0.025,
                isPotentiallyHazardous = true,
            )
    }
}
