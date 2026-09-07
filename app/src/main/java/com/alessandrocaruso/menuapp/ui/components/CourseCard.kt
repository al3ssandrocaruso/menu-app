package com.alessandrocaruso.menuapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alessandrocaruso.menuapp.R
import com.alessandrocaruso.menuapp.domain.CartLine
import com.alessandrocaruso.menuapp.model.Course
import com.alessandrocaruso.menuapp.ui.formatPrice
import com.alessandrocaruso.menuapp.ui.theme.myGreen
import com.alessandrocaruso.menuapp.ui.theme.myYellow

/**
 * A dish on the menu screen.
 *
 * [quantity] comes from the cart, so the counter reflects changes made on the cart screen
 * immediately. Previously each card kept a private `rememberSaveable` copy of the count that no
 * other screen could update, and mutated the shared `Course` object underneath it.
 */
@Composable
fun CourseCard(
    course: Course,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDescription by rememberSaveable(course.id) { mutableStateOf(false) }

    if (showDescription) {
        AlertDialog(
            shape = RoundedCornerShape(14.dp),
            onDismissRequest = { showDescription = false },
            title = {
                Text(
                    text = course.name,
                    fontSize = 16.sp,
                    color = MaterialTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = course.description.ifBlank {
                        stringResource(R.string.no_description)
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.onSurface,
                )
            },
            buttons = {
                Row(
                    modifier = Modifier.padding(all = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showDescription = false },
                        colors = ButtonDefaults.buttonColors(backgroundColor = myYellow),
                    ) {
                        Text(text = stringResource(R.string.ok), color = myGreen, fontWeight = FontWeight.Bold)
                    }
                }
            },
        )
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier
            .padding(start = 20.dp)
            .clickable { showDescription = true },
    ) {
        Card(
            modifier = Modifier
                .padding(top = 30.dp)
                .width(180.dp),
            shape = RoundedCornerShape(14.dp),
            elevation = 5.dp,
        ) {
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.padding(top = 100.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
            ) {
                Text(
                    text = course.name,
                    modifier = Modifier.padding(top = 5.dp),
                    fontSize = 16.sp,
                )
                Text(
                    text = formatPrice(course.price),
                    color = myYellow,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                QuantityCounter(
                    count = quantity,
                    remove = onRemove,
                    add = onAdd,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
        RemoteImage(
            url = course.poster,
            contentDescription = course.name,
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape),
        )
    }
}

/** A line of the order on the cart screen. */
@Composable
fun OrderedDishCard(
    line: CartLine,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.padding(start = 20.dp),
    ) {
        Card(
            modifier = Modifier
                .padding(end = 30.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(14.dp),
            elevation = 5.dp,
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.padding(end = 100.dp, start = 20.dp, top = 10.dp, bottom = 10.dp),
            ) {
                RemoteImage(
                    url = line.course.poster,
                    contentDescription = line.course.name,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape),
                )
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.padding(start = 10.dp),
                ) {
                    Text(
                        text = line.course.name,
                        modifier = Modifier.padding(top = 5.dp),
                        fontSize = 16.sp,
                    )
                    Text(
                        modifier = Modifier.padding(top = 10.dp),
                        text = formatPrice(line.lineTotal),
                        color = myYellow,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        QuantityCounter(
            count = line.quantity,
            remove = onRemove,
            add = onAdd,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .background(color = myYellow, shape = RoundedCornerShape(50))
                .padding(5.dp),
        )
    }
}

/**
 * Confirmation shown when the user adds a dish while the cart holds another restaurant's order.
 * Hoisted out of [CourseCard] so the decision is made once, by the ViewModel, rather than by
 * whichever card happened to be tapped.
 */
@Composable
fun NewRestaurantDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        shape = RoundedCornerShape(14.dp),
        onDismissRequest = onDismiss,
        text = {
            Text(
                text = stringResource(R.string.cart_other_restaurant),
                color = MaterialTheme.colors.onSurface,
            )
        },
        buttons = {
            Row(
                modifier = Modifier.padding(all = 14.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Button(
                    modifier = Modifier.wrapContentHeight(),
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(backgroundColor = myYellow),
                ) {
                    Text(stringResource(R.string.accept), color = myGreen, fontWeight = FontWeight.Bold)
                }
                Button(
                    modifier = Modifier.padding(start = 8.dp),
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(backgroundColor = myYellow),
                ) {
                    Text(stringResource(R.string.dismiss), color = myGreen, fontWeight = FontWeight.Bold)
                }
            }
        },
    )
}
