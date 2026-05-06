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
package com.tarek.asteroidradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.tarek.asteroidradar.domain.Asteroid
import com.tarek.asteroidradar.ui.detail.DetailScreen
import com.tarek.asteroidradar.ui.main.AsteroidNavType
import com.tarek.asteroidradar.ui.main.MainScreen
import com.tarek.asteroidradar.ui.theme.AsteroidRadarTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlin.reflect.typeOf

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate per AndroidX Activity contract.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AsteroidRadarTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = MainRoute) {
                    composable<MainRoute> {
                        MainScreen(
                            onAsteroidClick = { asteroid ->
                                navController.navigate(DetailRoute(asteroid))
                            },
                        )
                    }
                    composable<DetailRoute>(
                        // Nav-Compose typed routes need a typeMap entry for any
                        // non-primitive @Serializable argument; AsteroidNavType
                        // encodes via Json.encodeToString.
                        typeMap = mapOf(typeOf<Asteroid>() to AsteroidNavType),
                    ) { entry ->
                        val route: DetailRoute = entry.toRoute()
                        DetailScreen(
                            asteroid = route.asteroid,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
