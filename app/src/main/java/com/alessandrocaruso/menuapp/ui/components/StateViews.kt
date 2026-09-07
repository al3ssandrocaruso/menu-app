package com.alessandrocaruso.menuapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alessandrocaruso.menuapp.R
import com.alessandrocaruso.menuapp.ui.ErrorKind
import com.alessandrocaruso.menuapp.ui.theme.myGreen
import com.alessandrocaruso.menuapp.ui.theme.myYellow

/** Shown while a request is in flight. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = myGreen)
    }
}

/**
 * Shown when a request fails, with the reason and a retry action.
 *
 * The original app had neither: a network failure logged `Log.v("IMDB", "KAOS")` and left the
 * user on a permanently empty screen.
 */
@Composable
fun ErrorState(
    kind: ErrorKind,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(kind.messageRes()),
            color = MaterialTheme.colors.onSurface,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetry,
            modifier = Modifier
                .padding(top = 20.dp)
                .height(48.dp),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 32.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = myYellow),
        ) {
            Text(
                text = stringResource(R.string.retry),
                color = myGreen,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Shown when a request succeeded but produced nothing to display. */
@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colors.onSurface,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
    }
}

private fun ErrorKind.messageRes(): Int = when (this) {
    ErrorKind.NoConnection -> R.string.error_no_connection
    ErrorKind.Timeout -> R.string.error_timeout
    ErrorKind.NotFound -> R.string.error_not_found
    ErrorKind.Server -> R.string.error_server
    ErrorKind.Malformed -> R.string.error_malformed
    ErrorKind.Unknown -> R.string.error_unknown
}
