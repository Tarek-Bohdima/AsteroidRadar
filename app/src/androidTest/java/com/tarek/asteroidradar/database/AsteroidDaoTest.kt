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
package com.tarek.asteroidradar.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.tarek.asteroidradar.testing.getOrAwaitValue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AsteroidDaoTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AsteroidDatabase
    private lateinit var dao: AsteroidDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        // allowMainThreadQueries() is fine for tests — instrumented tests run on
        // the main thread and the DAO methods we exercise are LiveData / suspend,
        // not blocking. Production code never sees this builder.
        db =
            Room
                .inMemoryDatabaseBuilder(context, AsteroidDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = db.asteroidDao
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getAsteroids_returnsAllRowsOrderedByCloseApproachDateAsc() {
        val later = asteroid(id = 1L, date = "2026-05-10")
        val earliest = asteroid(id = 2L, date = "2026-05-01")
        val middle = asteroid(id = 3L, date = "2026-05-05")
        dao.insertAll(later, earliest, middle)

        val result = dao.getAsteroids().getOrAwaitValue()

        assertThat(result.map { it.id }).containsExactly(2L, 3L, 1L).inOrder()
    }

    @Test
    fun getTodayAsteroids_returnsOnlyRowsMatchingToday() {
        val today = "2026-05-05"
        dao.insertAll(
            asteroid(id = 1L, date = today),
            asteroid(id = 2L, date = today),
            asteroid(id = 3L, date = "2026-05-04"),
            asteroid(id = 4L, date = "2026-05-06"),
        )

        val result = dao.getTodayAsteroids(today).getOrAwaitValue()

        assertThat(result.map { it.id }).containsExactly(1L, 2L)
    }

    @Test
    fun getWeeklyAsteroids_isInclusiveOnBothBounds() {
        val start = "2026-05-05"
        val end = "2026-05-12"
        dao.insertAll(
            asteroid(id = 1L, date = "2026-05-04"), // before window
            asteroid(id = 2L, date = start), // inclusive lower bound
            asteroid(id = 3L, date = "2026-05-08"), // mid-window
            asteroid(id = 4L, date = end), // inclusive upper bound
            asteroid(id = 5L, date = "2026-05-13"), // after window
        )

        val result = dao.getWeeklyAsteroids(start, end).getOrAwaitValue()

        assertThat(result.map { it.id }).containsExactly(2L, 3L, 4L).inOrder()
    }

    @Test
    fun insertAll_replacesRowOnConflictingPrimaryKey() {
        dao.insertAll(asteroid(id = 1L, codename = "first", date = "2026-05-05"))
        dao.insertAll(asteroid(id = 1L, codename = "second", date = "2026-05-06"))

        val result = dao.getAsteroids().getOrAwaitValue()

        assertThat(result).hasSize(1)
        assertThat(result.single().codename).isEqualTo("second")
        assertThat(result.single().closeApproachDate).isEqualTo("2026-05-06")
    }

    @Test
    fun deletePreviousAsteroid_removesOnlyRowsStrictlyBeforeToday() =
        runTest {
            val today = "2026-05-05"
            dao.insertAll(
                asteroid(id = 1L, date = "2026-05-03"), // past
                asteroid(id = 2L, date = "2026-05-04"), // past
                asteroid(id = 3L, date = today), // boundary — kept
                asteroid(id = 4L, date = "2026-05-06"), // future
            )

            dao.deletePreviousAsteroid(today)

            val result = dao.getAsteroids().getOrAwaitValue()
            assertThat(result.map { it.id }).containsExactly(3L, 4L).inOrder()
        }

    private fun asteroid(
        id: Long,
        date: String,
        codename: String = "(2024 AB$id)",
    ): DatabaseAsteroid =
        DatabaseAsteroid(
            id = id,
            codename = codename,
            closeApproachDate = date,
            absoluteMagnitude = 19.5,
            estimatedDiameter = 0.42,
            relativeVelocity = 11.7,
            distanceFromEarth = 0.025,
            isPotentiallyHazardous = id % 2L == 0L,
        )
}
