package com.alessandrocaruso.menuapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.alessandrocaruso.menuapp.R

/**
 * Loads a remote image into a Compose slot.
 *
 * Replaces a hand-rolled Picasso `Target` composable that had three concrete defects:
 *  - Picasso holds `Target` references weakly, and the anonymous target was not retained, so
 *    in-flight loads could be collected and images intermittently never appeared;
 *  - `BitmapFactory.decodeResource` for the placeholder ran on the main thread on every
 *    recomposition of every card;
 *  - full-resolution bitmaps were decoded into 130dp slots with no downsampling or caching.
 *
 * Coil handles retention, sizing, memory/disk caching and cancellation as part of the composable
 * lifecycle, which is why it replaces Picasso here rather than being added alongside it.
 */
@Composable
fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val fallback = painterResource(id = R.drawable.intl_closed)
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url.ifBlank { null })
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = fallback,
        error = fallback,
        fallback = fallback,
    )
}
