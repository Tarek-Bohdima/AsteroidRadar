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
import com.tarek.asteroidradar.network.AsteroidService
import com.tarek.asteroidradar.network.ImageOfTheDay
import com.tarek.asteroidradar.repository.AsteroidRepository.AsteroidsFilter
import com.tarek.asteroidradar.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

class AsteroidRepositoryTest {
    private lateinit var dao: FakeAsteroidDao
    private lateinit var pictureOfDayDao: FakePictureOfDayDao
    private lateinit var service: FakeAsteroidService
    private lateinit var repository: AsteroidRepository

    @Before
    fun setUp() {
        dao = FakeAsteroidDao()
        pictureOfDayDao = FakePictureOfDayDao()
        service = FakeAsteroidService()
        repository = AsteroidRepository(dao, pictureOfDayDao, service)
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

    @Test
    fun `refreshAsteroids inserts parsed entities into the DAO`() =
        runTest {
            val today = nextSevenDaysFormattedDates().first()
            service.asteroidsResult = { feedWithSingleAsteroidOn(today) }

            repository.refreshAsteroids()

            assertThat(dao.insertAllCalls).hasSize(1)
            val inserted = dao.insertAllCalls.single()
            assertThat(inserted).hasSize(1)
            with(inserted.single()) {
                assertThat(id).isEqualTo(SAMPLE_ID)
                assertThat(closeApproachDate).isEqualTo(today)
                assertThat(isPotentiallyHazardous).isTrue()
            }
        }

    @Test
    fun `refreshAsteroids swallows network exceptions and leaves the DAO untouched`() =
        runTest {
            service.asteroidsResult = { throw IOException("simulated network failure") }

            repository.refreshAsteroids()

            assertThat(dao.insertAllCalls).isEmpty()
        }

    @Test
    fun `refreshPictureOfDay persists the converted entity into the DAO`() =
        runTest {
            service.imageOfDayResult = {
                ImageOfTheDay(
                    mediaType = "image",
                    title = "APOD title",
                    url = "https://example.invalid/apod.jpg",
                )
            }

            repository.refreshPictureOfDay()

            assertThat(pictureOfDayDao.insertCalls).hasSize(1)
            with(pictureOfDayDao.insertCalls.single()) {
                assertThat(mediaType).isEqualTo("image")
                assertThat(title).isEqualTo("APOD title")
                assertThat(url).isEqualTo("https://example.invalid/apod.jpg")
            }
        }

    @Test
    fun `refreshPictureOfDay swallows network exceptions and leaves the DAO untouched`() =
        runTest {
            service.imageOfDayResult = { throw IOException("simulated network failure") }

            repository.refreshPictureOfDay()

            assertThat(pictureOfDayDao.insertCalls).isEmpty()
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

    // Builds the same 8-date NeoWs feed shape `parseAsteroidsJsonResult` expects,
    // with one populated asteroid on `today` and empty arrays for the remaining
    // dates. Mirrors the fixture used in ParseAsteroidsJsonResultTest.
    private fun feedWithSingleAsteroidOn(today: String): String {
        val dates = nextSevenDaysFormattedDates()
        val nearEarthObjects =
            JSONObject().apply {
                dates.forEach { date ->
                    if (date == today) {
                        put(date, JSONArray().put(populatedAsteroidJson(date)))
                    } else {
                        put(date, JSONArray())
                    }
                }
            }
        return JSONObject().put("near_earth_objects", nearEarthObjects).toString()
    }

    private fun populatedAsteroidJson(date: String): JSONObject =
        JSONObject().apply {
            put("id", SAMPLE_ID)
            put("name", "(2024 AB1)")
            put("absolute_magnitude_h", 19.5)
            put(
                "estimated_diameter",
                JSONObject().put(
                    "kilometers",
                    JSONObject().put("estimated_diameter_max", 0.42),
                ),
            )
            put(
                "close_approach_data",
                JSONArray().put(
                    JSONObject().apply {
                        put("close_approach_date", date)
                        put("relative_velocity", JSONObject().put("kilometers_per_second", 11.7))
                        put("miss_distance", JSONObject().put("astronomical", 0.025))
                    },
                ),
            )
            put("is_potentially_hazardous_asteroid", true)
        }

    private fun nextSevenDaysFormattedDates(): List<String> {
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat(Constants.API_QUERY_DATE_FORMAT, Locale.getDefault())
        return (0..Constants.DEFAULT_END_DATE_DAYS).map {
            val s = fmt.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            s
        }
    }

    private companion object {
        const val SAMPLE_ID = 54218396L
    }
}

private class FakeAsteroidDao : AsteroidDao {
    val allFlow = MutableStateFlow<List<DatabaseAsteroid>>(emptyList())
    val todayFlow = MutableStateFlow<List<DatabaseAsteroid>>(emptyList())
    val weeklyFlow = MutableStateFlow<List<DatabaseAsteroid>>(emptyList())

    var getTodayCalledWith: String? = null
        private set
    var getWeeklyCalledWith: Pair<String, String>? = null
        private set

    val insertAllCalls = mutableListOf<List<DatabaseAsteroid>>()

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
        insertAllCalls += asteroids.toList()
    }

    override suspend fun deletePreviousAsteroid(today: String) {
        error("not used in this test")
    }
}

private class FakePictureOfDayDao : PictureOfDayDao {
    val flow = MutableStateFlow<DatabasePictureOfDay?>(null)
    val insertCalls = mutableListOf<DatabasePictureOfDay>()

    override fun getPictureOfDay(): Flow<DatabasePictureOfDay?> = flow

    override suspend fun insertPictureOfDay(pictureOfDay: DatabasePictureOfDay) {
        insertCalls += pictureOfDay
    }
}

private class FakeAsteroidService : AsteroidService {
    var asteroidsResult: () -> String = { error("getAsteroids not stubbed") }
    var imageOfDayResult: () -> ImageOfTheDay = { error("getImageOfDay not stubbed") }

    override suspend fun getAsteroids(key: String): String = asteroidsResult()

    override suspend fun getImageOfDay(key: String): ImageOfTheDay = imageOfDayResult()
}
