package com.alessandrocaruso.menuapp.ui.layout

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.BottomAppBar
import androidx.compose.material.ContentAlpha
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alessandrocaruso.menuapp.R
import com.alessandrocaruso.menuapp.model.RestaurantPreview
import com.alessandrocaruso.menuapp.ui.UiState
import com.alessandrocaruso.menuapp.ui.components.EmptyState
import com.alessandrocaruso.menuapp.ui.components.ErrorState
import com.alessandrocaruso.menuapp.ui.components.LoadingState
import com.alessandrocaruso.menuapp.ui.components.RestaurantCard
import com.alessandrocaruso.menuapp.ui.theme.myGreen
import com.alessandrocaruso.menuapp.ui.theme.myYellow

/**
 * Restaurant chooser.
 *
 * Stateless with respect to data: everything it renders arrives as parameters and every
 * interaction leaves as a callback. That is what allows the screen to be driven by the ViewModel
 * (and by a test) instead of issuing its own network calls during composition.
 */
@Composable
fun HomeScreen(
    state: UiState<List<RestaurantPreview>>,
    searchQuery: String,
    showFavouritesOnly: Boolean,
    favouriteIds: Set<String>,
    onSearchQueryChange: (String) -> Unit,
    onToggleFavouritesFilter: () -> Unit,
    onOpenMenu: (String) -> Unit,
    onOpenCart: () -> Unit,
    onOpenScanner: () -> Unit,
    onRetry: () -> Unit,
) {
    val portrait = LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE
    var searching by rememberSaveable { mutableStateOf(false) }

    Image(
        modifier = Modifier.fillMaxSize(),
        painter = painterResource(id = R.drawable.bg_app),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
    )
    Scaffold(
        backgroundColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenCart,
                modifier = Modifier.size(64.dp),
                backgroundColor = myYellow,
                contentColor = myGreen,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(5.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.shop_bag2),
                    contentDescription = stringResource(R.string.cd_open_cart),
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        isFloatingActionButtonDocked = true,
        bottomBar = {
            HomeBottomBar(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                searching = searching,
                onSearchingChange = { searching = it },
                showFavouritesOnly = showFavouritesOnly,
                onToggleFavouritesFilter = onToggleFavouritesFilter,
                portrait = portrait,
                onOpenScanner = onOpenScanner,
            )
        },
        topBar = {
            if (portrait) {
                HomeTopBar(searchQuery = searchQuery, onSearchQueryChange = onSearchQueryChange)
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValues),
        ) {
            when (state) {
                is UiState.Loading -> LoadingState()
                is UiState.Error -> ErrorState(kind = state.kind, onRetry = onRetry)
                is UiState.Success -> {
                    val visible = state.data.filterFor(searchQuery, showFavouritesOnly, favouriteIds)
                    if (visible.isEmpty()) {
                        EmptyState(
                            message = stringResource(
                                if (showFavouritesOnly) R.string.empty_favourites
                                else R.string.empty_search
                            ),
                        )
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(
                                start = if (portrait) 20.dp else 40.dp,
                                end = 20.dp,
                                top = 80.dp,
                            ),
                        ) {
                            items(items = visible, key = { it.id }) { preview ->
                                RestaurantCard(
                                    restaurantPreview = preview,
                                    portrait = portrait,
                                    onOpenMenu = { onOpenMenu(preview.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Applies the search box and the favourites filter. Kept separate so it can be reasoned about. */
private fun List<RestaurantPreview>.filterFor(
    query: String,
    favouritesOnly: Boolean,
    favouriteIds: Set<String>,
): List<RestaurantPreview> = filter { preview ->
    val matchesQuery = query.isBlank() || preview.name.contains(query, ignoreCase = true)
    val matchesFavourites = !favouritesOnly || preview.id in favouriteIds
    matchesQuery && matchesFavourites
}

@Composable
private fun HomeTopBar(searchQuery: String, onSearchQueryChange: (String) -> Unit) {
    Box(modifier = Modifier.height(110.dp)) {
        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(id = R.drawable.bg_topbar),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
        )
        Row(
            Modifier
                .padding(20.dp, 10.dp, 20.dp, 20.dp)
                .wrapContentHeight()
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            SearchField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
            )
        }
    }
}

@Composable
private fun HomeBottomBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searching: Boolean,
    onSearchingChange: (Boolean) -> Unit,
    showFavouritesOnly: Boolean,
    onToggleFavouritesFilter: () -> Unit,
    portrait: Boolean,
    onOpenScanner: () -> Unit,
) {
    BottomAppBar(
        elevation = AppBarDefaults.BottomAppBarElevation,
        cutoutShape = CircleShape,
        backgroundColor = myGreen,
        contentPadding = AppBarDefaults.ContentPadding,
    ) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
            IconButton(onClick = onToggleFavouritesFilter) {
                Icon(
                    painter = painterResource(
                        id = if (!showFavouritesOnly) R.drawable.ic_baseline_favorite_24
                        else R.drawable.arrow_back
                    ),
                    contentDescription = stringResource(
                        if (showFavouritesOnly) R.string.cd_show_all else R.string.cd_show_favourites
                    ),
                    tint = myYellow,
                )
            }
        }
        Spacer(Modifier.weight(1f, true))
        if (searching) {
            SearchField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.height(50.dp),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_close_search),
                        modifier = Modifier.clickable { onSearchingChange(false) },
                    )
                },
            )
        } else if (portrait) {
            IconButton(onClick = onOpenScanner) {
                Icon(
                    painter = painterResource(id = R.drawable.qr_code_scanner),
                    contentDescription = stringResource(R.string.cd_scan_qr),
                    tint = myYellow,
                    modifier = Modifier.padding(3.dp),
                )
            }
        } else {
            IconButton(onClick = { onSearchingChange(true) }) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.search),
                    tint = myYellow,
                )
            }
        }
    }
}

/** The search box, shared by the portrait top bar and the landscape bottom bar. */
@Composable
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(text = stringResource(R.string.search), fontSize = 16.sp) },
        textStyle = TextStyle(lineHeight = 70.sp),
        maxLines = 1,
        singleLine = true,
        leadingIcon = {
            Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
        },
        trailingIcon = trailingIcon,
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = MaterialTheme.colors.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedLabelColor = myGreen,
            textColor = Color.Gray,
        ),
        shape = RoundedCornerShape(8.dp),
    )
}
