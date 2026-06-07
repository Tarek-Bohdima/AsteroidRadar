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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tarek.asteroidradar.domain.PictureOfDay
import com.tarek.asteroidradar.log.LogEvent
import com.tarek.asteroidradar.log.Logger
import com.tarek.asteroidradar.repository.AsteroidRepository
import com.tarek.asteroidradar.repository.AsteroidRepository.AsteroidsFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val repository: AsteroidRepository,
        private val logger: Logger,
    ) : ViewModel() {
        // Issue #116: APOD is sourced from Room, not the network. The cached
        // row paints immediately on cold start; refreshPictureOfDay() runs in
        // parallel and the new row swaps in via Coil's crossfade once it lands.
        val imageOfTheDay: StateFlow<PictureOfDay?> =
            repository
                .getPictureOfDay()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                    initialValue = null,
                )

        private val _filter = MutableStateFlow<AsteroidsFilter>(AsteroidsFilter.STORED)
        val filter: StateFlow<AsteroidsFilter> = _filter.asStateFlow()

        // Issue #180: drives the swipe-to-refresh spinner. True for the duration
        // of a user-initiated refresh, reset in a finally so a swallowed network
        // failure still clears the indicator.
        private val _isRefreshing = MutableStateFlow(false)
        val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

        // Re-evaluates the DAO query whenever the filter flips. WhileSubscribed
        // with a 5s grace handles configuration-change hand-off without
        // resubscribing the underlying Room flow on every recomposition.
        val asteroids: StateFlow<List<com.tarek.asteroidradar.domain.Asteroid>> =
            _filter
                .flatMapLatest { repository.getAsteroidSelection(it) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                    initialValue = emptyList(),
                )

        init {
            // Two independent launches so the APOD round-trip doesn't queue
            // behind the NeoWs feed (the latency users have flagged on every
            // v3.x verification).
            viewModelScope.launch { refreshAsteroids() }
            viewModelScope.launch { refreshPictureOfDay() }
        }

        private suspend fun refreshPictureOfDay() {
            try {
                repository.refreshPictureOfDay()
            } catch (e: Exception) {
                logger.log(LogEvent.Network.RefreshPictureOfDayFailed(e))
            }
        }

        private suspend fun refreshAsteroids() {
            try {
                repository.refreshAsteroids()
            } catch (e: Exception) {
                logger.log(LogEvent.Network.RefreshAsteroidsFailed(e))
            }
        }

        // Issue #180: user-initiated pull-to-refresh. Reloads both surfaces
        // concurrently (mirroring init) so one gesture recovers the whole screen
        // — e.g. after a NASA outage left the APOD header empty at launch. Each
        // refresh swallows its own network error, and coroutineScope waits for
        // both before the finally clears the spinner.
        fun refresh() {
            viewModelScope.launch {
                _isRefreshing.value = true
                try {
                    coroutineScope {
                        launch { refreshAsteroids() }
                        launch { refreshPictureOfDay() }
                    }
                } finally {
                    _isRefreshing.value = false
                }
            }
        }

        fun updateFilters(filter: AsteroidsFilter) {
            _filter.value = filter
        }
    }
