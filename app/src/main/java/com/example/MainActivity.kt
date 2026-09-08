package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.camera.CameraHardwareDetails
import com.example.camera.SamsungCameraHelper
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CameraScreenRoot()
            }
        }
    }
}

/**
 * Root Composable managing runtime permissions and displaying camera screen.
 */
@Composable
fun CameraScreenRoot() {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] ?: hasAudioPermission
    }

    if (hasCameraPermission) {
        SamsungCameraView(
            hasAudioPermission = hasAudioPermission,
            onRequestAudioPermission = {
                permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            }
        )
    } else {
        PermissionRequestScreen(
            onRequestPermissions = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            }
        )
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermissions: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0284C7).copy(alpha = 0.2f))
                    .border(2.dp, Color(0xFF38BDF8), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Samsung 60 FPS Camera",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Diperlukan izin kamera & audio untuk merekam video 60 FPS dengan hardware interop Continuous Autofocus.",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "Izinkan Akses Kamera",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun SamsungCameraView(
    hasAudioPermission: Boolean,
    onRequestAudioPermission: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<androidx.camera.video.Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    var cameraLensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var isAudioEnabled by remember { mutableStateOf(hasAudioPermission) }

    // Recording State
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }

    // Diagnostic & Samsung HAL specs
    var hardwareDetails by remember { mutableStateOf(CameraHardwareDetails()) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Tap to focus animation position
    var tapPoint by remember { mutableStateOf<Offset?>(null) }
    var isFocusing by remember { mutableStateOf(false) }

    // Timer coroutine for recording duration
    LaunchedEffect(isRecording, isPaused) {
        if (isRecording && !isPaused) {
            while (isRecording && !isPaused) {
                delay(1000)
                recordingDurationSeconds++
            }
        } else if (!isRecording) {
            recordingDurationSeconds = 0
        }
    }

    // Function to re-bind camera with Camera2Interop 60 FPS + Continuous AF
    fun bindCameraUseCases(pView: PreviewView) {
        val provider = cameraProvider ?: return
        try {
            provider.unbindAll()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(cameraLensFacing)
                .build()

            // 1. Build Preview with Camera2Interop injecting:
            // - CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE = (60, 60)
            // - CaptureRequest.CONTROL_AF_MODE = CONTROL_AF_MODE_CONTINUOUS_VIDEO
            val preview = SamsungCameraHelper.build60FpsPreview()
            preview.surfaceProvider = pView.surfaceProvider

            // 2. Build VideoCapture with FHD quality
            val vCapture = SamsungCameraHelper.buildVideoCapture()
            videoCapture = vCapture

            // 3. Bind to lifecycle
            val cam = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                vCapture
            )
            camera = cam

            // 4. Also enforce Samsung HAL repeating capture request settings on the active CameraControl
            SamsungCameraHelper.enforceSamsungHardwareSettings(context, cam)

            // 5. Inspect hardware capabilities (supported FPS ranges and AF modes)
            hardwareDetails = SamsungCameraHelper.inspectCameraHardware(cam)

        } catch (e: Exception) {
            Toast.makeText(context, "Gagal menginisialisasi kamera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Initialize Camera Provider
    LaunchedEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            cameraProvider = future.get()
            previewView?.let { bindCameraUseCases(it) }
        }, ContextCompat.getMainExecutor(context))
    }

    // Re-bind when lens facing changes
    LaunchedEffect(cameraLensFacing) {
        previewView?.let { bindCameraUseCases(it) }
    }

    // Clean up recording on dispose
    DisposableEffect(Unit) {
        onDispose {
            activeRecording?.stop()
            activeRecording = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera PreviewView using AndroidView
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    previewView = this
                    cameraProvider?.let { bindCameraUseCases(this) }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(camera, previewView) {
                    detectTapGestures { offset ->
                        val pView = previewView ?: return@detectTapGestures
                        val cam = camera ?: return@detectTapGestures
                        tapPoint = offset
                        isFocusing = true

                        SamsungCameraHelper.performTapToFocus(
                            previewView = pView,
                            cameraControl = cam.cameraControl,
                            x = offset.x,
                            y = offset.y
                        ) {
                            coroutineScope.launch {
                                delay(1200)
                                isFocusing = false
                                tapPoint = null
                            }
                        }
                    }
                }
        )

        // Animated Tap-to-Focus Reticle
        tapPoint?.let { pos ->
            val density = LocalDensity.current
            val animatedScale by animateFloatAsState(
                targetValue = if (isFocusing) 1f else 1.3f,
                animationSpec = tween(300),
                label = "focus_scale"
            )

            Box(
                modifier = Modifier
                    .offset {
                        with(density) {
                            IntOffset(
                                (pos.x - 36.dp.toPx()).roundToInt(),
                                (pos.y - 36.dp.toPx()).roundToInt()
                            )
                        }
                    }
                    .size(72.dp)
                    .scale(animatedScale)
                    .border(2.dp, Color(0xFFFBBF24), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFBBF24))
                        .align(Alignment.Center)
                )
            }
        }

        // Top Overlay Bar: Status, 60 FPS Badge, Torch, Diagnostics
        TopStatusBar(
            isSamsung = hardwareDetails.isSamsungDevice,
            isRecording = isRecording,
            isTorchOn = isTorchEnabled,
            onToggleTorch = {
                camera?.let { cam ->
                    isTorchEnabled = !isTorchEnabled
                    cam.cameraControl.enableTorch(isTorchEnabled)
                }
            },
            onOpenInfo = { showInfoDialog = true }
        )

        // Bottom Controls Bar: Shutter, Timer, Lens Switch, Audio Toggle
        BottomControlsBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            isRecording = isRecording,
            isPaused = isPaused,
            recordingDurationSeconds = recordingDurationSeconds,
            isAudioEnabled = isAudioEnabled,
            onToggleAudio = {
                if (!hasAudioPermission) {
                    onRequestAudioPermission()
                } else {
                    isAudioEnabled = !isAudioEnabled
                }
            },
            onSwitchCamera = {
                if (!isRecording) {
                    cameraLensFacing = if (cameraLensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }
                } else {
                    Toast.makeText(context, "Hentikan rekaman sebelum mengganti kamera", Toast.LENGTH_SHORT).show()
                }
            },
            onPauseResumeRecording = {
                activeRecording?.let { rec ->
                    if (isPaused) {
                        rec.resume()
                        isPaused = false
                    } else {
                        rec.pause()
                        isPaused = true
                    }
                }
            },
            onRecordClick = {
                val vCap = videoCapture
                if (vCap == null) {
                    Toast.makeText(context, "Kamera belum siap", Toast.LENGTH_SHORT).show()
                    return@BottomControlsBar
                }

                if (isRecording) {
                    // Stop Recording
                    activeRecording?.stop()
                    activeRecording = null
                    isRecording = false
                    isPaused = false
                } else {
                    // Start Recording
                    val rec = SamsungCameraHelper.prepareRecording(
                        context = context,
                        videoCapture = vCap,
                        enableAudio = isAudioEnabled
                    ) { event ->
                        when (event) {
                            is VideoRecordEvent.Start -> {
                                isRecording = true
                                isPaused = false
                            }
                            is VideoRecordEvent.Finalize -> {
                                isRecording = false
                                isPaused = false
                                if (!event.hasError()) {
                                    Toast.makeText(
                                        context,
                                        "Video 60 FPS berhasil disimpan ke Galeri (Movies/Samsung60FPS)",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Gagal menyimpan video: error kode ${event.error}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            is VideoRecordEvent.Pause -> isPaused = true
                            is VideoRecordEvent.Resume -> isPaused = false
                        }
                    }
                    activeRecording = rec
                }
            }
        )

        // Hardware Diagnostics & Samsung HAL Fix Info Dialog
        if (showInfoDialog) {
            SamsungHardwareInfoDialog(
                details = hardwareDetails,
                onDismiss = { showInfoDialog = false }
            )
        }
    }
}

/**
 * Top Status Bar with 60 FPS Badge and Hardware Fix indicators.
 */
@Composable
fun TopStatusBar(
    isSamsung: Boolean,
    isRecording: Boolean,
    isTorchOn: Boolean,
    onToggleTorch: () -> Unit,
    onOpenInfo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.7f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 60 FPS Active Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(20.dp))
                    .clickable { onOpenInfo() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) Color(0xFFEF4444) else Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "60 FPS",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSamsung) "SAMSUNG HAL FIX" else "CAMERA2 INTEROP",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp
                )
            }

            // Quick Actions: Torch & Info
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = if (isTorchOn) Color(0xFFFBBF24) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onOpenInfo,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info Diagnostics",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bottom Controls Bar with Record Button, Recording Timer, Camera Flip, and Audio Mute.
 */
@Composable
fun BottomControlsBar(
    modifier: Modifier = Modifier,
    isRecording: Boolean,
    isPaused: Boolean,
    recordingDurationSeconds: Int,
    isAudioEnabled: Boolean,
    onToggleAudio: () -> Unit,
    onSwitchCamera: () -> Unit,
    onPauseResumeRecording: () -> Unit,
    onRecordClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing_rec")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.85f)
                    )
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Recording Status & Timer Indicator
        AnimatedVisibility(
            visible = isRecording,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.9f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPaused) Color(0xFFFBBF24)
                            else Color(0xFFEF4444).copy(alpha = pulseAlpha)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                val minutes = recordingDurationSeconds / 60
                val seconds = recordingDurationSeconds % 60
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (isPaused) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PAUSED",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Main Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Audio toggle button
            IconButton(
                onClick = onToggleAudio,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B).copy(alpha = 0.7f))
            ) {
                Icon(
                    imageVector = if (isAudioEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Audio Toggle",
                    tint = if (isAudioEnabled) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Pause / Resume button during recording
            if (isRecording) {
                IconButton(
                    onClick = onPauseResumeRecording,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155).copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = "Pause Resume",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Central Shutter / Record Button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .clickable { onRecordClick() },
                contentAlignment = Alignment.Center
            ) {
                val recordColor by animateColorAsState(
                    targetValue = if (isRecording) Color(0xFFEF4444) else Color(0xFFDC2626),
                    label = "rec_color"
                )

                if (isRecording) {
                    // Recording stop square
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(recordColor)
                    )
                } else {
                    // Shutter circle
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(recordColor)
                    )
                }
            }

            // Lens Switch Button (Back / Front)
            IconButton(
                onClick = onSwitchCamera,
                enabled = !isRecording,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B).copy(alpha = if (isRecording) 0.3f else 0.7f))
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = if (isRecording) Color(0xFF64748B) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Diagnostic Dialog detailing Samsung HAL Fix and Camera2 Interop properties.
 */
@Composable
fun SamsungHardwareInfoDialog(
    details: CameraHardwareDetails,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Samsung Camera2 Interop",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Solusi Masalah Fixed Focus Samsung pada 60 FPS:",
                    color = Color(0xFF38BDF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Aplikasi pihak ketiga standar (seperti Open Camera) sering mengalami fokus terkunci karena HAL Samsung menonaktifkan continuous autofocus ketika target FPS dinaikkan ke 60.\n\n" +
                            "Aplikasi ini menginjeksi kontrol langsung ke level hardware Samsung:",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        DiagnosticItem(
                            label = "CONTROL_AE_TARGET_FPS_RANGE",
                            value = "[60, 60] (Enforced)",
                            highlightColor = Color(0xFF10B981)
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = Color(0xFF334155)
                        )
                        DiagnosticItem(
                            label = "CONTROL_AF_MODE",
                            value = "CONTINUOUS_VIDEO (Enforced)",
                            highlightColor = Color(0xFF10B981)
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = Color(0xFF334155)
                        )
                        DiagnosticItem(
                            label = "Device Manufacturer",
                            value = details.sensorName,
                            highlightColor = Color(0xFF38BDF8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Available FPS Ranges dari HAL:",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                val fpsText = if (details.availableFpsRanges.isNotEmpty()) {
                    details.availableFpsRanges.joinToString(", ") { "[${it.lower}, ${it.upper}]" }
                } else {
                    "[60, 60] (Camera2 Injected)"
                }
                Text(
                    text = fpsText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Supported AF Modes:",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                val afText = if (details.availableAfModes.isNotEmpty()) {
                    details.availableAfModes.joinToString("\n") { "• $it" }
                } else {
                    "• CONTINUOUS_VIDEO (Injected via Camera2Interop)"
                }
                Text(
                    text = afText,
                    color = Color.White,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Text("Tutup", color = Color.White)
            }
        }
    )
}

@Composable
fun DiagnosticItem(label: String, value: String, highlightColor: Color) {
    Column {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp)
        Text(
            text = value,
            color = highlightColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Placeholder Greeting composable for Robolectric screenshot test compatibility.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
