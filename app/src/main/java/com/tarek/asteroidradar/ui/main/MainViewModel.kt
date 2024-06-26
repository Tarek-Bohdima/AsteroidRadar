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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.tarek.asteroidradar.database.getDatabase
import com.tarek.asteroidradar.domain.Asteroid
import com.tarek.asteroidradar.domain.PictureOfDay
import com.tarek.asteroidradar.repository.AsteroidRepository
import com.tarek.asteroidradar.repository.AsteroidRepository.AsteroidsFilter
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = getDatabase(application)
    private val repository = AsteroidRepository(database)

    private val _imageOfTheDay = MutableLiveData<PictureOfDay>()
    val imageOfTheDay: LiveData<PictureOfDay>
        get() = _imageOfTheDay

    private val _navigateToDetail = MutableLiveData<Asteroid?>()
    val navigateToDetail
        get() = _navigateToDetail

    private val _filter = MutableLiveData<AsteroidsFilter>()
    val filter
        get() = _filter

    val asteroids =
        filter.switchMap {
            repository.getAsteroidSelection(it)
        }

    init {
        viewModelScope.launch {
            getAsteroidList()
            getImageOfTheDay()
        }
    }

    private suspend fun getImageOfTheDay() {
        try {
            _imageOfTheDay.value = repository.getImageOfTheDay()
        } catch (e: Exception) {
            Timber.d("MainViewModel: getImageOfTheDay called : %s", e.message)
        }
    }

    private suspend fun getAsteroidList() {
        try {
            repository.refreshAsteroids()
        } catch (e: Exception) {
            Timber.d("MainViewModel: getAsteroidList() called : %s", e.message)
        }
    }

    fun onAsteroidClicked(asteroid: Asteroid) {
        _navigateToDetail.value = asteroid
    }

    fun onAsteroidDetailNavigated() {
        _navigateToDetail.value = null
    }

    /**
     * Updates the data set filter for the web services by querying the data with the new filter
     * by calling [getAsteroidList]
     * @param filter the [AsteroidsFilter] that is sent as part of the web server request
     */
    fun updateFilters(filter: AsteroidsFilter) {
        _filter.value = filter
    }

    /**
     * Factory for constructing MainViewModel with parameter
     */
    class Factory(val app: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(app) as T
            }
            throw IllegalArgumentException("Unable to construct viewmodel")
        }
    }
}
