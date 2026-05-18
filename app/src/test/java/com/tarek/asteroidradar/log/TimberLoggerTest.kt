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
package com.tarek.asteroidradar.log

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import timber.log.Timber
import java.io.IOException

// Priority constants from android.util.Log. Hard-coded as ints so the JVM
// classpath doesn't need android.jar resolved — these are what Timber's
// .v/.d/.i/.w/.e wrappers pass downstream into Tree.log(priority, ...).
private const val INFO = 4
private const val WARN = 5

// Concrete LogEvent subtypes only: sealed-class hierarchies can't be extended
// from the unit-test source set (Kotlin treats it as a different module), so
// the routing is exercised through the events the codebase actually emits.
// Verbose/Debug/Error priority `when` arms are not covered here — they're
// trivial and will be covered organically when concrete events at those
// priorities land.
class TimberLoggerTest {
    private lateinit var tree: RecordingTree
    private lateinit var logger: TimberLogger

    @Before
    fun setUp() {
        tree = RecordingTree()
        Timber.plant(tree)
        logger = TimberLogger()
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
    }

    @Test
    fun `Info priority routes to Timber i with the event tag and message`() {
        logger.log(LogEvent.App.Launched)

        with(tree.entries.single()) {
            assertThat(priority).isEqualTo(INFO)
            assertThat(tag).isEqualTo("App")
            assertThat(message).isEqualTo("application onCreate")
            assertThat(throwable).isNull()
        }
    }

    @Test
    fun `Warn priority routes to Timber w and forwards the throwable`() {
        val cause = IOException("network down")

        logger.log(LogEvent.Network.RefreshAsteroidsFailed(cause))

        with(tree.entries.single()) {
            assertThat(priority).isEqualTo(WARN)
            assertThat(tag).isEqualTo("Network")
            assertThat(throwable).isSameInstanceAs(cause)
        }
    }

    @Test
    fun `attributes append as key=value suffix to the message`() {
        logger.log(
            LogEvent.Work.RefreshDataWorkerFinished(
                durationMs = 1234L,
                outcome = LogEvent.Work.Outcome.Success,
            ),
        )

        assertThat(tree.entries.single().message)
            .isEqualTo("RefreshDataWorker finished outcome=Success durationMs=1234")
    }

    @Test
    fun `event with no attributes has no message suffix`() {
        logger.log(LogEvent.Work.RefreshDataWorkerStarted)

        // Trailing space would be a bug — verify the empty-attributes branch.
        assertThat(tree.entries.single().message).isEqualTo("RefreshDataWorker started")
    }

    @Test
    fun `tag from event drives Timber tag for each domain`() {
        logger.log(LogEvent.App.Launched)
        logger.log(LogEvent.Network.RefreshPictureOfDayFailed(IOException("x")))
        logger.log(LogEvent.Work.RefreshDataWorkerStarted)

        assertThat(tree.entries.map { it.tag }).containsExactly("App", "Network", "Work").inOrder()
    }
}

private class RecordingTree : Timber.Tree() {
    data class Entry(
        val priority: Int,
        val tag: String?,
        val message: String,
        val throwable: Throwable?,
    )

    val entries = mutableListOf<Entry>()

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        entries += Entry(priority, tag, message, t)
    }
}
