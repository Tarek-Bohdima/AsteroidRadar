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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.common.truth.Truth.assertThat
import com.tarek.asteroidradar.database.AsteroidDao
import com.tarek.asteroidradar.database.DatabaseAsteroid
import com.tarek.asteroidradar.database.asDomainModel
import com.tarek.asteroidradar.repository.AsteroidRepository.AsteroidsFilter
import com.tarek.asteroidradar.testing.getOrAwaitValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class AsteroidRepositoryTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var dao: FakeAsteroidDao
    private lateinit var repository: AsteroidRepository

    @Before
    fun setUp() {
        dao = FakeAsteroidDao()
        repository = AsteroidRepository(dao)
    }

    @Test
    fun `STORED filter observes getAsteroids and maps to domain`() {
        val entities = listOf(asteroidEntity(id = 1L), asteroidEntity(id = 2L))
        dao.allLive.value = entities

        val result = repository.getAsteroidSelection(AsteroidsFilter.STORED).getOrAwaitValue()

        assertThat(result).isEqualTo(entities.asDomainModel())
    }

    @Test
    fun `WEEK filter observes getWeeklyAsteroids with a 7-day window`() {
        val entities = listOf(asteroidEntity(id = 1L))
        dao.weeklyLive.value = entities

        val result = repository.getAsteroidSelection(AsteroidsFilter.WEEK).getOrAwaitValue()

        assertThat(result).isEqualTo(entities.asDomainModel())
        val captured = checkNotNull(dao.getWeeklyCalledWith)
        val start = LocalDate.parse(captured.first)
        val end = LocalDate.parse(captured.second)
        assertThat(start.plusDays(7)).isEqualTo(end)
    }

    @Test
    fun `TODAY filter observes getTodayAsteroids with today's date`() {
        val entities = listOf(asteroidEntity(id = 1L))
        dao.todayLive.value = entities

        val result = repository.getAsteroidSelection(AsteroidsFilter.TODAY).getOrAwaitValue()

        assertThat(result).isEqualTo(entities.asDomainModel())
        val captured = checkNotNull(dao.getTodayCalledWith)
        // Repository captures `LocalDate.now()` at construction time; the test
        // runs in the same process moments later, so the values must agree.
        assertThat(LocalDate.parse(captured)).isEqualTo(LocalDate.now())
    }

    private fun asteroidEntity(
        id: Long,
        date: String = LocalDate.now().toString(),
    ): DatabaseAsteroid =
        DatabaseAsteroid(
            id = id,
            codename = "(2024 AB$id)",
            closeApproachDate = date,
            absoluteMagnitude = 19.5,
            estimatedDiameter = 0.42,
            relativeVelocity = 11.7,
            distanceFromEarth = 0.025,
            isPotentiallyHazardous = id % 2L == 0L,
        )
}

private class FakeAsteroidDao : AsteroidDao {
    val allLive = MutableLiveData<List<DatabaseAsteroid>>()
    val todayLive = MutableLiveData<List<DatabaseAsteroid>>()
    val weeklyLive = MutableLiveData<List<DatabaseAsteroid>>()

    var getTodayCalledWith: String? = null
        private set
    var getWeeklyCalledWith: Pair<String, String>? = null
        private set

    override fun getAsteroids(): LiveData<List<DatabaseAsteroid>> = allLive

    override fun getTodayAsteroids(today: String): LiveData<List<DatabaseAsteroid>> {
        getTodayCalledWith = today
        return todayLive
    }

    override fun getWeeklyAsteroids(
        startDate: String,
        endDate: String,
    ): LiveData<List<DatabaseAsteroid>> {
        getWeeklyCalledWith = startDate to endDate
        return weeklyLive
    }

    override fun insertAll(vararg asteroids: DatabaseAsteroid) {
        error("not used in this test")
    }

    override suspend fun deletePreviousAsteroid(today: String) {
        error("not used in this test")
    }
}
