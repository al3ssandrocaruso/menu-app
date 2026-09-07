package com.alessandrocaruso.menuapp.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alessandrocaruso.menuapp.R
import com.alessandrocaruso.menuapp.ui.theme.myGreen
import com.alessandrocaruso.menuapp.ui.theme.myYellow

/**
 * Stateless +/- stepper. [count] is supplied by the caller and the callbacks report intent, so
 * the same widget reflects cart state identically on the menu and cart screens.
 */
@Composable
fun QuantityCounter(
    count: Int,
    remove: () -> Unit,
    add: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        IconButton(
            onClick = remove,
            enabled = count > 0,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .size(25.dp)
                .background(color = myYellow, shape = RoundedCornerShape(50)),
        ) {
            Icon(
                modifier = Modifier.padding(3.dp),
                painter = painterResource(id = R.drawable.ic_baseline_remove_24),
                tint = myGreen,
                contentDescription = stringResource(R.string.cd_remove_one),
            )
        }

        Crossfade(
            targetState = count,
            label = "quantity",
            modifier = Modifier.align(Alignment.CenterVertically),
        ) { value ->
            Text(
                text = "$value",
                color = MaterialTheme.colors.onSurface,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 30.dp),
            )
        }

        IconButton(
            onClick = add,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .size(25.dp)
                .background(color = myYellow, shape = RoundedCornerShape(50)),
        ) {
            Icon(
                modifier = Modifier.padding(3.dp),
                imageVector = Icons.Default.Add,
                tint = myGreen,
                contentDescription = stringResource(R.string.cd_add_one),
            )
        }
    }
}

@Preview
@Composable
private fun QuantityCounterPreview() {
    QuantityCounter(count = 1, remove = {}, add = {})
}
