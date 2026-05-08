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

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PictureOfDayDaoTest {
    private lateinit var db: AsteroidDatabase
    private lateinit var dao: PictureOfDayDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(context, AsteroidDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = db.pictureOfDayDao
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getPictureOfDay_emitsNullBeforeFirstInsert() =
        runTest {
            val result = dao.getPictureOfDay().first()

            assertThat(result).isNull()
        }

    @Test
    fun getPictureOfDay_returnsRowAfterInsert() =
        runTest {
            val row = pictureOfDay(title = "Hubble Deep Field", url = "https://example.invalid/a.jpg")
            dao.insertPictureOfDay(row)

            val result = dao.getPictureOfDay().first()

            assertThat(result).isEqualTo(row)
        }

    @Test
    fun insertPictureOfDay_replacesExistingRowOnConflict() =
        runTest {
            dao.insertPictureOfDay(pictureOfDay(title = "yesterday", url = "https://example.invalid/y.jpg"))
            dao.insertPictureOfDay(pictureOfDay(title = "today", url = "https://example.invalid/t.jpg"))

            val result = dao.getPictureOfDay().first()

            assertThat(result?.title).isEqualTo("today")
            assertThat(result?.url).isEqualTo("https://example.invalid/t.jpg")
        }

    private fun pictureOfDay(
        title: String,
        url: String,
        mediaType: String = "image",
    ): DatabasePictureOfDay =
        DatabasePictureOfDay(
            id = PICTURE_OF_DAY_ROW_ID,
            mediaType = mediaType,
            title = title,
            url = url,
        )
}
