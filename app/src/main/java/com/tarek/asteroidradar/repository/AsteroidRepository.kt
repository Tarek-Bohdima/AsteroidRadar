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

package com.tarek.asteroidradar.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.tarek.asteroidradar.BuildConfig
import com.tarek.asteroidradar.database.AsteroidDatabase
import com.tarek.asteroidradar.database.asDomainModel
import com.tarek.asteroidradar.domain.Asteroid
import com.tarek.asteroidradar.domain.PictureOfDay
import com.tarek.asteroidradar.network.AsteroidApi
import com.tarek.asteroidradar.network.asDatabaseModel
import com.tarek.asteroidradar.network.asDomainModel
import com.tarek.asteroidradar.network.parseAsteroidsJsonResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.time.LocalDate

class AsteroidRepository(private val database: AsteroidDatabase) {
    sealed class AsteroidsFilter {
        data object TODAY : AsteroidsFilter()

        data object WEEK : AsteroidsFilter()

        data object STORED : AsteroidsFilter()
    }

    companion object {
        const val API_KEY = BuildConfig.NASA_API_KEY
    }

    private val startDate = LocalDate.now().toString()

    private val endDate = LocalDate.now().plusDays(7).toString()

    fun getAsteroidSelection(filter: AsteroidsFilter): LiveData<List<Asteroid>> {
        return when (filter) {
            AsteroidsFilter.STORED ->
                database.asteroidDao.getAsteroids().map {
                    it.asDomainModel()
                }

            AsteroidsFilter.WEEK ->
                database.asteroidDao.getWeeklyAsteroids(startDate, endDate).map {
                    it.asDomainModel()
                }

            AsteroidsFilter.TODAY ->
                database.asteroidDao.getTodayAsteroids(startDate).map {
                    it.asDomainModel()
                }
        }
    }

    suspend fun getImageOfTheDay(): PictureOfDay {
        var imageOfTheDay: PictureOfDay
        withContext(Dispatchers.Main) {
            imageOfTheDay = AsteroidApi.retrofitService.getImageOfDay(API_KEY).asDomainModel()
        }
        return imageOfTheDay
    }

    suspend fun refreshAsteroids() {
        withContext(Dispatchers.IO) {
            try {
                val asteroidsJson =
                    AsteroidApi.retrofitService.getAsteroids(API_KEY)
                database.asteroidDao.insertAll(
                    *parseAsteroidsJsonResult(JSONObject(asteroidsJson))
                        .asDatabaseModel(),
                )
            } catch (e: Exception) {
                Timber.d("AsteroidRepository: refreshAsteroids() failed %s", e.message)
            }
        }
    }

    suspend fun deletePastAsteroids() {
        database.asteroidDao.deletePreviousAsteroid(startDate)
    }
}
