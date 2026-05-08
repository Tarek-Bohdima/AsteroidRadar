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

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tarek.asteroidradar.R
import com.tarek.asteroidradar.domain.Asteroid
import com.tarek.asteroidradar.domain.PictureOfDay
import com.tarek.asteroidradar.repository.AsteroidRepository.AsteroidsFilter

private val ApodHeaderHeight = 220.dp

// Compose port of fragment_main.xml + main_overflow_menu.xml (Phase 9c). The
// top-app-bar's overflow menu replaces the AppCompat MenuProvider; the
// LazyColumn (key = id) replaces AsteroidAdapter + DiffUtil.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onAsteroidClick: (Asteroid) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val asteroids by viewModel.asteroids.collectAsStateWithLifecycle()
    val image by viewModel.imageOfTheDay.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { MainTopBar(onFilterSelect = viewModel::updateFilters) },
        containerColor = colorResource(R.color.app_background),
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            ImageOfTheDayHeader(
                picture = image,
                onVideoTap = { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(asteroids, key = { it.id }) { asteroid ->
                    AsteroidRow(asteroid = asteroid, onClick = { onAsteroidClick(asteroid) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(onFilterSelect: (AsteroidsFilter) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_overflow),
                    contentDescription = stringResource(R.string.overflow_menu_description),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.next_week_asteroids)) },
                    onClick = {
                        onFilterSelect(AsteroidsFilter.WEEK)
                        menuOpen = false
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.today_asteroids)) },
                    onClick = {
                        onFilterSelect(AsteroidsFilter.TODAY)
                        menuOpen = false
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.saved_asteroids)) },
                    onClick = {
                        onFilterSelect(AsteroidsFilter.STORED)
                        menuOpen = false
                    },
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = colorResource(R.color.colorPrimary),
                titleContentColor = colorResource(R.color.default_text_color),
                actionIconContentColor = colorResource(R.color.default_text_color),
            ),
    )
}

// `internal` so MainScreenTest can render this composable without going
// through the Hilt-injected MainViewModel — same testing seam DetailScreen
// uses by accepting its data as a parameter.
@Composable
internal fun ImageOfTheDayHeader(
    picture: PictureOfDay?,
    onVideoTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isVideo = picture?.mediaType == "video"
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(ApodHeaderHeight),
    ) {
        SubcomposeAsyncImage(
            model =
                ImageRequest
                    .Builder(context)
                    .data(picture?.url?.replaceFirst(Regex("^http://"), "https://"))
                    .placeholder(R.drawable.placeholder_picture_of_day)
                    .crossfade(true)
                    .build(),
            contentDescription = stringResource(R.string.image_of_the_day),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            error = {
                // VideoFrameDecoder handles direct .mp4/.webm — when it can't
                // (YouTube/Vimeo embeds), fall back to a tap-through card
                // instead of a broken-image icon.
                if (picture?.mediaType == "video") {
                    VideoFallbackCard(
                        title = picture.title,
                        onClick = { onVideoTap(picture.url) },
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_broken_image),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
        )
        // Caption stays visible on image-days; on video-days the card paints
        // over it via the error slot's fillMaxSize, so no double-label.
        if (!isVideo) {
            Text(
                text = stringResource(R.string.image_of_the_day),
                color = colorResource(R.color.default_text_color),
                style = MaterialTheme.typography.titleLarge,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(colorResource(R.color.image_of_day_caption_scrim))
                        .padding(16.dp),
            )
        }
    }
}

@Composable
internal fun VideoFallbackCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colorResource(R.color.image_of_day_caption_scrim))
                .clickable(onClick = onClick)
                .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_videocam),
            contentDescription = stringResource(R.string.video_of_the_day_open),
            tint = colorResource(R.color.default_text_color),
            modifier = Modifier.height(48.dp),
        )
        Text(
            text = stringResource(R.string.video_of_the_day_format, title),
            color = colorResource(R.color.default_text_color),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun AsteroidRow(
    asteroid: Asteroid,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = asteroid.codename,
                color = colorResource(R.color.default_text_color),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = asteroid.closeApproachDate,
                color = colorResource(R.color.default_text_color),
            )
        }
        Icon(
            painter =
                painterResource(
                    if (asteroid.isPotentiallyHazardous) {
                        R.drawable.ic_status_potentially_hazardous
                    } else {
                        R.drawable.ic_status_normal
                    },
                ),
            contentDescription = stringResource(R.string.content_description_hazard_indicator),
            tint = Color.Unspecified,
            modifier = Modifier.height(64.dp),
        )
    }
}
