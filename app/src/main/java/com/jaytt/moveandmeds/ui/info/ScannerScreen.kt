package com.jaytt.moveandmeds.ui.info

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onScanResult: (encodedText: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imageCaptureRef = remember { mutableStateOf<ImageCapture?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Document") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCaptureRef.value = imageCapture
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        } catch (e: Exception) {
                            Log.e("ScannerScreen", "Camera bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Loading overlay
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Recognising text…", color = Color.White)
                    }
                }
            }

            // Error snackbar area
            errorMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { errorMessage = null }) { Text("Dismiss") }
                    }
                ) {
                    Text(msg)
                }
            }

            // Capture button
            if (!isProcessing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp)
                ) {
                    FilledIconButton(
                        onClick = {
                            val capture = imageCaptureRef.value ?: return@FilledIconButton
                            isProcessing = true
                            capture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                        val mediaImage = imageProxy.image
                                        if (mediaImage == null) {
                                            imageProxy.close()
                                            isProcessing = false
                                            errorMessage = "Could not read image from camera."
                                            return
                                        }
                                        val inputImage = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )
                                        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                            .process(inputImage)
                                            .addOnSuccessListener { visionText ->
                                                imageProxy.close()
                                                isProcessing = false
                                                val fullText = visionText.text
                                                if (fullText.isBlank()) {
                                                    errorMessage = "No text found in image. Try again with better lighting."
                                                } else {
                                                    val encoded = URLEncoder.encode(
                                                        fullText,
                                                        StandardCharsets.UTF_8.name()
                                                    )
                                                    onScanResult(encoded)
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                imageProxy.close()
                                                isProcessing = false
                                                errorMessage = "Text recognition failed: ${e.message}"
                                            }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        isProcessing = false
                                        errorMessage = "Capture failed: ${exception.message}"
                                    }
                                }
                            )
                        },
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Camera,
                            contentDescription = "Capture",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}
