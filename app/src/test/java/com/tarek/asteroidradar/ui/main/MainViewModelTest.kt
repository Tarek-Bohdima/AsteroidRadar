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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.common.truth.Truth.assertThat
import com.tarek.asteroidradar.domain.Asteroid
import com.tarek.asteroidradar.domain.PictureOfDay
import com.tarek.asteroidradar.repository.AsteroidRepository
import com.tarek.asteroidradar.repository.AsteroidRepository.AsteroidsFilter
import com.tarek.asteroidradar.testing.getOrAwaitValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AsteroidRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        // The init block calls both refresh paths; default both to no-op so the
        // test owner only stubs what they care about.
        coEvery { repository.refreshAsteroids() } returns Unit
        coEvery { repository.getImageOfTheDay() } returns SAMPLE_IMAGE
        every { repository.getAsteroidSelection(any()) } returns MutableLiveData(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init refreshes asteroids and fetches the image of the day`() =
        runTest(testDispatcher) {
            val viewModel = MainViewModel(repository)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.refreshAsteroids() }
            coVerify(exactly = 1) { repository.getImageOfTheDay() }
            assertThat(viewModel.imageOfTheDay.getOrAwaitValue()).isEqualTo(SAMPLE_IMAGE)
        }

    @Test
    fun `getImageOfTheDay error path is swallowed and leaves no value`() =
        runTest(testDispatcher) {
            coEvery { repository.getImageOfTheDay() } throws RuntimeException("boom")

            val viewModel = MainViewModel(repository)
            advanceUntilIdle()

            assertThat(viewModel.imageOfTheDay.value).isNull()
        }

    @Test
    fun `updateFilters propagates to filter and asteroids switchMap`() =
        runTest(testDispatcher) {
            val today = MutableLiveData<List<Asteroid>>(emptyList())
            val week = MutableLiveData<List<Asteroid>>(emptyList())
            every { repository.getAsteroidSelection(AsteroidsFilter.TODAY) } returns today
            every { repository.getAsteroidSelection(AsteroidsFilter.WEEK) } returns week

            val viewModel = MainViewModel(repository)
            advanceUntilIdle()

            // The asteroids switchMap is lazy — it only re-evaluates while it has
            // an active observer. Keep one attached for the duration of the
            // filter changes so each updateFilters re-triggers the mapping.
            val asteroidsObserver = Observer<List<Asteroid>> { /* drain emissions */ }
            viewModel.asteroids.observeForever(asteroidsObserver)
            try {
                viewModel.updateFilters(AsteroidsFilter.TODAY)
                assertThat(viewModel.filter.value).isEqualTo(AsteroidsFilter.TODAY)
                assertThat(viewModel.asteroids.value).isEmpty()

                viewModel.updateFilters(AsteroidsFilter.WEEK)
                assertThat(viewModel.filter.value).isEqualTo(AsteroidsFilter.WEEK)
                assertThat(viewModel.asteroids.value).isEmpty()
            } finally {
                viewModel.asteroids.removeObserver(asteroidsObserver)
            }

            verify(exactly = 1) { repository.getAsteroidSelection(AsteroidsFilter.TODAY) }
            verify(exactly = 1) { repository.getAsteroidSelection(AsteroidsFilter.WEEK) }
        }

    @Test
    fun `onAsteroidClicked sets navigateToDetail and onAsteroidDetailNavigated clears it`() =
        runTest(testDispatcher) {
            val viewModel = MainViewModel(repository)
            advanceUntilIdle()

            viewModel.onAsteroidClicked(SAMPLE_ASTEROID)
            assertThat(viewModel.navigateToDetail.getOrAwaitValue()).isEqualTo(SAMPLE_ASTEROID)

            viewModel.onAsteroidDetailNavigated()
            assertThat(viewModel.navigateToDetail.value).isNull()
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
