package com.alessandrocaruso.menuapp.ui.layout

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.alessandrocaruso.menuapp.R
import com.alessandrocaruso.menuapp.domain.MenuQrPayload
import com.alessandrocaruso.menuapp.ui.theme.myGreen
import com.alessandrocaruso.menuapp.ui.theme.myYellow
import com.alessandrocaruso.menuapp.utils.MenuDownloader
import com.alessandrocaruso.menuapp.utils.QRCodeAnalyzer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Camera screen that scans menu QR codes.
 *
 * Lifecycle handling, in contrast to the original:
 *  - the camera provider is obtained asynchronously instead of blocking the main thread on
 *    `cameraProviderFuture.get()`;
 *  - frames are analysed on a dedicated single-thread executor rather than the main executor, so
 *    ZXing decoding never competes with rendering;
 *  - use cases are unbound and the executor is shut down in `onDispose`, so leaving the screen
 *    releases the camera instead of leaving it bound to a stale lifecycle.
 *
 * Interpretation of the scanned text is delegated entirely to [MenuQrPayload].
 */
@Composable
fun QrCodeScannerScreen(
    qrPayload: MenuQrPayload,
    onOpenMenu: (String) -> Unit,
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            permissionRequested = true
        },
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasCameraPermission) {
        PermissionDenied(
            showRetry = permissionRequested,
            onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        )
        return
    }

    var scan by remember { mutableStateOf<MenuQrPayload.ScanResult?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            enabled = scan == null,
            onScanned = { text ->
                // Ignore further frames while a result is on screen.
                if (scan == null) scan = qrPayload.decode(text)
            },
        )

        when (val result = scan) {
            null -> Unit
            is MenuQrPayload.ScanResult.MenuLink -> MenuLinkDialog(
                result = result,
                onDownload = {
                    val outcome = MenuDownloader.enqueue(
                        context = context,
                        url = result.pdfUrl,
                        fileName = "Menu-${result.restaurantId}",
                    )
                    val message = when (outcome) {
                        is MenuDownloader.Result.Enqueued -> R.string.download_started
                        is MenuDownloader.Result.Failed -> R.string.download_failed
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    scan = null
                },
                onOpenInApp = {
                    scan = null
                    onOpenMenu(result.restaurantId)
                },
                onDismiss = { scan = null },
            )
            MenuQrPayload.ScanResult.NotAMenuLink,
            MenuQrPayload.ScanResult.Empty,
            -> InvalidQrDialog(onDismiss = { scan = null })
        }
    }
}

/**
 * Binds the CameraX preview and analysis use cases for as long as this composable is present.
 */
@Composable
private fun CameraPreview(
    enabled: Boolean,
    onScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    // Read by the analyzer thread; held outside composition state so pausing analysis while a
    // result dialog is up does not tear down and rebind the camera.
    val analysisEnabled = remember { AtomicBoolean(true) }
    analysisEnabled.set(enabled)

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null

        cameraProviderFuture.addListener({
            val cameraProvider = runCatching { cameraProviderFuture.get() }.getOrNull()
                ?: return@addListener
            provider = cameraProvider

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(
                        analysisExecutor,
                        QRCodeAnalyzer { text -> if (analysisEnabled.get()) onScanned(text) },
                    )
                }

            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            provider?.unbindAll()
            analysisExecutor.shutdown()
        }
    }
}

@Composable
private fun MenuLinkDialog(
    result: MenuQrPayload.ScanResult.MenuLink,
    onDownload: () -> Unit,
    onOpenInApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    ScanDialog(title = stringResource(R.string.confim), onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.qr_menu_found, result.restaurantId),
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Button(
            onClick = onOpenInApp,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = myGreen),
        ) {
            Text(
                text = stringResource(R.string.qr_open_in_app),
                fontWeight = FontWeight.Bold,
                color = myYellow,
            )
        }
        Button(
            onClick = onDownload,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(50.dp),
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = myYellow),
        ) {
            Text(
                text = stringResource(R.string.qr_download_pdf),
                fontWeight = FontWeight.Bold,
                color = myGreen,
            )
        }
    }
}

@Composable
private fun InvalidQrDialog(onDismiss: () -> Unit) {
    ScanDialog(title = stringResource(R.string.QR), onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.qr_invalid_detail),
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.buttonColors(backgroundColor = myYellow),
        ) {
            Text(stringResource(R.string.ok), fontWeight = FontWeight.Bold, color = myGreen)
        }
    }
}

@Composable
private fun ScanDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colors.surface) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.onSurface,
                    )
                }
                content()
            }
        }
    }
}

@Composable
private fun PermissionDenied(showRetry: Boolean, onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.camera_permission_required),
            color = MaterialTheme.colors.onSurface,
            fontSize = 16.sp,
        )
        if (showRetry) {
            Button(
                onClick = onRequest,
                modifier = Modifier.padding(top = 20.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(backgroundColor = myYellow),
            ) {
                Text(stringResource(R.string.retry), color = myGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}
