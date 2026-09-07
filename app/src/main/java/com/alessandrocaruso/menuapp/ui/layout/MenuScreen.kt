package com.alessandrocaruso.menuapp.ui.layout

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.alessandrocaruso.menuapp.R
import com.alessandrocaruso.menuapp.domain.Cart
import com.alessandrocaruso.menuapp.model.Course
import com.alessandrocaruso.menuapp.model.CourseCategory
import com.alessandrocaruso.menuapp.model.Menu
import com.alessandrocaruso.menuapp.ui.UiState
import com.alessandrocaruso.menuapp.ui.components.CourseCard
import com.alessandrocaruso.menuapp.ui.components.ErrorState
import com.alessandrocaruso.menuapp.ui.components.LoadingState
import com.alessandrocaruso.menuapp.ui.theme.myGreen
import com.alessandrocaruso.menuapp.ui.theme.myYellow
import com.alessandrocaruso.menuapp.utils.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A restaurant's menu, grouped into the sections present in the feed.
 *
 * Only sections that actually contain dishes are drawn, so a restaurant with no desserts no
 * longer renders an empty "Desserts" heading.
 */
@Composable
fun MenuScreen(
    state: UiState<Menu>,
    cart: Cart,
    isFavourite: Boolean,
    onAddCourse: (Course) -> Unit,
    onRemoveCourse: (Course) -> Unit,
    onToggleFavourite: () -> Unit,
    onOpenCart: () -> Unit,
    onRetry: () -> Unit,
    qrPayloadFor: (String) -> String,
) {
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
        floatingActionButtonPosition = FabPosition.End,
    ) { paddingValues ->
        when (state) {
            is UiState.Loading -> LoadingState(Modifier.padding(paddingValues))
            is UiState.Error -> ErrorState(
                kind = state.kind,
                onRetry = onRetry,
                modifier = Modifier.padding(paddingValues),
            )
            is UiState.Success -> MenuContent(
                menu = state.data,
                cart = cart,
                isFavourite = isFavourite,
                onAddCourse = onAddCourse,
                onRemoveCourse = onRemoveCourse,
                onToggleFavourite = onToggleFavourite,
                qrPayloadFor = qrPayloadFor,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}

@Composable
private fun MenuContent(
    menu: Menu,
    cart: Cart,
    isFavourite: Boolean,
    onAddCourse: (Course) -> Unit,
    onRemoveCourse: (Course) -> Unit,
    onToggleFavourite: () -> Unit,
    qrPayloadFor: (String) -> String,
    modifier: Modifier = Modifier,
) {
    var showQrDialog by remember { mutableStateOf(false) }

    if (showQrDialog) {
        ShareQrDialog(
            payload = qrPayloadFor(menu.restaurantId),
            onDismiss = { showQrDialog = false },
        )
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 20.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(
                onClick = { showQrDialog = true },
                modifier = Modifier
                    .padding(end = 50.dp)
                    .align(Alignment.CenterVertically)
                    .size(50.dp)
                    .background(color = myYellow, shape = RoundedCornerShape(50)),
            ) {
                Icon(
                    modifier = Modifier.padding(15.dp),
                    painter = painterResource(id = R.drawable.share),
                    tint = myGreen,
                    contentDescription = stringResource(R.string.cd_share_menu),
                )
            }

            TextButton(
                onClick = onToggleFavourite,
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = myYellow,
                    contentColor = myGreen,
                ),
            ) {
                Text(
                    text = stringResource(
                        if (isFavourite) R.string.remove_favourite else R.string.addFav
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
        }

        menu.sections.forEach { section ->
            SectionTitle(title = stringResource(section.category.titleRes()))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(20.dp),
            ) {
                items(items = section.courses, key = { it.id }) { course ->
                    CourseCard(
                        course = course,
                        quantity = cart.quantityOf(course),
                        onAdd = { onAddCourse(course) },
                        onRemove = { onRemoveCourse(course) },
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier
                .height(100.dp)
                .fillMaxWidth(),
        )
    }
}

/**
 * Renders the shareable QR code.
 *
 * Generation runs in a [LaunchedEffect] on [Dispatchers.Default]: the previous implementation
 * called the renderer inline in the composable body, so a ~600px bitmap was encoded on the main
 * thread on *every recomposition* while the dialog was open.
 */
@Composable
private fun ShareQrDialog(payload: String, onDismiss: () -> Unit) {
    var bitmap by remember(payload) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(payload) {
        bitmap = withContext(Dispatchers.Default) { QrCodeGenerator.render(payload) }
    }

    Dialog(onDismissRequest = onDismiss) {
        val rendered = bitmap
        if (rendered == null) {
            LoadingState(Modifier.size(200.dp))
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Image(
                    bitmap = rendered.asImageBitmap(),
                    contentDescription = stringResource(R.string.cd_menu_qr),
                )
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.padding(top = 12.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = myYellow),
                ) {
                    Text(stringResource(R.string.ok), color = myGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Divider(
        modifier = Modifier.padding(20.dp),
        color = Color.Black,
        thickness = 1.dp,
    )
    Text(
        modifier = Modifier.padding(start = 20.dp, bottom = 10.dp),
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
    )
}

/** Maps a domain category onto its localised heading. Keeps string resources out of the model. */
private fun CourseCategory.titleRes(): Int = when (this) {
    CourseCategory.STARTERS -> R.string.starters
    CourseCategory.FIRST_COURSES -> R.string.first
    CourseCategory.SECOND_COURSES -> R.string.second
    CourseCategory.SIDES -> R.string.sides
    CourseCategory.FRUITS -> R.string.fruits
    CourseCategory.DESSERTS -> R.string.Desserts
    CourseCategory.DRINKS -> R.string.drinks
}
