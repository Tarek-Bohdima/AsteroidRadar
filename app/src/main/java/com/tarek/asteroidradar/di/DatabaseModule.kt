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
package com.tarek.asteroidradar.di

import android.content.Context
import com.tarek.asteroidradar.database.AsteroidDao
import com.tarek.asteroidradar.database.AsteroidDatabase
import com.tarek.asteroidradar.database.getDatabase
import com.tarek.asteroidradar.repository.AsteroidRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Hilt module for database + repository graph. Phase 5b only — `getDatabase()`
// is intentionally kept as the construction site so :app (via Hilt) and
// `RefreshDataWorker` (still pre-Hilt this phase) share a single Room instance.
// Phase 5c migrates the worker, deletes `getDatabase()`, and inlines
// `Room.databaseBuilder(...)` here.
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAsteroidDatabase(
        @ApplicationContext context: Context,
    ): AsteroidDatabase = getDatabase(context)

    @Provides
    fun provideAsteroidDao(database: AsteroidDatabase): AsteroidDao = database.asteroidDao

    @Provides
    @Singleton
    fun provideAsteroidRepository(database: AsteroidDatabase): AsteroidRepository = AsteroidRepository(database)
}
