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

import com.google.common.truth.Truth.assertThat
import com.tarek.asteroidradar.database.AsteroidDao
import com.tarek.asteroidradar.database.DatabaseAsteroid
import com.tarek.asteroidradar.database.DatabasePictureOfDay
import com.tarek.asteroidradar.database.PictureOfDayDao
import com.tarek.asteroidradar.database.asDomainModel
import com.tarek.asteroidradar.domain.PictureOfDay
import com.tarek.asteroidradar.repository.AsteroidRepository.AsteroidsFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class AsteroidRepositoryTest {
    private lateinit var dao: FakeAsteroidDao
    private lateinit var pictureOfDayDao: FakePictureOfDayDao
    private lateinit var repository: AsteroidRepository

    @Before
    fun setUp() {
        dao = FakeAsteroidDao()
        pictureOfDayDao = FakePictureOfDayDao()
        repository = AsteroidRepository(dao, pictureOfDayDao)
    }

    @Test
    fun `STORED filter observes getAsteroids and maps to domain`() =
        runTest {
            val entities = listOf(asteroidEntity(id = 1L), asteroidEntity(id = 2L))
            dao.allFlow.value = entities

            val result = repository.getAsteroidSelection(AsteroidsFilter.STORED).first()

            assertThat(result).isEqualTo(entities.asDomainModel())
        }

    @Test
    fun `WEEK filter observes getWeeklyAsteroids with a 7-day window`() =
        runTest {
            val entities = listOf(asteroidEntity(id = 1L))
            dao.weeklyFlow.value = entities

            val result = repository.getAsteroidSelection(AsteroidsFilter.WEEK).first()

            assertThat(result).isEqualTo(entities.asDomainModel())
            val captured = checkNotNull(dao.getWeeklyCalledWith)
            val start = LocalDate.parse(captured.first)
            val end = LocalDate.parse(captured.second)
            assertThat(start.plusDays(7)).isEqualTo(end)
        }

    @Test
    fun `TODAY filter observes getTodayAsteroids with today's date`() =
        runTest {
            val entities = listOf(asteroidEntity(id = 1L))
            dao.todayFlow.value = entities

            val result = repository.getAsteroidSelection(AsteroidsFilter.TODAY).first()

            assertThat(result).isEqualTo(entities.asDomainModel())
            val captured = checkNotNull(dao.getTodayCalledWith)
            // Repository captures `LocalDate.now()` at construction time; the test
            // runs in the same process moments later, so the values must agree.
            assertThat(LocalDate.parse(captured)).isEqualTo(LocalDate.now())
        }

    @Test
    fun `getPictureOfDay maps the cached entity to the domain model`() =
        runTest {
            pictureOfDayDao.flow.value =
                DatabasePictureOfDay(
                    id = 0,
                    mediaType = "image",
                    title = "Cached APOD",
                    url = "https://example.invalid/apod.jpg",
                )

            val result = repository.getPictureOfDay().first()

            assertThat(result).isEqualTo(
                PictureOfDay(
                    mediaType = "image",
                    title = "Cached APOD",
                    url = "https://example.invalid/apod.jpg",
                ),
            )
        }

    @Test
    fun `getPictureOfDay emits null when nothing is cached`() =
        runTest {
            val result = repository.getPictureOfDay().first()

            assertThat(result).isNull()
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
    val allFlow = MutableStateFlow<List<DatabaseAsteroid>>(emptyList())
    val todayFlow = MutableStateFlow<List<DatabaseAsteroid>>(emptyList())
    val weeklyFlow = MutableStateFlow<List<DatabaseAsteroid>>(emptyList())

    var getTodayCalledWith: String? = null
        private set
    var getWeeklyCalledWith: Pair<String, String>? = null
        private set

    override fun getAsteroids(): Flow<List<DatabaseAsteroid>> = allFlow

    override fun getTodayAsteroids(today: String): Flow<List<DatabaseAsteroid>> {
        getTodayCalledWith = today
        return todayFlow
    }

    override fun getWeeklyAsteroids(
        startDate: String,
        endDate: String,
    ): Flow<List<DatabaseAsteroid>> {
        getWeeklyCalledWith = startDate to endDate
        return weeklyFlow
    }

    override fun insertAll(vararg asteroids: DatabaseAsteroid) {
        error("not used in this test")
    }

    override suspend fun deletePreviousAsteroid(today: String) {
        error("not used in this test")
    }
}

private class FakePictureOfDayDao : PictureOfDayDao {
    val flow = MutableStateFlow<DatabasePictureOfDay?>(null)

    override fun getPictureOfDay(): Flow<DatabasePictureOfDay?> = flow

    override suspend fun insertPictureOfDay(pictureOfDay: DatabasePictureOfDay) {
        error("not used in this test")
    }
}
