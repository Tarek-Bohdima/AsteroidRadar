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
package com.tarek.asteroidradar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Minimal Material 3 dark-only theme keyed off the existing palette
// (`@color/app_background`, `@color/default_text_color`, etc.) so the post-9c
// Compose surface stays visually consistent with the post-Phase-8 view-system
// look. A proper Material 3 colour-tokens migration is its own phase.
private val AsteroidColorScheme =
    darkColorScheme(
        primary = Color(0xFFFF8282),
        onPrimary = Color(0xFF010613),
        background = Color(0xFF010613),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF010613),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF1A2030),
        onSurfaceVariant = Color(0xFFD2D2D2),
    )

@Composable
fun AsteroidRadarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AsteroidColorScheme,
        content = content,
    )
}
