package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Range
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MotionPhotosAuto
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.camera.BitrateMode
import com.example.camera.CameraCaptureMode
import com.example.camera.CameraHardwareDetails
import com.example.camera.CapturedMediaItem
import com.example.camera.FpsMode
import com.example.camera.ResolutionMode
import com.example.camera.SamsungCameraHelper
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
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
                text = "Samsung Camera2, 60 FPS & EIS",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Izin kamera & audio dibutuhkan untuk kontrol Camera2 API tingkat rendah, perekaman 60 FPS nyata, stabilisasi video EIS/OIS, dan penyimpanan DCIM/Camera.",
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
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<androidx.camera.video.Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // User Selection States
    var captureMode by remember { mutableStateOf(CameraCaptureMode.PHOTO) }
    var isCamera2ApiEnabled by remember { mutableStateOf(true) }
    var isVideoStabilizationEnabled by remember { mutableStateOf(true) }
    var selectedFps by remember { mutableStateOf(FpsMode.FPS_60) }
    var selectedResolution by remember { mutableStateOf(ResolutionMode.RES_1080P) }
    var selectedBitrate by remember { mutableStateOf(BitrateMode.BITRATE_100) }
    var isFpsLockEnabled by remember { mutableStateOf(true) }
    var cameraLensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    var isTorchEnabled by remember { mutableStateOf(false) }
    var isAudioEnabled by remember { mutableStateOf(hasAudioPermission) }

    // Recording State
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }

    // Photo Capture Flash Effect & Media Gallery
    var showPhotoFlash by remember { mutableStateOf(false) }
    var recentMediaList by remember { mutableStateOf<List<CapturedMediaItem>>(emptyList()) }
    var showGalleryViewer by remember { mutableStateOf(false) }

    // Hardware specs & Diagnostic & Open Camera Settings
    var hardwareDetails by remember { mutableStateOf(CameraHardwareDetails()) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var activeFpsRange by remember { mutableStateOf<Range<Int>?>(null) }

    // Tap to focus state
    var tapPoint by remember { mutableStateOf<Offset?>(null) }
    var isFocusing by remember { mutableStateOf(false) }
    var isRefocusingAnimation by remember { mutableStateOf(false) }

    // Function to reload recent media from DCIM/Camera
    fun refreshRecentMedia() {
        coroutineScope.launch {
            val list = SamsungCameraHelper.queryRecentMediaList(context, limit = 30)
            recentMediaList = list
        }
    }

    // Query recent media on startup
    LaunchedEffect(Unit) {
        refreshRecentMedia()
    }

    // Timer coroutine for recording
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

    // Bind camera use cases with Camera2 API and Open Camera 60 FPS approach
    fun bindCameraUseCases(pView: PreviewView) {
        val provider = cameraProvider ?: return
        try {
            provider.unbindAll()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(cameraLensFacing)
                .build()

            // Pre-inspect camera to get supported FPS ranges on this sensor
            val initialCam = provider.bindToLifecycle(lifecycleOwner, cameraSelector)
            val initialSpecs = SamsungCameraHelper.inspectCameraHardware(
                camera = initialCam,
                activeRange = null,
                isCamera2Enabled = isCamera2ApiEnabled,
                isStabilizationActive = isVideoStabilizationEnabled
            )
            provider.unbindAll()

            // Resolve real hardware-supported range for the selected FPS mode (checks 120 FPS high-speed too)
            val optimalRange = SamsungCameraHelper.resolveOptimalFpsRange(
                supportedRanges = initialSpecs.availableFpsRanges,
                highSpeedRanges = initialSpecs.highSpeedFpsRanges,
                requestedFps = selectedFps
            )
            activeFpsRange = optimalRange

            val isPhoto = (captureMode == CameraCaptureMode.PHOTO)

            // 1. Build Preview with Camera2 API and EIS/OIS stabilization
            val preview = SamsungCameraHelper.buildPreview(
                targetFpsRange = optimalRange,
                isPhotoMode = isPhoto,
                enableCamera2Api = isCamera2ApiEnabled,
                enableStabilization = isVideoStabilizationEnabled,
                lockFpsAntiDrop = isFpsLockEnabled
            )
            preview.surfaceProvider = pView.surfaceProvider

            val boundCamera: Camera
            if (isPhoto) {
                // Photo mode: Bind Preview + ImageCapture
                val imgCapture = SamsungCameraHelper.buildImageCapture()
                imageCapture = imgCapture
                videoCapture = null

                boundCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imgCapture
                )
            } else {
                // Video mode: Bind Preview + VideoCapture with Open Camera explicit 60 FPS, Bitrate & EIS/OIS
                val vCapture = SamsungCameraHelper.buildVideoCapture(
                    resolutionMode = selectedResolution,
                    targetFpsRange = optimalRange,
                    bitrateMode = selectedBitrate,
                    enableCamera2Api = isCamera2ApiEnabled,
                    enableStabilization = isVideoStabilizationEnabled,
                    lockFpsAntiDrop = isFpsLockEnabled
                )
                videoCapture = vCapture
                imageCapture = null

                boundCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    vCapture
                )
            }

            camera = boundCamera

            // 2. Apply Camera2 Hardware Controls (Active AF, FPS range, Anti-Drop & Stabilization)
            SamsungCameraHelper.applyActiveHardwareSettings(
                context = context,
                camera = boundCamera,
                targetFpsRange = optimalRange,
                isPhotoMode = isPhoto,
                enableCamera2Api = isCamera2ApiEnabled,
                enableStabilization = isVideoStabilizationEnabled,
                lockFpsAntiDrop = isFpsLockEnabled
            )

            // 3. Update hardware specs for diagnostic display
            hardwareDetails = SamsungCameraHelper.inspectCameraHardware(
                camera = boundCamera,
                activeRange = optimalRange,
                isCamera2Enabled = isCamera2ApiEnabled,
                isStabilizationActive = isVideoStabilizationEnabled
            )

        } catch (e: Exception) {
            Toast.makeText(context, "Inisialisasi kamera: ${e.message}", Toast.LENGTH_SHORT).show()
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

    // Re-bind when settings change
    LaunchedEffect(
        captureMode,
        isCamera2ApiEnabled,
        isVideoStabilizationEnabled,
        selectedFps,
        selectedResolution,
        selectedBitrate,
        isFpsLockEnabled,
        cameraLensFacing
    ) {
        if (!isRecording) {
            previewView?.let { bindCameraUseCases(it) }
        }
    }

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
        // Camera Preview Viewfinder
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
                        val cam = camera ?: return@detectTapGestures
                        val pView = previewView ?: return@detectTapGestures
                        tapPoint = offset
                        isFocusing = true
                        SamsungCameraHelper.performTapToFocus(pView, cam.cameraControl, offset.x, offset.y) {
                            isFocusing = false
                        }
                    }
                }
        )

        // Shutter White Flash Effect
        if (showPhotoFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.85f))
            )
        }

        // Tap-to-Focus Reticle
        tapPoint?.let { point ->
            val density = LocalDensity.current
            val animatedScale by animateFloatAsState(
                targetValue = if (isFocusing) 1.25f else 0.95f,
                animationSpec = tween(durationMillis = 250),
                label = "focus_scale"
            )

            Box(
                modifier = Modifier
                    .offset {
                        with(density) {
                            IntOffset(
                                (point.x - 36.dp.toPx()).roundToInt(),
                                (point.y - 36.dp.toPx()).roundToInt()
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

        // Floating "Force Re-Focus" Button
        val refocusScale by animateFloatAsState(
            targetValue = if (isRefocusingAnimation) 1.3f else 1f,
            animationSpec = tween(200),
            label = "refocus_scale"
        )
        IconButton(
            onClick = {
                camera?.let { cam ->
                    isRefocusingAnimation = true
                    SamsungCameraHelper.triggerAutofocus(cam, previewView) {
                        Toast.makeText(context, "Autofocus di-reset (Re-focusing)", Toast.LENGTH_SHORT).show()
                    }
                    coroutineScope.launch {
                        delay(400)
                        isRefocusingAnimation = false
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .scale(refocusScale)
                .clip(CircleShape)
                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                .border(1.5.dp, Color(0xFF38BDF8), CircleShape)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = "Trigger Autofocus",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(24.dp)
            )
        }

        // Stabilization Active Indicator Badge (Visible on Video Mode when active)
        if (captureMode == CameraCaptureMode.VIDEO && isVideoStabilizationEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.8f))
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MotionPhotosAuto,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "STABILISASI (EIS+OIS) AKTIF",
                        color = Color(0xFF10B981),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Top Control Bar with Camera2 API, EIS Stabilization, Resolution, FPS, Bitrate, Flash, Settings, Diagnostics
        TopHeaderBar(
            captureMode = captureMode,
            isCamera2ApiEnabled = isCamera2ApiEnabled,
            isVideoStabilizationEnabled = isVideoStabilizationEnabled,
            hardwareLevel = hardwareDetails.hardwareLevel,
            selectedResolution = selectedResolution,
            selectedFps = selectedFps,
            selectedBitrate = selectedBitrate,
            activeFpsRange = activeFpsRange,
            isRecording = isRecording,
            isTorchOn = isTorchEnabled,
            onToggleCamera2Api = {
                if (!isRecording) {
                    isCamera2ApiEnabled = !isCamera2ApiEnabled
                    val status = if (isCamera2ApiEnabled) "AKTIF (Continuous AF, 60/120 FPS & Bitrate 100M)" else "NONAKTIF (Standard CameraX)"
                    Toast.makeText(context, "Camera2 API: $status", Toast.LENGTH_SHORT).show()
                }
            },
            onToggleStabilization = {
                isVideoStabilizationEnabled = !isVideoStabilizationEnabled
                val status = if (isVideoStabilizationEnabled) "Stabilisasi Video (EIS/OIS) Diaktifkan" else "Stabilisasi Dimatikan"
                Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
            },
            onSelectResolution = { selectedResolution = it },
            onSelectFps = { selectedFps = it },
            onSelectBitrate = { selectedBitrate = it },
            onToggleTorch = {
                camera?.let { cam ->
                    isTorchEnabled = !isTorchEnabled
                    cam.cameraControl.enableTorch(isTorchEnabled)
                }
            },
            onOpenSettings = { showSettingsDialog = true },
            onOpenInfo = { showInfoDialog = true }
        )

        // Bottom Bar with Gallery Thumbnail Button, Mode Selector, and Shutter Controls
        val latestMediaItem = recentMediaList.firstOrNull()

        BottomSectionControls(
            modifier = Modifier.align(Alignment.BottomCenter),
            captureMode = captureMode,
            latestMedia = latestMediaItem,
            onOpenGallery = {
                refreshRecentMedia()
                showGalleryViewer = true
            },
            onModeSelected = { newMode ->
                if (!isRecording) {
                    captureMode = newMode
                }
            },
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
                    Toast.makeText(context, "Hentikan rekaman sebelum membalik kamera", Toast.LENGTH_SHORT).show()
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
            onPhotoShutterClick = {
                val imgCap = imageCapture
                if (imgCap == null) {
                    Toast.makeText(context, "Kamera foto belum siap", Toast.LENGTH_SHORT).show()
                    return@BottomSectionControls
                }
                showPhotoFlash = true
                coroutineScope.launch {
                    delay(80)
                    showPhotoFlash = false
                }
                SamsungCameraHelper.takePhoto(
                    context = context,
                    imageCapture = imgCap,
                    onSuccess = { uri ->
                        refreshRecentMedia()
                        Toast.makeText(context, "Foto tersimpan di Galeri DCIM/Camera", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, "Gagal mengambil foto: $err", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onVideoShutterClick = {
                val vCap = videoCapture
                if (vCap == null) {
                    Toast.makeText(context, "Kamera video belum siap", Toast.LENGTH_SHORT).show()
                    return@BottomSectionControls
                }

                if (isRecording) {
                    activeRecording?.stop()
                    activeRecording = null
                    isRecording = false
                    isPaused = false
                    refreshRecentMedia()
                } else {
                    val fpsTag = activeFpsRange?.upper?.toString() ?: selectedFps.targetFps.toString()
                    val bitrateTag = "${selectedBitrate.bps / 1_000_000}MBPS"
                    val rec = SamsungCameraHelper.prepareRecording(
                        context = context,
                        videoCapture = vCap,
                        fpsLabel = "${selectedResolution.displayName}_${fpsTag}FPS_${bitrateTag}",
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
                                refreshRecentMedia()
                                if (!event.hasError()) {
                                    Toast.makeText(
                                        context,
                                        "Video tersimpan di Galeri DCIM/Camera",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Perekaman selesai dengan catatan: ${event.error}",
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

        // Modern In-App Media Gallery Dialog (Resolves the empty folder issue)
        if (showGalleryViewer) {
            ModernMediaGalleryDialog(
                mediaList = recentMediaList,
                onDismiss = { showGalleryViewer = false }
            )
        }

        // Diagnostic Dialog
        if (showInfoDialog) {
            HardwareInfoDialog(
                details = hardwareDetails,
                isCamera2Enabled = isCamera2ApiEnabled,
                isStabilizationEnabled = isVideoStabilizationEnabled,
                onToggleCamera2 = {
                    isCamera2ApiEnabled = !isCamera2ApiEnabled
                },
                onToggleStabilization = {
                    isVideoStabilizationEnabled = !isVideoStabilizationEnabled
                },
                onDismiss = { showInfoDialog = false }
            )
        }

        // Open Camera Comprehensive Settings Dialog
        if (showSettingsDialog) {
            OpenCameraSettingsDialog(
                isCamera2Enabled = isCamera2ApiEnabled,
                onToggleCamera2 = {
                    if (!isRecording) {
                        isCamera2ApiEnabled = !isCamera2ApiEnabled
                    }
                },
                selectedBitrate = selectedBitrate,
                onSelectBitrate = { selectedBitrate = it },
                selectedFps = selectedFps,
                onSelectFps = { selectedFps = it },
                selectedResolution = selectedResolution,
                onSelectResolution = { selectedResolution = it },
                isFpsLockEnabled = isFpsLockEnabled,
                onToggleFpsLock = { isFpsLockEnabled = !isFpsLockEnabled },
                isStabilizationEnabled = isVideoStabilizationEnabled,
                onToggleStabilization = { isVideoStabilizationEnabled = !isVideoStabilizationEnabled },
                isAudioEnabled = isAudioEnabled,
                onToggleAudio = {
                    if (!hasAudioPermission) {
                        onRequestAudioPermission()
                    } else {
                        isAudioEnabled = !isAudioEnabled
                    }
                },
                hardwareDetails = hardwareDetails,
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

@Composable
fun TopHeaderBar(
    captureMode: CameraCaptureMode,
    isCamera2ApiEnabled: Boolean,
    isVideoStabilizationEnabled: Boolean,
    hardwareLevel: String,
    selectedResolution: ResolutionMode,
    selectedFps: FpsMode,
    selectedBitrate: BitrateMode,
    activeFpsRange: Range<Int>?,
    isRecording: Boolean,
    isTorchOn: Boolean,
    onToggleCamera2Api: () -> Unit,
    onToggleStabilization: () -> Unit,
    onSelectResolution: (ResolutionMode) -> Unit,
    onSelectFps: (FpsMode) -> Unit,
    onSelectBitrate: (BitrateMode) -> Unit,
    onToggleTorch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenInfo: () -> Unit
) {
    var showResolutionMenu by remember { mutableStateOf(false) }
    var showFpsMenu by remember { mutableStateOf(false) }
    var showBitrateMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.88f), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left row: Camera2 API toggle pill & FPS/Resolution/Bitrate/EIS
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Camera2 API Toggle Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isCamera2ApiEnabled) Color(0xFF0284C7).copy(alpha = 0.9f)
                            else Color(0xFF334155).copy(alpha = 0.85f)
                        )
                        .border(
                            1.dp,
                            if (isCamera2ApiEnabled) Color(0xFF38BDF8) else Color(0xFF64748B),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = !isRecording) { onToggleCamera2Api() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = if (isCamera2ApiEnabled) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                        contentDescription = "Toggle Camera2 API",
                        tint = if (isCamera2ApiEnabled) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isCamera2ApiEnabled) "Camera2: ON" else "Camera2: OFF",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Video Stabilization EIS/OIS Toggle Pill
                if (captureMode == CameraCaptureMode.VIDEO) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isVideoStabilizationEnabled) Color(0xFF059669).copy(alpha = 0.9f)
                                else Color(0xFF334155).copy(alpha = 0.85f)
                            )
                            .border(
                                1.dp,
                                if (isVideoStabilizationEnabled) Color(0xFF34D399) else Color(0xFF64748B),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable(enabled = !isRecording) { onToggleStabilization() }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MotionPhotosAuto,
                            contentDescription = "Stabilizer EIS/OIS",
                            tint = if (isVideoStabilizationEnabled) Color.White else Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isVideoStabilizationEnabled) "EIS: ON" else "EIS: OFF",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bitrate Dropdown Button (Enabled for Video)
                if (captureMode == CameraCaptureMode.VIDEO) {
                    Box {
                        val bitrateLabel = when (selectedBitrate) {
                            BitrateMode.BITRATE_100 -> "100M"
                            BitrateMode.BITRATE_50 -> "50M"
                            BitrateMode.BITRATE_150 -> "150M"
                            BitrateMode.BITRATE_200 -> "200M"
                            BitrateMode.BITRATE_DEFAULT -> "Auto"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1E293B).copy(alpha = 0.9f))
                                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
                                .clickable(enabled = !isRecording) { showBitrateMenu = true }
                                .padding(horizontal = 7.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = bitrateLabel,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        DropdownMenu(
                            expanded = showBitrateMenu,
                            onDismissRequest = { showBitrateMenu = false }
                        ) {
                            BitrateMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = mode.displayName,
                                                    fontWeight = if (mode == selectedBitrate) FontWeight.Bold else FontWeight.Normal
                                                )
                                                if (mode == BitrateMode.BITRATE_100) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "OPEN CAMERA",
                                                        color = Color(0xFF10B981),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${mode.description} (${mode.approxPerTenSec}/10s)",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    },
                                    onClick = {
                                        onSelectBitrate(mode)
                                        showBitrateMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Resolution Dropdown Button (Enabled for Video)
                if (captureMode == CameraCaptureMode.VIDEO) {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1E293B).copy(alpha = 0.9f))
                                .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(16.dp))
                                .clickable(enabled = !isRecording) { showResolutionMenu = true }
                                .padding(horizontal = 7.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = selectedResolution.displayName,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        DropdownMenu(
                            expanded = showResolutionMenu,
                            onDismissRequest = { showResolutionMenu = false }
                        ) {
                            ResolutionMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(mode.displayName, fontWeight = FontWeight.Bold)
                                            Text(mode.description, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    },
                                    onClick = {
                                        onSelectResolution(mode)
                                        showResolutionMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // FPS Dropdown Button
                Box {
                    val fpsLabel = when (selectedFps) {
                        FpsMode.FPS_120 -> activeFpsRange?.let { "${it.upper} FPS" } ?: "120 FPS"
                        FpsMode.FPS_60 -> activeFpsRange?.let { "${it.upper} FPS" } ?: "60 FPS"
                        FpsMode.FPS_30 -> "30 FPS"
                        FpsMode.FPS_AUTO -> "AUTO FPS"
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.9f))
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(16.dp))
                            .clickable(enabled = !isRecording) { showFpsMenu = true }
                            .padding(horizontal = 7.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = fpsLabel,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    DropdownMenu(
                        expanded = showFpsMenu,
                        onDismissRequest = { showFpsMenu = false }
                    ) {
                        FpsMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = mode.displayName,
                                        fontWeight = if (mode == selectedFps) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    onSelectFps(mode)
                                    showFpsMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Right icons: Flash, Open Camera Settings, Info Diagnostics
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = if (isTorchOn) Color(0xFFFBBF24) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0284C7).copy(alpha = 0.85f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Pengaturan Open Camera",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onOpenInfo,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info Diagnostics",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomSectionControls(
    modifier: Modifier = Modifier,
    captureMode: CameraCaptureMode,
    latestMedia: CapturedMediaItem?,
    onOpenGallery: () -> Unit,
    onModeSelected: (CameraCaptureMode) -> Unit,
    isRecording: Boolean,
    isPaused: Boolean,
    recordingDurationSeconds: Int,
    isAudioEnabled: Boolean,
    onToggleAudio: () -> Unit,
    onSwitchCamera: () -> Unit,
    onPauseResumeRecording: () -> Unit,
    onPhotoShutterClick: () -> Unit,
    onVideoShutterClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing_rec")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Video Duration Timer (Visible only when recording)
        AnimatedVisibility(
            visible = isRecording,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.95f))
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

        Spacer(modifier = Modifier.height(10.dp))

        // Mode Switcher: FOTO | VIDEO (Disabled during recording)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            val isPhoto = (captureMode == CameraCaptureMode.PHOTO)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isPhoto) Color(0xFF0284C7) else Color.Transparent)
                    .clickable(enabled = !isRecording) { onModeSelected(CameraCaptureMode.PHOTO) }
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = if (isPhoto) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "FOTO",
                        color = if (isPhoto) Color.White else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (!isPhoto) Color(0xFFEF4444) else Color.Transparent)
                    .clickable(enabled = !isRecording) { onModeSelected(CameraCaptureMode.VIDEO) }
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = if (!isPhoto) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "VIDEO",
                        color = if (!isPhoto) Color.White else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Main Controls Row: Gallery Thumbnail, Shutter Button, Switch Camera
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Button: Gallery Thumbnail Button with Live Preview
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                    .border(2.dp, Color(0xFF0284C7), CircleShape)
                    .clickable { onOpenGallery() },
                contentAlignment = Alignment.Center
            ) {
                if (latestMedia != null) {
                    AsyncImage(
                        model = latestMedia.uri,
                        contentDescription = "Hasil Kamera Terakhir",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (latestMedia.isVideo) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleFilled,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Buka Galeri",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Audio mic toggle (for video) or Pause button
            if (isRecording) {
                IconButton(
                    onClick = onPauseResumeRecording,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155).copy(alpha = 0.85f))
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = "Pause Resume",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else if (captureMode == CameraCaptureMode.VIDEO) {
                IconButton(
                    onClick = onToggleAudio,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = if (isAudioEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Audio Toggle",
                        tint = if (isAudioEnabled) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            // Central Shutter Button
            if (captureMode == CameraCaptureMode.PHOTO) {
                // Photo Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .clickable { onPhotoShutterClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            } else {
                // Video Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .clickable { onVideoShutterClick() },
                    contentAlignment = Alignment.Center
                ) {
                    val recordColor by animateColorAsState(
                        targetValue = if (isRecording) Color(0xFFEF4444) else Color(0xFFDC2626),
                        label = "rec_color"
                    )

                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(recordColor)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(recordColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(48.dp))

            // Right Button: Camera Switcher (Back / Front)
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
 * Modern In-App Media Gallery Dialog.
 * Directly fixes the "folder ternyata kosong" issue by displaying all photos & videos taken
 * in DCIM/Camera with an in-app viewer, and opening the exact URI in Samsung Gallery.
 */
@Composable
fun ModernMediaGalleryDialog(
    mediaList: List<CapturedMediaItem>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedIndex by remember { mutableIntStateOf(0) }

    val currentItem = mediaList.getOrNull(selectedIndex) ?: mediaList.firstOrNull()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090D16))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Galeri Hasil Kamera",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (mediaList.isNotEmpty()) "${mediaList.size} media di folder DCIM/Camera" else "Folder DCIM/Camera",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White
                        )
                    }
                }

                if (currentItem != null) {
                    // Preview Area for Selected Media
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = currentItem.uri,
                            contentDescription = currentItem.displayName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        )

                        if (currentItem.isVideo) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .border(2.dp, Color.White, CircleShape)
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(currentItem.uri, "video/*")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Tidak dapat memutar video", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircleFilled,
                                    contentDescription = "Putar Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                        }
                    }

                    // Metadata Info Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = currentItem.displayName,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            val typeLabel = if (currentItem.isVideo) "VIDEO" else "FOTO"
                            Text(
                                text = typeLabel,
                                color = if (currentItem.isVideo) Color(0xFFEF4444) else Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        val formattedDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(currentItem.dateAddedMillis))
                        Text(
                            text = "Lokasi: Internal Storage > DCIM > Camera • $formattedDate",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    // Horizontal Thumbnail Strip of Captured Items
                    if (mediaList.size > 1) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(mediaList) { index, item ->
                                val isSelected = (index == selectedIndex)
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedIndex = index }
                                ) {
                                    AsyncImage(
                                        model = item.uri,
                                        contentDescription = item.displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (item.isVideo) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircleFilled,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .align(Alignment.BottomEnd)
                                                .padding(2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Action Bar: Open in Gallery & Share
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        val mime = if (currentItem.isVideo) "video/*" else "image/*"
                                        setDataAndType(currentItem.uri, mime)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Tidak dapat membuka aplikasi galeri", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka di Galeri", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = if (currentItem.isVideo) "video/*" else "image/*"
                                        putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Bagikan Hasil Kamera"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal membagikan media", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7)))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bagikan")
                        }
                    }
                } else {
                    // Empty State Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Belum Ada Foto atau Video",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Foto dan video yang Anda ambil akan tersimpan langsung di folder DCIM/Camera perangkat Samsung ini dan akan muncul otomatis di Galeri bawaan.",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Ambil Foto Sekarang")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HardwareInfoDialog(
    details: CameraHardwareDetails,
    isCamera2Enabled: Boolean,
    isStabilizationEnabled: Boolean,
    onToggleCamera2: () -> Unit,
    onToggleStabilization: () -> Unit,
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
                    text = "Status Sensor & Camera2",
                    color = Color.White,
                    fontSize = 17.sp,
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
                // Camera2 API Switch Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Camera2 API Interop",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isCamera2Enabled) "Aktif: Inject continuous autofocus & 60 FPS range langsung ke HAL" else "Nonaktif: Gunakan CameraX standard pipeline",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        Switch(
                            checked = isCamera2Enabled,
                            onCheckedChange = { onToggleCamera2() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0284C7)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Video Stabilization (EIS / OIS) Switch Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Stabilisasi Video (EIS / OIS)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isStabilizationEnabled) "Aktif: Meredam getaran tangan secara real-time via hardware giroskop sensor" else "Nonaktif: Rekaman tanpa stabilisasi hardware",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        Switch(
                            checked = isStabilizationEnabled,
                            onCheckedChange = { onToggleStabilization() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        DiagItem(
                            label = "Camera2 Hardware Level",
                            value = details.hardwareLevel,
                            highlightColor = if (details.hardwareLevel in listOf("FULL", "LEVEL_3")) Color(0xFF10B981) else Color(0xFFFBBF24)
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = Color(0xFF334155)
                        )
                        DiagItem(
                            label = "Active FPS Range",
                            value = details.activeFpsRange?.let { "[${it.lower}, ${it.upper}]" } ?: "Auto Negotiated",
                            highlightColor = Color(0xFF10B981)
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = Color(0xFF334155)
                        )
                        DiagItem(
                            label = "Stabilisasi Elektronik (EIS)",
                            value = if (details.isEisSupported) "SUPPORTED (Hardware EIS)" else "N/A",
                            highlightColor = if (details.isEisSupported) Color(0xFF10B981) else Color(0xFF94A3B8)
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = Color(0xFF334155)
                        )
                        DiagItem(
                            label = "Stabilisasi Optik (OIS)",
                            value = if (details.isOisSupported) "SUPPORTED (Moving Lens Actuator)" else "N/A (EIS Only)",
                            highlightColor = if (details.isOisSupported) Color(0xFF10B981) else Color(0xFF94A3B8)
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = Color(0xFF334155)
                        )
                        DiagItem(
                            label = "Continuous Autofocus (AF)",
                            value = if (details.isContinuousAfSupported) "SUPPORTED (Active)" else "Fallback (Auto)",
                            highlightColor = if (details.isContinuousAfSupported) Color(0xFF10B981) else Color(0xFFFBBF24)
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = Color(0xFF334155)
                        )
                        DiagItem(
                            label = "Lokasi Folder Simpan",
                            value = "DCIM/Camera (Standar Galeri)",
                            highlightColor = Color(0xFF38BDF8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Rentang Target FPS yang Didukung Sensor Ini:",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                val fpsText = if (details.availableFpsRanges.isNotEmpty()) {
                    details.availableFpsRanges.joinToString(", ") { "[${it.lower}, ${it.upper}]" }
                } else {
                    "Default HAL ranges"
                }
                Text(
                    text = fpsText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (details.highSpeedFpsRanges.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "High Speed Video Ranges (Slow-Motion/High FPS):",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = details.highSpeedFpsRanges.joinToString(", ") { "[${it.lower}, ${it.upper}]" },
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
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
fun DiagItem(label: String, value: String, highlightColor: Color) {
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

@Composable
fun OpenCameraSettingsDialog(
    isCamera2Enabled: Boolean,
    onToggleCamera2: () -> Unit,
    selectedBitrate: BitrateMode,
    onSelectBitrate: (BitrateMode) -> Unit,
    selectedFps: FpsMode,
    onSelectFps: (FpsMode) -> Unit,
    selectedResolution: ResolutionMode,
    onSelectResolution: (ResolutionMode) -> Unit,
    isFpsLockEnabled: Boolean,
    onToggleFpsLock: () -> Unit,
    isStabilizationEnabled: Boolean,
    onToggleStabilization: () -> Unit,
    isAudioEnabled: Boolean,
    onToggleAudio: () -> Unit,
    hardwareDetails: CameraHardwareDetails,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = Color(0xFF090D16)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7).copy(alpha = 0.2f))
                                .border(1.5.dp, Color(0xFF38BDF8), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pengaturan Open Camera",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Bitrate 100 Mbps, 60/120 FPS & Camera2 API",
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card 1: Penjelasan Analisis Foto Pengguna (Foto 1, 2, 3)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.border(1.dp, Color(0xFF0284C7).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Analisis Perbandingan Foto Anda",
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Rahasia 60 FPS di Ruangan Gelap (Mode Open Camera):\nDi ruangan redup/gelap, kamera biasa otomatis memperpanjang exposure time sehingga gambar dipaksa terang tapi FPS anjlok ke 17-22 FPS. Open Camera mengunci waktu rana maks 1/60s (fixed [60, 60]), sehingga layar tetap gelap alami dan FPS KOKOH 60 FPS tanpa drop! Aplikasi kita sekarang mengunci fixed [60, 60] dan SENSOR_FRAME_DURATION agar 60 FPS selalu stabil.\n\n2. Bitrate 100 Mbps (Sesuai 113 MB / 10s):\nOpen Camera menghasilkan ~113 MB karena bitrate tinggi ~100 Mbps. Aplikasi kita kini default menggunakan Bitrate 100 Mbps.\n\n3. 119 FPS di 720p HD:\nSensor Samsung Anda mendukung 120 FPS High Speed pada 720p HD (Open Camera mendeteksi 119 fps). Pilih 720p HD + 120 FPS untuk 119 fps.",
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card 2: Camera API (Sesuai Foto ke-3 Open Camera)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Camera API: Camera2 API",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF0284C7))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("WAJIB", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Select Camera2 API to enable extra features such as manual modes for exposure, focus, along with 60/120 FPS and high bitrate (Sesuai foto ke-3 Open Camera).",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Switch(
                            checked = isCamera2Enabled,
                            onCheckedChange = { onToggleCamera2() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0284C7)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 3: Video Bitrate (Penyebab utama perbedaan kualitas!)
                Text(
                    text = "Bitrate Video (Tingkat Kualitas Gambar):",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pilih 100 Mbps untuk menghasilkan kualitas ultra tanpa pecah (~113 MB / 10s persis Open Camera).",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                BitrateMode.values().forEach { mode ->
                    val isSelected = (mode == selectedBitrate)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF0369A1).copy(alpha = 0.35f) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectBitrate(mode) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = mode.displayName,
                                        color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    if (mode == BitrateMode.BITRATE_100) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF10B981))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text("OPEN CAMERA", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(
                                    text = "${mode.description} • Perkiraan: ${mode.approxPerTenSec} per 10 detik",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 4: Target FPS
                Text(
                    text = "Video Frame Rate (FPS):",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pilih 120 FPS untuk High Speed (119 fps seperti foto Open Camera) atau 60 FPS untuk super smooth.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                FpsMode.values().forEach { mode ->
                    val isSelected = (mode == selectedFps)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF065F46).copy(alpha = 0.35f) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF34D399) else Color(0xFF334155),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectFps(mode) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.displayName,
                                    color = if (isSelected) Color(0xFF34D399) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = mode.description,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card 5: Kunci FPS Shutter (Anti-Drop ke 22 FPS)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kunci FPS Anti-Drop (Mode Open Camera)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Mengunci fixed [60, 60] FPS dan waktu rana maks 1/60s. Dalam kondisi gelap, kamera tetap gelap alami (tidak memaksakan slow-shutter terang) sehingga perekaman kuat 60 FPS stabil tanpa drop ke 17 FPS.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                        Switch(
                            checked = isFpsLockEnabled,
                            onCheckedChange = { onToggleFpsLock() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 6: Resolusi Video
                Text(
                    text = "Resolusi Video:",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                ResolutionMode.values().forEach { mode ->
                    val isSelected = (mode == selectedResolution)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF1E1B4B).copy(alpha = 0.5f) else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) Color(0xFF818CF8) else Color(0xFF334155),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectResolution(mode) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${mode.displayName} (${mode.description})",
                                    color = if (isSelected) Color(0xFF818CF8) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = mode.bestFor,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card 7: Stabilisasi Video EIS/OIS
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Stabilisasi Video (EIS + OIS)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Meredam guncangan kamera secara real-time via hardware giroskop Samsung.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isStabilizationEnabled,
                            onCheckedChange = { onToggleStabilization() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card 8: Rekam Audio
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rekam Audio (Stereo AAC)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Merekam suara jernih stereo bersama video.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isAudioEnabled,
                            onCheckedChange = { onToggleAudio() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0284C7)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Terapkan Pengaturan & Tutup",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
