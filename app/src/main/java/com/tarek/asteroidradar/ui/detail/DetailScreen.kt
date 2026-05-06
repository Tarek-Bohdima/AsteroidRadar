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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tarek.asteroidradar.R
import com.tarek.asteroidradar.domain.Asteroid

// Compose port of fragment_detail.xml (Phase 9b). Hosted by DetailFragment via
// ComposeView until 9c rewires navigation to Nav-Compose. Top inset is left to
// the AppCompat ActionBar (matches Phase 8's fragment-side insets handling) —
// only horizontal + bottom safe-drawing insets are padded here.
@Composable
fun DetailScreen(
    asteroid: Asteroid,
    modifier: Modifier = Modifier,
) {
    var showAstronomicalUnitDialog by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colorResource(R.color.app_background))
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                ).verticalScroll(rememberScrollState()),
    ) {
        // Hazard banner — local drawable selected by the Asteroid's flag.
        // Replaces the asteroidStatusImage @BindingAdapter that 9b deletes.
        Image(
            painter =
                painterResource(
                    if (asteroid.isPotentiallyHazardous) {
                        R.drawable.asteroid_hazardous
                    } else {
                        R.drawable.asteroid_safe
                    },
                ),
            contentDescription = stringResource(R.string.content_description_hazard_indicator),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(modifier = Modifier.padding(16.dp)) {
            DetailSection(
                title = stringResource(R.string.close_approach_data_title),
                value = asteroid.closeApproachDate,
            )

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                DetailSection(
                    title = stringResource(R.string.absolute_magnitude_title),
                    value =
                        stringResource(
                            R.string.astronomical_unit_format,
                            asteroid.absoluteMagnitude,
                        ),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showAstronomicalUnitDialog = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_help_circle),
                        contentDescription =
                            stringResource(
                                R.string.astronomical_unit_explanation_button,
                            ),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            DetailSection(
                title = stringResource(R.string.estimated_diameter_title),
                value = stringResource(R.string.km_unit_format, asteroid.estimatedDiameter),
            )

            Spacer(Modifier.height(16.dp))
            DetailSection(
                title = stringResource(R.string.relative_velocity_title),
                value = stringResource(R.string.km_s_unit_format, asteroid.relativeVelocity),
            )

            Spacer(Modifier.height(16.dp))
            DetailSection(
                title = stringResource(R.string.distance_from_earth_title),
                value =
                    stringResource(
                        R.string.astronomical_unit_format,
                        asteroid.distanceFromEarth,
                    ),
            )
        }
    }

    if (showAstronomicalUnitDialog) {
        AlertDialog(
            onDismissRequest = { showAstronomicalUnitDialog = false },
            confirmButton = {
                TextButton(onClick = { showAstronomicalUnitDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            text = { Text(stringResource(R.string.astronomical_unit_explanation)) },
        )
    }
}

@Composable
private fun DetailSection(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = title, color = colorResource(R.color.default_text_color))
        Text(
            text = value,
            color = colorResource(R.color.default_text_color),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Preview
@Composable
private fun DetailScreenPreview() {
    DetailScreen(
        asteroid =
            Asteroid(
                id = 1L,
                codename = "(2020 AB1)",
                closeApproachDate = "2020-02-01",
                absoluteMagnitude = 25.126,
                estimatedDiameter = 0.82,
                relativeVelocity = 11.9,
                distanceFromEarth = 0.0924,
                isPotentiallyHazardous = false,
            ),
    )
}

@Preview
@Composable
private fun DetailScreenHazardousPreview() {
    DetailScreen(
        asteroid =
            Asteroid(
                id = 2L,
                codename = "(2020 XX9)",
                closeApproachDate = "2020-02-15",
                absoluteMagnitude = 19.4,
                estimatedDiameter = 1.84,
                relativeVelocity = 27.6,
                distanceFromEarth = 0.0034,
                isPotentiallyHazardous = true,
            ),
    )
}
