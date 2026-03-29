package com.jaytt.moveandmeds.ui.info

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Photo
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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isProcessing = true
        try {
            // Copy to cache so the scan result screen can access the image
            val tempFile = File(context.cacheDir, "exercise_scan_temp.jpg")
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            val inputImage = InputImage.fromFilePath(context, uri)
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(inputImage)
                .addOnSuccessListener { visionText ->
                    isProcessing = false
                    val fullText = visionText.text
                    if (fullText.isBlank()) {
                        errorMessage = "No text found in image."
                    } else {
                        val encoded = URLEncoder.encode(fullText, StandardCharsets.UTF_8.name())
                        onScanResult(encoded)
                    }
                }
                .addOnFailureListener { e ->
                    isProcessing = false
                    errorMessage = "Text recognition failed: ${e.message}"
                }
        } catch (e: Exception) {
            isProcessing = false
            errorMessage = "Could not read image: ${e.message}"
        }
    }

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

            // Capture + gallery buttons
            if (!isProcessing) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Default.Photo,
                            contentDescription = "Pick from gallery",
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    FilledIconButton(
                        onClick = {
                            val capture = imageCaptureRef.value ?: return@FilledIconButton
                            isProcessing = true
                            capture.takePicture(
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                        try {
                                            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                            val bitmap = imageProxy.toBitmap()
                                            imageProxy.close()
                                            val matrix = Matrix()
                                            if (rotationDegrees != 0) matrix.postRotate(rotationDegrees.toFloat())
                                            val rotated = if (rotationDegrees != 0)
                                                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                            else bitmap
                                            val tempFile = File(context.cacheDir, "exercise_scan_temp.jpg")
                                            FileOutputStream(tempFile).use { out ->
                                                rotated.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                            }
                                            val inputImage = InputImage.fromBitmap(rotated, 0)
                                            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                                                .process(inputImage)
                                                .addOnSuccessListener { visionText ->
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
                                                    isProcessing = false
                                                    errorMessage = "Text recognition failed: ${e.message}"
                                                }
                                        } catch (e: Exception) {
                                            isProcessing = false
                                            errorMessage = "Could not read image from camera."
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
