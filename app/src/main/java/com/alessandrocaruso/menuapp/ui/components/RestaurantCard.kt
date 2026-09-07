package com.alessandrocaruso.menuapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alessandrocaruso.menuapp.R
import com.alessandrocaruso.menuapp.model.RestaurantPreview
import com.alessandrocaruso.menuapp.ui.theme.myGreen
import com.alessandrocaruso.menuapp.ui.theme.myYellow

/**
 * Home-screen card for one restaurant.
 *
 * Purely presentational: it renders [restaurantPreview] and reports the open-menu tap. Fetching
 * the menu is the ViewModel's job — the previous version issued seven HTTP requests from inside
 * this card's `onClick` and parsed them by hand.
 */
@Composable
fun RestaurantCard(
    restaurantPreview: RestaurantPreview,
    portrait: Boolean,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var moreInfo by rememberSaveable(restaurantPreview.id) { mutableStateOf(false) }

    Card(
        modifier = modifier
            .padding(end = 20.dp)
            .width(180.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = 5.dp,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            RemoteImage(
                url = restaurantPreview.poster,
                contentDescription = restaurantPreview.name,
                modifier = Modifier
                    .padding(5.dp)
                    .size(130.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .align(Alignment.CenterHorizontally),
            )

            Column(modifier = Modifier.padding(top = 10.dp)) {
                Text(
                    text = restaurantPreview.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = restaurantPreview.type,
                    color = Color.Gray,
                    fontSize = 14.sp,
                )

                if (!moreInfo && !portrait) {
                    Text(
                        text = stringResource(R.string.infoPlus),
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable { moreInfo = true }
                            .padding(top = 5.dp),
                        fontSize = 14.sp,
                        color = myGreen,
                    )
                } else {
                    listOf(
                        restaurantPreview.price,
                        restaurantPreview.address,
                        restaurantPreview.city,
                        restaurantPreview.phone,
                    ).forEach { detail ->
                        Divider(
                            color = Color.Gray,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 3.dp),
                        )
                        Text(text = detail, color = Color.Gray, fontSize = 14.sp)
                    }
                    if (!portrait) {
                        Text(
                            text = stringResource(R.string.infoLess),
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clickable { moreInfo = false }
                                .padding(top = 5.dp),
                            fontSize = 14.sp,
                            color = myGreen,
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = onOpenMenu,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.End),
                backgroundColor = myYellow,
                contentColor = myGreen,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(3.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.eye2),
                    tint = myGreen,
                    contentDescription = stringResource(R.string.cd_open_menu),
                    modifier = Modifier.padding(6.dp),
                )
            }
        }
    }
}
