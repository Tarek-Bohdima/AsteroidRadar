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
package com.tarek.asteroidradar.network

import com.tarek.asteroidradar.util.Constants
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val DEFAULT_ID = 54218396L
private const val DEFAULT_NAME = "(2024 AB1)"
private const val DEFAULT_MAGNITUDE = 19.5
private const val DEFAULT_DIAMETER_MAX_KM = 0.42
private const val DEFAULT_VELOCITY_KM_PER_SEC = 11.7
private const val DEFAULT_MISS_AU = 0.025

class ParseAsteroidsJsonResultTest {
    @Test
    fun `parses one asteroid per date across the seven-day window`() {
        val dates = expectedDateWindow()
        val payload = payloadWithSingleAsteroidPerDate(dates)

        val asteroids = parseAsteroidsJsonResult(payload)

        // Parser asks for an 8-date window (DEFAULT_END_DATE_DAYS=7, range 0..7).
        assertEquals(dates.size, asteroids.size)
        assertEquals(dates.toSet(), asteroids.map { it.closeApproachDate }.toSet())
    }

    @Test
    fun `maps every field on a populated asteroid`() {
        val dates = expectedDateWindow()
        val today = dates.first()
        val payload =
            JSONObject().put(
                "near_earth_objects",
                JSONObject().apply {
                    put(today, JSONArray().put(populatedAsteroidJson(today)))
                    // Remaining 7 dates required by the parser; empty arrays are fine.
                    dates.drop(1).forEach { put(it, JSONArray()) }
                },
            )

        val parsed = parseAsteroidsJsonResult(payload)

        assertEquals(1, parsed.size)
        with(parsed.first()) {
            assertEquals(DEFAULT_ID, id)
            assertEquals(DEFAULT_NAME, codename)
            assertEquals(today, closeApproachDate)
            assertEquals(DEFAULT_MAGNITUDE, absoluteMagnitude, 0.0001)
            assertEquals(DEFAULT_DIAMETER_MAX_KM, estimatedDiameter, 0.0001)
            assertEquals(DEFAULT_VELOCITY_KM_PER_SEC, relativeVelocity, 0.0001)
            assertEquals(DEFAULT_MISS_AU, distanceFromEarth, 0.0001)
            assertTrue(isPotentiallyHazardous)
        }
    }

    @Test
    fun `respects the hazardous flag`() {
        val dates = expectedDateWindow()
        val today = dates.first()
        val safe = populatedAsteroidJson(today, id = 1, hazardous = false)
        val payload =
            JSONObject().put(
                "near_earth_objects",
                JSONObject().apply {
                    put(today, JSONArray().put(safe))
                    dates.drop(1).forEach { put(it, JSONArray()) }
                },
            )

        val parsed = parseAsteroidsJsonResult(payload)

        assertEquals(1, parsed.size)
        assertFalse(parsed.first().isPotentiallyHazardous)
    }

    private fun payloadWithSingleAsteroidPerDate(dates: List<String>): JSONObject {
        val nearEarthObjects =
            JSONObject().apply {
                dates.forEachIndexed { index, date ->
                    put(date, JSONArray().put(populatedAsteroidJson(date, id = index.toLong())))
                }
            }
        return JSONObject().put("near_earth_objects", nearEarthObjects)
    }

    private fun populatedAsteroidJson(
        date: String,
        id: Long = DEFAULT_ID,
        hazardous: Boolean = true,
    ): JSONObject =
        JSONObject().apply {
            put("id", id)
            put("name", DEFAULT_NAME)
            put("absolute_magnitude_h", DEFAULT_MAGNITUDE)
            put(
                "estimated_diameter",
                JSONObject().put(
                    "kilometers",
                    JSONObject().put("estimated_diameter_max", DEFAULT_DIAMETER_MAX_KM),
                ),
            )
            put(
                "close_approach_data",
                JSONArray().put(
                    JSONObject().apply {
                        put("close_approach_date", date)
                        put(
                            "relative_velocity",
                            JSONObject().put("kilometers_per_second", DEFAULT_VELOCITY_KM_PER_SEC),
                        )
                        put("miss_distance", JSONObject().put("astronomical", DEFAULT_MISS_AU))
                    },
                ),
            )
            put("is_potentially_hazardous_asteroid", hazardous)
        }

    // Mirrors the parser's internal date helper so the fixture lines up with the
    // dates parseAsteroidsJsonResult will iterate. If the parser starts injecting
    // its date list, this can be deleted in favor of a constructor parameter.
    private fun expectedDateWindow(): List<String> {
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat(Constants.API_QUERY_DATE_FORMAT, Locale.getDefault())
        return (0..Constants.DEFAULT_END_DATE_DAYS).map {
            val s = fmt.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            s
        }
    }
}
