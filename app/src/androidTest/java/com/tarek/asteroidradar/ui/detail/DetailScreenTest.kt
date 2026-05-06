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
package com.tarek.asteroidradar.ui.detail

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tarek.asteroidradar.R
import com.tarek.asteroidradar.domain.Asteroid
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleAsteroid =
        Asteroid(
            id = 42L,
            codename = "(2020 AB1)",
            closeApproachDate = "2020-02-01",
            absoluteMagnitude = 25.126,
            estimatedDiameter = 0.82,
            relativeVelocity = 11.9,
            distanceFromEarth = 0.0924,
            isPotentiallyHazardous = false,
        )

    @Test
    fun rendersAllSectionTitles() {
        composeTestRule.setContent { DetailScreen(asteroid = sampleAsteroid) }

        composeTestRule.onNodeWithText(string(R.string.close_approach_data_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.absolute_magnitude_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.estimated_diameter_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.relative_velocity_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.distance_from_earth_title)).assertIsDisplayed()
    }

    @Test
    fun rendersCloseApproachDateValue() {
        composeTestRule.setContent { DetailScreen(asteroid = sampleAsteroid) }

        composeTestRule.onNodeWithText(sampleAsteroid.closeApproachDate).assertIsDisplayed()
    }

    @Test
    fun helpButtonOpensAstronomicalUnitDialog() {
        composeTestRule.setContent { DetailScreen(asteroid = sampleAsteroid) }

        // Dialog text isn't on screen until the help button is tapped.
        val explanation = string(R.string.astronomical_unit_explanation)
        composeTestRule.onNodeWithText(explanation).assertDoesNotExist()

        composeTestRule
            .onNodeWithContentDescription(string(R.string.astronomical_unit_explanation_button))
            .performClick()

        composeTestRule.onNodeWithText(explanation).assertIsDisplayed()
    }

    private fun string(
        @StringRes id: Int,
    ): String = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
}
