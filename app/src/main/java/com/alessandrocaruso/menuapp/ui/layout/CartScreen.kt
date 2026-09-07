package com.alessandrocaruso.menuapp.ui.layout

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alessandrocaruso.menuapp.R
import com.alessandrocaruso.menuapp.domain.Cart
import com.alessandrocaruso.menuapp.model.Course
import com.alessandrocaruso.menuapp.ui.components.EmptyState
import com.alessandrocaruso.menuapp.ui.components.OrderedDishCard
import com.alessandrocaruso.menuapp.ui.formatPrice
import com.alessandrocaruso.menuapp.ui.theme.myYellow

/**
 * Order summary.
 *
 * Totals are read from [Cart] rather than recomputed here, so the subtotal shown can no longer
 * drift from the selected dishes. The previous screen tracked a running `Double` that was
 * incremented and decremented by each card independently.
 */
@Composable
fun CartScreen(
    cart: Cart,
    onAddCourse: (Course) -> Unit,
    onRemoveCourse: (Course) -> Unit,
) {
    Image(
        modifier = Modifier.fillMaxSize(),
        painter = painterResource(id = R.drawable.bg_app),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
    )
    Scaffold(
        backgroundColor = Color.Transparent,
        bottomBar = { CartBottomBar(cart = cart) },
        topBar = {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                text = stringResource(R.string.Cart),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline,
                textAlign = TextAlign.Center,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (cart.isEmpty) {
                EmptyState(message = stringResource(R.string.empty_cart))
            } else {
                LazyColumn(
                    modifier = Modifier.padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(20.dp),
                ) {
                    items(items = cart.lines, key = { it.course.id }) { line ->
                        OrderedDishCard(
                            line = line,
                            onAdd = { onAddCourse(line.course) },
                            onRemove = { onRemoveCourse(line.course) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartBottomBar(cart: Cart) {
    val portrait = LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp),
            )
            .padding(20.dp),
    ) {
        TextAndPrice(stringResource(R.string.subTotal), cart.subtotal, portrait)
        TextAndPrice(stringResource(R.string.delivery), if (cart.isEmpty) 0.0 else cart.deliveryFee, portrait)
        Divider(
            modifier = Modifier.padding(vertical = if (portrait) 10.dp else 5.dp),
            color = MaterialTheme.colors.onSurface,
            thickness = 1.dp,
        )
        TextAndPrice(stringResource(R.string.total), cart.total, portrait)
        TextButton(
            modifier = Modifier
                .padding(top = if (portrait) 20.dp else 0.dp)
                .align(Alignment.CenterHorizontally),
            onClick = { /* Checkout is out of scope: the data set is read-only. */ },
            enabled = !cart.isEmpty,
            shape = RoundedCornerShape(30),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = myYellow,
                contentColor = Color.Black,
            ),
        ) {
            Text(
                text = stringResource(R.string.conf),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 70.dp),
            )
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun TextAndPrice(text: String, price: Double, portrait: Boolean) {
    Row(modifier = Modifier.padding(vertical = if (portrait) 5.dp else 2.dp)) {
        Text(
            modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.Start),
            text = text,
            fontSize = 16.sp,
            color = MaterialTheme.colors.onSurface,
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .wrapContentWidth(Alignment.End),
            text = formatPrice(price),
            fontSize = 16.sp,
            color = MaterialTheme.colors.onSurface,
        )
    }
}
