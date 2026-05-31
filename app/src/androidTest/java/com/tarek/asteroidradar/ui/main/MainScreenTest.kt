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

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tarek.asteroidradar.R
import com.tarek.asteroidradar.domain.PictureOfDay
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val imageDayPicture =
        PictureOfDay(
            mediaType = "image",
            title = "Quiet Sun",
            url = "https://apod.nasa.gov/apod/image/2605/sun.jpg",
        )

    // file:// URL guarantees Coil hits its error path synchronously (no network
    // round-trip), so the fallback slot composes deterministically inside the
    // test's waitUntil window.
    private val videoDayPicture =
        PictureOfDay(
            mediaType = "video",
            title = "Supernova in a Sideways Spiral",
            url = "file:///does/not/exist.mp4",
        )

    // Same trick — file:// URL forces Coil's error path for an image-day, so the
    // broken-icon path composes deterministically without a network round-trip.
    private val brokenImageDayPicture =
        PictureOfDay(
            mediaType = "image",
            title = "Definitely Not a Real Image",
            url = "file:///does/not/exist.jpg",
        )

    @Before
    fun setUp() {
        Intents.init()
        // Stub every ACTION_VIEW so videoDayApodTapOpensBrowserIntent doesn't
        // actually launch a browser activity inside the test process.
        Intents
            .intending(hasAction(Intent.ACTION_VIEW))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun nullPictureShowsPlaceholderNotBrokenIcon() {
        // Given the cache is empty (fresh install, no APOD row in Room yet)
        composeTestRule.setContent {
            ImageOfTheDayHeader(picture = null, onVideoTap = {})
        }

        // When the header composes
        // Then the placeholder testTag is on screen and the broken-icon testTag is not
        // (issue #168: cache-empty state must not surface as a load-failure)
        composeTestRule.onNodeWithTag(APOD_PLACEHOLDER_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(APOD_BROKEN_ICON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun nonNullPictureWithBadUrlShowsBrokenIconNotPlaceholder() {
        // Given a cached image-day picture with an unreachable URL
        composeTestRule.setContent {
            ImageOfTheDayHeader(picture = brokenImageDayPicture, onVideoTap = {})
        }

        // When Coil's error slot fires (deterministic via file:// scheme)
        composeTestRule.waitUntil(timeoutMillis = LOAD_TIMEOUT_MS) {
            composeTestRule
                .onAllNodesWithTag(APOD_BROKEN_ICON_TEST_TAG)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Then the broken-icon testTag is on screen and the placeholder testTag is not —
        // preserves the load-failure surface for actual broken URLs
        composeTestRule.onNodeWithTag(APOD_BROKEN_ICON_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(APOD_PLACEHOLDER_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun imageDayApodRendersImage() {
        composeTestRule.setContent {
            ImageOfTheDayHeader(picture = imageDayPicture, onVideoTap = {})
        }

        // Image-day path keeps the "Image of the Day" caption; video-day path
        // hides it (the fallback card paints over the same area).
        val caption = composeTestRule.activity.getString(R.string.image_of_the_day)
        composeTestRule.onNodeWithText(caption).assertIsDisplayed()
    }

    @Test
    fun videoDayApodWithUnreadableUrlShowsVideoCard() {
        composeTestRule.setContent {
            ImageOfTheDayHeader(picture = videoDayPicture, onVideoTap = {})
        }

        val cardLabel =
            composeTestRule.activity.getString(
                R.string.video_of_the_day_format,
                videoDayPicture.title,
            )
        composeTestRule.waitUntil(timeoutMillis = LOAD_TIMEOUT_MS) {
            composeTestRule
                .onAllNodesWithText(cardLabel)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(cardLabel).assertIsDisplayed()
    }

    @Test
    fun videoDayApodTapOpensBrowserIntent() {
        composeTestRule.setContent {
            val context = LocalContext.current
            ImageOfTheDayHeader(
                picture = videoDayPicture,
                onVideoTap = { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
            )
        }

        val cardLabel =
            composeTestRule.activity.getString(
                R.string.video_of_the_day_format,
                videoDayPicture.title,
            )
        composeTestRule.waitUntil(timeoutMillis = LOAD_TIMEOUT_MS) {
            composeTestRule
                .onAllNodesWithText(cardLabel)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText(cardLabel).performClick()

        Intents.intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(Uri.parse(videoDayPicture.url)),
            ),
        )
    }

    private companion object {
        // Coil's file:// load fails almost instantly; 5s is generous headroom
        // for slow CI emulators without making test failures sluggish.
        const val LOAD_TIMEOUT_MS = 5_000L
    }
}
