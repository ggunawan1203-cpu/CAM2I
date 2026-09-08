package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraMetadata
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Range
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MotionPhotosAuto
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Portrait
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.camera.CinematicAperture
import com.example.camera.CinematicBokehState
import com.example.camera.CinematicStyle
import com.example.camera.FpsMode
import com.example.camera.ProVideoManualSettings
import com.example.camera.ResolutionMode
import com.example.camera.SamsungCameraHelper
import kotlin.math.roundToInt
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

    // New Camera Features States (Grid, Face Detect, AE Lock, Zoom & Exposure)
    var isGridEnabled by remember { mutableStateOf(false) }
    var isFaceDetectionEnabled by remember { mutableStateOf(true) }
    var detectedFaceCount by remember { mutableIntStateOf(0) }
    var isAeLocked by remember { mutableStateOf(false) }
    var currentExposureIndex by remember { mutableIntStateOf(0) }
    var exposureRange by remember { mutableStateOf(Range(-4, 4)) }
    var exposureStep by remember { mutableStateOf<Rational?>(Rational(1, 3)) }
    var isExposureSliderVisible by remember { mutableStateOf(false) }
    var currentZoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(8f) }

    var isTorchEnabled by remember { mutableStateOf(false) }
    var isAudioEnabled by remember { mutableStateOf(hasAudioPermission) }

    // Mode Sinematik Video ML Kit Bokeh State
    var cinematicBokehState by remember { mutableStateOf(CinematicBokehState()) }
    var cinematicImageAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }

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

    // Pro Video manual controls state
    var proVideoSettings by remember { mutableStateOf(ProVideoManualSettings()) }
    var selectedProTab by remember { mutableStateOf<String?>("ISO") }

    // Auto-dismiss exposure slider after inactivity
    LaunchedEffect(isExposureSliderVisible, currentExposureIndex) {
        if (isExposureSliderVisible) {
            delay(4000)
            isExposureSliderVisible = false
        }
    }

    // Apply Pro Video manual settings when changed
    LaunchedEffect(proVideoSettings, captureMode) {
        if (captureMode == CameraCaptureMode.PRO_VIDEO && camera != null) {
            SamsungCameraHelper.applyProVideoSettings(camera, proVideoSettings)
        }
    }

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

            val isPhoto = !captureMode.isVideo

            // 1. Build Preview with Camera2 API, Face Detection, AE Lock, Scene modes, and EIS/OIS stabilization
            val preview = SamsungCameraHelper.buildPreview(
                targetFpsRange = optimalRange,
                isPhotoMode = isPhoto,
                captureMode = captureMode,
                enableCamera2Api = isCamera2ApiEnabled,
                enableStabilization = isVideoStabilizationEnabled,
                lockFpsAntiDrop = isFpsLockEnabled,
                enableFaceDetection = isFaceDetectionEnabled,
                isAeLocked = isAeLocked,
                onFacesDetected = { faces ->
                    detectedFaceCount = faces.size
                }
            )
            preview.surfaceProvider = pView.surfaceProvider

            val boundCamera: Camera
            if (isPhoto) {
                // Photo modes (FOTO, POTRET, MALAM): Bind Preview + ImageCapture
                val imgCapture = SamsungCameraHelper.buildImageCapture(captureMode = captureMode)
                imageCapture = imgCapture
                videoCapture = null

                boundCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imgCapture
                )
            } else {
                // Video mode (VIDEO, CINEMATIC_VIDEO, PRO_VIDEO): Bind Preview + VideoCapture with Open Camera explicit 60 FPS, Bitrate & EIS/OIS
                val vCapture = SamsungCameraHelper.buildVideoCapture(
                    resolutionMode = selectedResolution,
                    targetFpsRange = optimalRange,
                    bitrateMode = selectedBitrate,
                    enableCamera2Api = isCamera2ApiEnabled,
                    enableStabilization = isVideoStabilizationEnabled,
                    lockFpsAntiDrop = isFpsLockEnabled,
                    enableFaceDetection = isFaceDetectionEnabled,
                    isAeLocked = isAeLocked
                )
                videoCapture = vCapture
                imageCapture = null

                if (captureMode == CameraCaptureMode.CINEMATIC_VIDEO) {
                    val analysis = SamsungCameraHelper.buildCinematicImageAnalysis(context) { isDetected, conf, bounds ->
                        cinematicBokehState = cinematicBokehState.copy(
                            isSubjectDetected = isDetected,
                            subjectConfidence = conf,
                            subjectBounds = bounds
                        )
                    }
                    cinematicImageAnalysis = analysis
                    boundCamera = try {
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            vCapture,
                            analysis
                        )
                    } catch (e: Exception) {
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            vCapture
                        )
                    }
                } else {
                    cinematicImageAnalysis = null
                    boundCamera = provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        vCapture
                    )
                }
            }

            camera = boundCamera

            // Observe hardware Zoom state
            boundCamera.cameraInfo.zoomState.observe(lifecycleOwner) { zState ->
                if (zState != null) {
                    currentZoomRatio = zState.zoomRatio
                    minZoomRatio = zState.minZoomRatio
                    maxZoomRatio = zState.maxZoomRatio
                }
            }

            // Inspect exposure compensation state
            val expState = boundCamera.cameraInfo.exposureState
            if (expState.isExposureCompensationSupported) {
                exposureRange = expState.exposureCompensationRange
                exposureStep = expState.exposureCompensationStep
                currentExposureIndex = expState.exposureCompensationIndex
            }

            // 2. Apply Camera2 Hardware Controls (Active AF, FPS range, Anti-Drop, Face Detect, AE Lock & Stabilization)
            SamsungCameraHelper.applyActiveHardwareSettings(
                context = context,
                camera = boundCamera,
                targetFpsRange = optimalRange,
                isPhotoMode = isPhoto,
                captureMode = captureMode,
                enableCamera2Api = isCamera2ApiEnabled,
                enableStabilization = isVideoStabilizationEnabled,
                lockFpsAntiDrop = isFpsLockEnabled,
                enableFaceDetection = isFaceDetectionEnabled,
                isAeLocked = isAeLocked
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
        cameraLensFacing,
        isFaceDetectionEnabled
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
        // Camera Preview Viewfinder with Tap-to-Focus, Long-Press AE Lock, and Pinch-to-Zoom
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
                    detectTapGestures(
                        onTap = { offset ->
                            val cam = camera ?: return@detectTapGestures
                            val pView = previewView ?: return@detectTapGestures
                            tapPoint = offset
                            isFocusing = true
                            isExposureSliderVisible = true
                            SamsungCameraHelper.performTapToFocus(pView, cam.cameraControl, offset.x, offset.y) {
                                isFocusing = false
                            }
                        },
                        onLongPress = { offset ->
                            val cam = camera ?: return@detectTapGestures
                            tapPoint = offset
                            isAeLocked = !isAeLocked
                            SamsungCameraHelper.setAeLock(cam, isAeLocked)
                            val msg = if (isAeLocked) "Penguncian Cahaya (AE Lock) AKTIF" else "Penguncian Cahaya DILEPAS"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                .pointerInput(camera) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val cam = camera ?: return@detectTransformGestures
                        val newRatio = (currentZoomRatio * zoom).coerceIn(minZoomRatio, maxZoomRatio)
                        cam.cameraControl.setZoomRatio(newRatio)
                    }
                }
        )

        // Grid 3x3 Overlay (Rule of Thirds)
        if (isGridEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val gridColor = Color.White.copy(alpha = 0.35f)
                val strokeW = 1.dp.toPx()
                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeW)
                drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeW)
                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeW)
                drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeW)
            }
        }

        // Cinematic Video Overlay (ML Kit AI Bokeh Tracking, CinemaScope 2.39:1 & Lens grading)
        if (captureMode == CameraCaptureMode.CINEMATIC_VIDEO) {
            CinematicBokehOverlay(
                state = cinematicBokehState,
                isRecording = isRecording
            )
        }

        // Shutter White Flash Effect
        if (showPhotoFlash) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.85f))
            )
        }

        // Tap-to-Focus Reticle + Exposure Slider (Brightness Naik-Turun & AE Lock)
        tapPoint?.let { point ->
            if (isExposureSliderVisible) {
                ExposureFocusWidget(
                    tapPoint = point,
                    isFocusing = isFocusing,
                    isAeLocked = isAeLocked,
                    exposureIndex = currentExposureIndex,
                    exposureRange = exposureRange,
                    exposureStep = exposureStep,
                    onExposureChanged = { newIdx ->
                        currentExposureIndex = newIdx
                        camera?.let { cam ->
                            SamsungCameraHelper.setExposureCompensation(cam, newIdx)
                        }
                    },
                    onToggleAeLock = {
                        isAeLocked = !isAeLocked
                        camera?.let { cam ->
                            SamsungCameraHelper.setAeLock(cam, isAeLocked)
                        }
                        val msg = if (isAeLocked) "Penguncian Cahaya (AE Lock) AKTIF" else "Penguncian Cahaya DILEPAS"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
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

        // Top Control Bar with Format pill, Quick Action Icons, Secondary Strip, and Badges (Integrated cleanly without overlaps)
        TopHeaderBar(
            captureMode = captureMode,
            isCamera2ApiEnabled = isCamera2ApiEnabled,
            isVideoStabilizationEnabled = isVideoStabilizationEnabled,
            isGridEnabled = isGridEnabled,
            isFaceDetectionEnabled = isFaceDetectionEnabled,
            detectedFaceCount = detectedFaceCount,
            isAeLocked = isAeLocked,
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
            onToggleGrid = {
                isGridEnabled = !isGridEnabled
                val msg = if (isGridEnabled) "Garis Kisi 3x3 AKTIF" else "Garis Kisi NONAKTIF"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
            onToggleFaceDetection = {
                isFaceDetectionEnabled = !isFaceDetectionEnabled
                val msg = if (isFaceDetectionEnabled) "Deteksi Wajah Otomatis AKTIF" else "Deteksi Wajah NONAKTIF"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
            onToggleAeLock = {
                isAeLocked = !isAeLocked
                camera?.let { cam ->
                    SamsungCameraHelper.setAeLock(cam, isAeLocked)
                }
                val msg = if (isAeLocked) "Penguncian Cahaya (AE Lock) AKTIF" else "Penguncian Cahaya DILEPAS"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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

        // Bottom Bar with Wide/Zoom Selector, Gallery Thumbnail, Mode Switcher, Pro Video Controls, and Shutter Controls
        val latestMediaItem = recentMediaList.firstOrNull()

        BottomSectionControls(
            modifier = Modifier.align(Alignment.BottomCenter),
            captureMode = captureMode,
            latestMedia = latestMediaItem,
            currentZoomRatio = currentZoomRatio,
            minZoomRatio = minZoomRatio,
            maxZoomRatio = maxZoomRatio,
            onSetZoomRatio = { targetZoom ->
                camera?.cameraControl?.setZoomRatio(targetZoom)
            },
            onOpenGallery = {
                refreshRecentMedia()
                showGalleryViewer = true
            },
            onModeSelected = { newMode ->
                if (!isRecording) {
                    captureMode = newMode
                }
            },
            proVideoSettings = proVideoSettings,
            selectedProTab = selectedProTab,
            onSelectProTab = { selectedProTab = it },
            onProVideoSettingsChanged = { proVideoSettings = it },
            cinematicBokehState = cinematicBokehState,
            onCinematicBokehStateChanged = { cinematicBokehState = it },
            onShowToast = { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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
                    val modePrefix = if (captureMode == CameraCaptureMode.CINEMATIC_VIDEO) "CINEMATIC_" else ""
                    val rec = SamsungCameraHelper.prepareRecording(
                        context = context,
                        videoCapture = vCap,
                        fpsLabel = "${modePrefix}${selectedResolution.displayName}_${fpsTag}FPS_${bitrateTag}",
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
                                    val msg = if (captureMode == CameraCaptureMode.CINEMATIC_VIDEO) {
                                        "Video Sinematik (Bokeh AI, $fpsTag FPS, $bitrateTag) tersimpan di DCIM/Camera"
                                    } else {
                                        "Video ($fpsTag FPS, $bitrateTag) tersimpan di DCIM/Camera"
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
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
                    val startMsg = if (captureMode == CameraCaptureMode.CINEMATIC_VIDEO) {
                        "Merekam Sinematik Bokeh ${cinematicBokehState.aperture.label} • $fpsTag FPS • $bitrateTag"
                    } else {
                        "Merekam: $fpsTag FPS • $bitrateTag"
                    }
                    Toast.makeText(context, startMsg, Toast.LENGTH_SHORT).show()
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
                isGridEnabled = isGridEnabled,
                onToggleGrid = { isGridEnabled = !isGridEnabled },
                isFaceDetectionEnabled = isFaceDetectionEnabled,
                onToggleFaceDetection = { isFaceDetectionEnabled = !isFaceDetectionEnabled },
                isAeLocked = isAeLocked,
                onToggleAeLock = {
                    isAeLocked = !isAeLocked
                    camera?.let { cam ->
                        SamsungCameraHelper.setAeLock(cam, isAeLocked)
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

/**
 * Interactive Tap-to-Focus Reticle + Vertical Exposure Slider (Geser Naik-Turun untuk Mengatur Cahaya)
 * and Light Lock (Penguncian Cahaya / AE Lock) button.
 */
@Composable
fun ExposureFocusWidget(
    tapPoint: Offset,
    isFocusing: Boolean,
    isAeLocked: Boolean,
    exposureIndex: Int,
    exposureRange: Range<Int>,
    exposureStep: Rational?,
    onExposureChanged: (Int) -> Unit,
    onToggleAeLock: () -> Unit
) {
    val density = LocalDensity.current
    val minExp = exposureRange.lower
    val maxExp = exposureRange.upper
    val span = (maxExp - minExp).coerceAtLeast(1)

    val animatedScale by animateFloatAsState(
        targetValue = if (isFocusing) 1.22f else 1.0f,
        animationSpec = tween(durationMillis = 180),
        label = "focus_scale"
    )

    // Position slider to the left if tap is close to right screen edge to avoid clipping
    val sliderToLeft = tapPoint.x > 250f * density.density

    Box(
        modifier = Modifier
            .offset {
                with(density) {
                    IntOffset(
                        (tapPoint.x - 36.dp.toPx()).roundToInt(),
                        (tapPoint.y - 36.dp.toPx()).roundToInt()
                    )
                }
            }
    ) {
        // Focus Reticle Ring
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(animatedScale)
                .border(
                    2.dp,
                    if (isAeLocked) Color(0xFFF59E0B) else Color(0xFFFBBF24),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isAeLocked) Color(0xFFF59E0B) else Color(0xFFFBBF24))
            )
        }

        // Vertical Exposure Slider (Geser Naik-Turun untuk Mengatur Cahaya)
        val sliderOffsetXDp = if (sliderToLeft) (-68).dp else 76.dp
        Column(
            modifier = Modifier
                .offset(x = sliderOffsetXDp, y = (-42).dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.94f))
                .border(1.dp, if (isAeLocked) Color(0xFFF59E0B) else Color(0xFF334155), RoundedCornerShape(16.dp))
                .padding(horizontal = 5.dp, vertical = 6.dp)
                .pointerInput(exposureIndex, minExp, maxExp) {
                    detectVerticalDragGestures { _, dragAmount ->
                        val stepPx = 14f
                        val delta = (-dragAmount / stepPx).roundToInt()
                        if (delta != 0) {
                            val newIdx = (exposureIndex + delta).coerceIn(minExp, maxExp)
                            if (newIdx != exposureIndex) {
                                onExposureChanged(newIdx)
                            }
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lock / Unlock button for AE Lock
            IconButton(
                onClick = onToggleAeLock,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isAeLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Penguncian Cahaya",
                    tint = if (isAeLocked) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Quick increment '+' button
            IconButton(
                onClick = {
                    val next = (exposureIndex + 1).coerceIn(minExp, maxExp)
                    onExposureChanged(next)
                },
                modifier = Modifier.size(22.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Terang", tint = Color.White, modifier = Modifier.size(16.dp))
            }

            // Brightness Sun Icon
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = "Brightness Slider",
                tint = Color(0xFFFBBF24),
                modifier = Modifier.size(15.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Vertical Track Indicating Current EV
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF334155)),
                contentAlignment = Alignment.BottomCenter
            ) {
                val fraction = ((exposureIndex - minExp).toFloat() / span).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Quick decrement '-' button
            IconButton(
                onClick = {
                    val prev = (exposureIndex - 1).coerceIn(minExp, maxExp)
                    onExposureChanged(prev)
                },
                modifier = Modifier.size(22.dp)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Gelap", tint = Color.White, modifier = Modifier.size(16.dp))
            }

            // EV Index Formatted String
            val evText = SamsungCameraHelper.formatEvString(exposureIndex, exposureStep)
            Text(
                text = evText,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun TopHeaderBar(
    captureMode: CameraCaptureMode,
    isCamera2ApiEnabled: Boolean,
    isVideoStabilizationEnabled: Boolean,
    isGridEnabled: Boolean,
    isFaceDetectionEnabled: Boolean,
    detectedFaceCount: Int,
    isAeLocked: Boolean,
    hardwareLevel: String,
    selectedResolution: ResolutionMode,
    selectedFps: FpsMode,
    selectedBitrate: BitrateMode,
    activeFpsRange: Range<Int>?,
    isRecording: Boolean,
    isTorchOn: Boolean,
    onToggleCamera2Api: () -> Unit,
    onToggleStabilization: () -> Unit,
    onToggleGrid: () -> Unit,
    onToggleFaceDetection: () -> Unit,
    onToggleAeLock: () -> Unit,
    onSelectResolution: (ResolutionMode) -> Unit,
    onSelectFps: (FpsMode) -> Unit,
    onSelectBitrate: (BitrateMode) -> Unit,
    onToggleTorch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenInfo: () -> Unit
) {
    var showResolutionMenu by remember { mutableStateOf(false) }
    var showBitrateMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.92f), Color.Black.copy(alpha = 0.45f), Color.Transparent)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Row 1: Primary Bar with Format/Quality Dropdown (Left) & Quick Action Icons (Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quality & FPS Dropdown pill for Video / Pro Video, or Hi-Res Photo indicator
            if (captureMode.isVideo) {
                Box {
                    val resolutionLabel = when (selectedResolution) {
                        ResolutionMode.RES_4K -> "4K"
                        ResolutionMode.RES_1080P -> "FHD"
                        ResolutionMode.RES_720P -> "HD"
                        ResolutionMode.RES_480P -> "SD"
                    }
                    val fpsLabel = when (selectedFps) {
                        FpsMode.FPS_120 -> activeFpsRange?.let { "${it.upper}" } ?: "120"
                        FpsMode.FPS_60 -> activeFpsRange?.let { "${it.upper}" } ?: "60"
                        FpsMode.FPS_30 -> "30"
                        FpsMode.FPS_AUTO -> "AUTO"
                    }
                    val modeColor = when (captureMode) {
                        CameraCaptureMode.CINEMATIC_VIDEO -> Color(0xFFF59E0B)
                        CameraCaptureMode.PRO_VIDEO -> Color(0xFFE11D48)
                        else -> Color(0xFF38BDF8)
                    }
                    val pillPrefix = when (captureMode) {
                        CameraCaptureMode.CINEMATIC_VIDEO -> "CINEMA • "
                        CameraCaptureMode.PRO_VIDEO -> "PRO • "
                        else -> ""
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                            .border(1.dp, modeColor, RoundedCornerShape(16.dp))
                            .clickable(enabled = !isRecording) { showResolutionMenu = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "$pillPrefix$resolutionLabel • ${fpsLabel}FPS",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = modeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showResolutionMenu,
                        onDismissRequest = { showResolutionMenu = false }
                    ) {
                        Text(
                            text = "  RESOLUSI VIDEO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        ResolutionMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            mode.displayName,
                                            fontWeight = if (mode == selectedResolution) FontWeight.Bold else FontWeight.Normal,
                                            color = if (mode == selectedResolution) Color(0xFF0284C7) else Color.Unspecified
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(mode.description, fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    onSelectResolution(mode)
                                    showResolutionMenu = false
                                }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "  FRAME RATE (FPS)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                        FpsMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        mode.displayName,
                                        fontWeight = if (mode == selectedFps) FontWeight.Bold else FontWeight.Normal,
                                        color = if (mode == selectedFps) Color(0xFF10B981) else Color.Unspecified
                                    )
                                },
                                onClick = {
                                    onSelectFps(mode)
                                    showResolutionMenu = false
                                }
                            )
                        }
                    }
                }
            } else {
                // Photo Mode Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.9f))
                        .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(16.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "HI-RES PHOTO",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right Quick Action Icons (Grid, Face, AE Lock, Flash, Settings, Info)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Grid 3x3 toggle button
                IconButton(
                    onClick = onToggleGrid,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isGridEnabled) Color(0xFF0284C7).copy(alpha = 0.85f)
                            else Color(0xFF1E293B).copy(alpha = 0.7f)
                        )
                ) {
                    Icon(
                        imageVector = if (isGridEnabled) Icons.Default.GridOn else Icons.Default.GridOff,
                        contentDescription = "Garis Kisi 3x3",
                        tint = if (isGridEnabled) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Face Detection toggle button
                IconButton(
                    onClick = onToggleFaceDetection,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFaceDetectionEnabled) Color(0xFF0284C7).copy(alpha = 0.85f)
                            else Color(0xFF1E293B).copy(alpha = 0.7f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = "Deteksi Wajah Otomatis",
                        tint = if (isFaceDetectionEnabled) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Penguncian Cahaya (AE Lock) button
                IconButton(
                    onClick = onToggleAeLock,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isAeLocked) Color(0xFFF59E0B).copy(alpha = 0.9f)
                            else Color(0xFF1E293B).copy(alpha = 0.7f)
                        )
                ) {
                    Icon(
                        imageVector = if (isAeLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Penguncian Cahaya",
                        tint = if (isAeLocked) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Flash Torch
                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = if (isTorchOn) Color(0xFFFBBF24) else Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Open Camera Settings Gear
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0284C7).copy(alpha = 0.85f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Pengaturan Open Camera",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Diagnostics Info
                IconButton(
                    onClick = onOpenInfo,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info Diagnostics",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        // Row 2: Secondary Hardware Status Quick Strip (Camera2, EIS, Bitrate)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Camera2 API Toggle Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isCamera2ApiEnabled) Color(0xFF0284C7).copy(alpha = 0.9f)
                        else Color(0xFF334155).copy(alpha = 0.85f)
                    )
                    .border(
                        1.dp,
                        if (isCamera2ApiEnabled) Color(0xFF38BDF8) else Color(0xFF64748B),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(enabled = !isRecording) { onToggleCamera2Api() }
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = if (isCamera2ApiEnabled) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                    contentDescription = "Toggle Camera2 API",
                    tint = if (isCamera2ApiEnabled) Color.White else Color(0xFF94A3B8),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isCamera2ApiEnabled) "Camera2: ON" else "Camera2: OFF",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Video Stabilization EIS/OIS Toggle Pill
            if (captureMode.isVideo) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isVideoStabilizationEnabled) Color(0xFF059669).copy(alpha = 0.9f)
                            else Color(0xFF334155).copy(alpha = 0.85f)
                        )
                        .border(
                            1.dp,
                            if (isVideoStabilizationEnabled) Color(0xFF34D399) else Color(0xFF64748B),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable(enabled = !isRecording) { onToggleStabilization() }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MotionPhotosAuto,
                        contentDescription = "Stabilizer EIS/OIS",
                        tint = if (isVideoStabilizationEnabled) Color.White else Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isVideoStabilizationEnabled) "EIS: ON" else "EIS: OFF",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bitrate Dropdown Button (Enabled for Video)
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E293B).copy(alpha = 0.9f))
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(14.dp))
                            .clickable(enabled = !isRecording) { showBitrateMenu = true }
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = bitrateLabel,
                            color = Color.White,
                            fontSize = 10.sp,
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
        }

        // Row 3: Active Status Badges (Centred directly beneath - prevents any overlaps!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (captureMode == CameraCaptureMode.PORTRAIT) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF78350F).copy(alpha = 0.9f))
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Portrait, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("MODE POTRET • BOKEH DEPTH", color = Color(0xFFF59E0B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (captureMode == CameraCaptureMode.NIGHT) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF4C1D95).copy(alpha = 0.9f))
                        .border(1.dp, Color(0xFF8B5CF6), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NightsStay, null, tint = Color(0xFFC4B5FD), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("MODE MALAM • LOW NOISE NR", color = Color(0xFFC4B5FD), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (captureMode == CameraCaptureMode.PRO_VIDEO) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF881337).copy(alpha = 0.9f))
                        .border(1.dp, Color(0xFFFB7185), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, null, tint = Color(0xFFFB7185), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("MODE PRO VIDEO • KONTROL MANUAL", color = Color(0xFFFB7185), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isAeLocked) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF78350F).copy(alpha = 0.9f))
                        .border(1.dp, Color(0xFFFBBF24), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = Color(0xFFFBBF24), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("CAHAYA TERKUNCI", color = Color(0xFFFBBF24), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (isFaceDetectionEnabled && detectedFaceCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0C4A6E).copy(alpha = 0.9f))
                        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Face, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("$detectedFaceCount Wajah Terdeteksi", color = Color(0xFF38BDF8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Interactive Pro Video Control Bar:
 * Allows user to control ISO, Shutter Speed, White Balance, Focus, and Audio direction.
 */
@Composable
fun ProVideoControlBar(
    proSettings: ProVideoManualSettings,
    activeTab: String?,
    onSelectTab: (String?) -> Unit,
    onSettingsChanged: (ProVideoManualSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Active Sub-Options Selector (Horizontal chip list when a tab is selected)
        AnimatedVisibility(
            visible = activeTab != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                    .border(1.dp, Color(0xFFE11D48).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (activeTab) {
                    "ISO" -> {
                        val isoOptions = listOf(0 to "AUTO", 50 to "50", 100 to "100", 200 to "200", 400 to "400", 800 to "800", 1600 to "1600", 3200 to "3200")
                        isoOptions.forEach { (valIso, label) ->
                            val isSel = (proSettings.iso == valIso)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) Color(0xFFE11D48) else Color(0xFF1E293B))
                                    .clickable { onSettingsChanged(proSettings.copy(iso = valIso)) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(label, color = if (isSel) Color.White else Color(0xFFCBD5E1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    "SPEED" -> {
                        val speedOptions = listOf(
                            0L to "AUTO",
                            1_000_000L to "1/1000",
                            2_000_000L to "1/500",
                            4_000_000L to "1/250",
                            8_000_000L to "1/125",
                            16_666_666L to "1/60",
                            33_333_333L to "1/30"
                        )
                        speedOptions.forEach { (speedNs, label) ->
                            val isSel = (proSettings.shutterSpeedNs == speedNs)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) Color(0xFFE11D48) else Color(0xFF1E293B))
                                    .clickable { onSettingsChanged(proSettings.copy(shutterSpeedNs = speedNs)) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(label, color = if (isSel) Color.White else Color(0xFFCBD5E1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    "WB" -> {
                        val wbOptions = listOf(
                            CameraMetadata.CONTROL_AWB_MODE_AUTO to "AUTO",
                            CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT to "💡 2800K",
                            CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT to "🔦 4000K",
                            CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT to "☀️ 5500K",
                            CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT to "☁️ 6500K"
                        )
                        wbOptions.forEach { (wbVal, label) ->
                            val isSel = (proSettings.awbMode == wbVal)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) Color(0xFFE11D48) else Color(0xFF1E293B))
                                    .clickable { onSettingsChanged(proSettings.copy(awbMode = wbVal)) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(label, color = if (isSel) Color.White else Color(0xFFCBD5E1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    "FOCUS" -> {
                        val focusOptions = listOf(
                            -1f to "AF Otomatis",
                            10.0f to "Makro (10cm)",
                            2.0f to "Dekat (50cm)",
                            1.0f to "Potret (1m)",
                            0.0f to "Jauh (∞)"
                        )
                        focusOptions.forEach { (dist, label) ->
                            val isSel = (proSettings.focusDistance == dist)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) Color(0xFFE11D48) else Color(0xFF1E293B))
                                    .clickable { onSettingsChanged(proSettings.copy(focusDistance = dist)) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(label, color = if (isSel) Color.White else Color(0xFFCBD5E1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    "MIC" -> {
                        val micOptions = listOf(
                            "OMNI" to "🎙️ OMNI (Semua Arah)",
                            "FRONT" to "🎤 DEPAN (Subjek)",
                            "REAR" to "🎧 BELAKANG (Kamera)"
                        )
                        micOptions.forEach { (src, label) ->
                            val isSel = (proSettings.audioSource == src)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) Color(0xFFE11D48) else Color(0xFF1E293B))
                                    .clickable { onSettingsChanged(proSettings.copy(audioSource = src)) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(label, color = if (isSel) Color.White else Color(0xFFCBD5E1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Main Tab Buttons (ISO, SPEED, WB, FOCUS, MIC)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.90f))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(18.dp))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                "ISO" to (if (proSettings.iso == 0) "AUTO" else "${proSettings.iso}"),
                "SPEED" to (when (proSettings.shutterSpeedNs) {
                    0L -> "AUTO"
                    1_000_000L -> "1/1000"
                    2_000_000L -> "1/500"
                    4_000_000L -> "1/250"
                    8_000_000L -> "1/125"
                    16_666_666L -> "1/60"
                    33_333_333L -> "1/30"
                    else -> "MANUAL"
                }),
                "WB" to (when (proSettings.awbMode) {
                    CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT -> "2800K"
                    CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT -> "4000K"
                    CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT -> "5500K"
                    CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT -> "6500K"
                    else -> "AUTO"
                }),
                "FOCUS" to (if (proSettings.focusDistance < 0f) "AF-C" else "MF"),
                "MIC" to proSettings.audioSource
            )

            tabs.forEach { (tabId, valueLabel) ->
                val isSelected = (activeTab == tabId)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) Color(0xFFE11D48) else Color(0xFF1E293B).copy(alpha = 0.6f))
                        .clickable { onSelectTab(if (isSelected) null else tabId) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tabId,
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = valueLabel,
                            color = if (isSelected) Color.White else Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cinematic Bokeh Overlay for Viewfinder
 * Renders CinemaScope 2.39:1 letterbox matte bars, subtle optical vignette/flare,
 * and dynamic ML Kit face/subject tracking reticle with bokeh locked badge.
 */
@Composable
fun CinematicBokehOverlay(
    state: CinematicBokehState,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 1. CinemaScope 2.39:1 Letterbox Bars
        if (state.isWidescreen239Enabled) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Matte Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.12f)
                        .background(Color.Black),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = Modifier.padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "CINEMASCOPE 2.39:1",
                            color = Color(0xFFF59E0B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "•",
                            color = Color.DarkGray,
                            fontSize = 9.sp
                        )
                        Text(
                            text = "${state.aperture.label} • ${state.style.displayName}",
                            color = Color.LightGray,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Bottom Matte Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.14f)
                        .background(Color.Black),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = if (state.isSubjectDetected) "● ML KIT BOKEH AI ACTIVE" else "○ AI TRACKING ACTIVE",
                        color = if (state.isSubjectDetected) Color(0xFF10B981) else Color.Gray,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp),
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // 2. Optical vignette and Lens Flare tone
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val vignetteBrush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                center = Offset(w / 2f, h / 2f),
                radius = w.coerceAtLeast(h) * 0.7f
            )
            drawRect(brush = vignetteBrush)

            if (state.style == CinematicStyle.ANAMORPHIC) {
                val flareY = h * 0.45f
                drawLine(
                    color = Color(0x3338BDF8),
                    start = Offset(0f, flareY),
                    end = Offset(w, flareY),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color(0x1A0284C7),
                    start = Offset(0f, flareY - 4f),
                    end = Offset(w, flareY - 4f),
                    strokeWidth = 6.dp.toPx()
                )
            } else if (state.style == CinematicStyle.WARM_GOLD) {
                drawRect(color = Color(0x14F59E0B))
            } else if (state.style == CinematicStyle.SPOTLIGHT && state.isSubjectDetected) {
                drawRect(color = Color(0x28000000))
            }
        }

        // 3. Dynamic ML Kit Subject Tracking Reticle
        val infiniteTransition = rememberInfiniteTransition(label = "bokeh_pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.65f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(700),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )

        if (state.isSubjectDetected) {
            val bounds = state.subjectBounds
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenW = maxWidth
                val screenH = maxHeight

                val boxLeft = if (bounds != null) (bounds.left * screenW.value).dp else screenW * 0.25f
                val boxTop = if (bounds != null) (bounds.top * screenH.value).dp else screenH * 0.28f
                val boxW = if (bounds != null) ((bounds.width() * screenW.value).coerceIn(120f, 320f)).dp else screenW * 0.5f
                val boxH = if (bounds != null) ((bounds.height() * screenH.value).coerceIn(140f, 400f)).dp else screenH * 0.42f

                Box(
                    modifier = Modifier
                        .offset(x = boxLeft, y = boxTop)
                        .size(width = boxW, height = boxH)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val bw = size.width
                        val bh = size.height
                        val cornerLen = 22.dp.toPx()
                        val stroke = 2.dp.toPx()
                        val color = Color(0xFFF59E0B).copy(alpha = pulseAlpha)

                        // Top-left
                        drawLine(color, Offset(0f, 0f), Offset(cornerLen, 0f), stroke)
                        drawLine(color, Offset(0f, 0f), Offset(0f, cornerLen), stroke)
                        // Top-right
                        drawLine(color, Offset(bw, 0f), Offset(bw - cornerLen, 0f), stroke)
                        drawLine(color, Offset(bw, 0f), Offset(bw, cornerLen), stroke)
                        // Bottom-left
                        drawLine(color, Offset(0f, bh), Offset(cornerLen, bh), stroke)
                        drawLine(color, Offset(0f, bh), Offset(0f, bh - cornerLen), stroke)
                        // Bottom-right
                        drawLine(color, Offset(bw, bh), Offset(bw - cornerLen, bh), stroke)
                        drawLine(color, Offset(bw, bh), Offset(bw, bh - cornerLen), stroke)
                    }

                    // Floating ML Kit Badge
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-24).dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A).copy(alpha = 0.9f))
                            .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "AI BOKEH LOCKED",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "•",
                            color = Color.Gray,
                            fontSize = 9.sp
                        )
                        Text(
                            text = state.aperture.label,
                            color = Color(0xFFF59E0B),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cinematic Bokeh Controls Bar
 * Provides instant aperture pills (f/1.4 - f/8.0), cinema lens styles, and 2.39:1 widescreen toggle.
 */
@Composable
fun CinematicBokehControlsWidget(
    state: CinematicBokehState,
    onStateChanged: (CinematicBokehState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Aperture Controller (f/1.4, f/2.0, f/2.8, f/4.0, f/8.0)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.92f))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CinematicAperture.values().forEach { ap ->
                val isSelected = (state.aperture == ap)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color(0xFFF59E0B) else Color.Transparent)
                        .clickable { onStateChanged(state.copy(aperture = ap)) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ap.label,
                        color = if (isSelected) Color.Black else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }
            }
        }

        // Row 2: Cinematic Style Selection + Widescreen 2.39:1 Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CinematicStyle.values().forEach { st ->
                val isSelected = (state.style == st)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) Color(0xFF0284C7).copy(alpha = 0.85f)
                            else Color(0xFF1E293B).copy(alpha = 0.7f)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onStateChanged(state.copy(style = st)) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = st.displayName,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            // 2.39:1 Aspect Ratio Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (state.isWidescreen239Enabled) Color(0xFFF59E0B).copy(alpha = 0.85f)
                        else Color(0xFF1E293B).copy(alpha = 0.7f)
                    )
                    .border(
                        1.dp,
                        if (state.isWidescreen239Enabled) Color(0xFFF59E0B) else Color(0xFF334155),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onStateChanged(state.copy(isWidescreen239Enabled = !state.isWidescreen239Enabled)) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Aspect Ratio 2.39:1",
                        tint = if (state.isWidescreen239Enabled) Color.Black else Color(0xFF94A3B8),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "2.39:1",
                        color = if (state.isWidescreen239Enabled) Color.Black else Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Row 3: ML Kit Subject Status Sub-strip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(top = 1.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (state.isSubjectDetected) Color(0xFF10B981) else Color(0xFFF59E0B))
            )
            Text(
                text = if (state.isSubjectDetected)
                    "ML Kit Bokeh Aktif • Subjek Terdeteksi (${state.aperture.description})"
                else
                    "ML Kit AI Aktif • Arahkan kamera ke wajah atau subjek",
                color = if (state.isSubjectDetected) Color(0xFF34D399) else Color(0xFFCBD5E1),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun BottomSectionControls(
    modifier: Modifier = Modifier,
    captureMode: CameraCaptureMode,
    latestMedia: CapturedMediaItem?,
    currentZoomRatio: Float,
    minZoomRatio: Float,
    maxZoomRatio: Float,
    onSetZoomRatio: (Float) -> Unit,
    onOpenGallery: () -> Unit,
    onModeSelected: (CameraCaptureMode) -> Unit,
    proVideoSettings: ProVideoManualSettings,
    selectedProTab: String?,
    onSelectProTab: (String?) -> Unit,
    onProVideoSettingsChanged: (ProVideoManualSettings) -> Unit,
    cinematicBokehState: CinematicBokehState = CinematicBokehState(),
    onCinematicBokehStateChanged: (CinematicBokehState) -> Unit = {},
    onShowToast: (String) -> Unit,
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
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.94f))
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
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

        // Pro Video Manual Controls Bar (Shown when in PRO_VIDEO mode)
        if (captureMode == CameraCaptureMode.PRO_VIDEO) {
            ProVideoControlBar(
                proSettings = proVideoSettings,
                activeTab = selectedProTab,
                onSelectTab = onSelectProTab,
                onSettingsChanged = onProVideoSettingsChanged
            )
        }

        // Cinematic Video Bokeh Controls Bar (Shown when in CINEMATIC_VIDEO mode)
        if (captureMode == CameraCaptureMode.CINEMATIC_VIDEO) {
            CinematicBokehControlsWidget(
                state = cinematicBokehState,
                onStateChanged = onCinematicBokehStateChanged
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Zoom & Wide Mode Selector: 0.5x WIDE | 1x | 2x
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isWideActive = currentZoomRatio <= 0.75f
            val is1xActive = currentZoomRatio in 0.8f..1.3f
            val is2xActive = currentZoomRatio >= 1.8f

            // 0.5x WIDE Button (Wide-angle support for video and photo)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isWideActive) Color(0xFF0284C7) else Color.Transparent)
                    .clickable {
                        val target = if (minZoomRatio < 0.9f) minZoomRatio else 0.5f
                        onSetZoomRatio(target.coerceIn(minZoomRatio, maxZoomRatio))
                        if (minZoomRatio >= 0.95f) {
                            onShowToast("Mode Wide: Menggunakan sudut pandang terlebar sensor")
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "0.5x WIDE",
                    color = if (isWideActive) Color.White else Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 1x Normal Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (is1xActive) Color(0xFF0284C7) else Color.Transparent)
                    .clickable {
                        onSetZoomRatio(1.0f.coerceIn(minZoomRatio, maxZoomRatio))
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "1x",
                    color = if (is1xActive) Color.White else Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2x Tele Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (is2xActive) Color(0xFF0284C7) else Color.Transparent)
                    .clickable {
                        onSetZoomRatio(2.0f.coerceIn(minZoomRatio, maxZoomRatio))
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "2x",
                    color = if (is2xActive) Color.White else Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mode Switcher Carousel: POTRET | FOTO | MALAM | VIDEO | PRO VIDEO
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.88f))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(24.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                CameraCaptureMode.values().forEach { mode ->
                    val isSelected = (captureMode == mode)
                    val accentColor = when (mode) {
                        CameraCaptureMode.PORTRAIT -> Color(0xFFF59E0B)
                        CameraCaptureMode.PHOTO -> Color(0xFF0284C7)
                        CameraCaptureMode.NIGHT -> Color(0xFF8B5CF6)
                        CameraCaptureMode.VIDEO -> Color(0xFFEF4444)
                        CameraCaptureMode.CINEMATIC_VIDEO -> Color(0xFFEC4899)
                        CameraCaptureMode.PRO_VIDEO -> Color(0xFFE11D48)
                    }
                    val icon = when (mode) {
                        CameraCaptureMode.PORTRAIT -> Icons.Default.Portrait
                        CameraCaptureMode.PHOTO -> Icons.Default.PhotoCamera
                        CameraCaptureMode.NIGHT -> Icons.Default.NightsStay
                        CameraCaptureMode.VIDEO -> Icons.Default.Videocam
                        CameraCaptureMode.CINEMATIC_VIDEO -> Icons.Default.Movie
                        CameraCaptureMode.PRO_VIDEO -> Icons.Default.Tune
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) accentColor else Color.Transparent)
                            .clickable(enabled = !isRecording) { onModeSelected(mode) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = mode.displayName,
                                tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = mode.displayName,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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

            // Audio mic toggle (for video) or Pause button when recording
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
            } else if (captureMode.isVideo) {
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

            // Central Shutter Button (Adapts according to Mode: Video, Pro Video, Cinematic Video, Potret, Foto, Malam)
            if (captureMode.isVideo) {
                // Video, Pro Video & Cinematic Video Shutter Button
                val proColor = when (captureMode) {
                    CameraCaptureMode.PRO_VIDEO -> Color(0xFFE11D48)
                    CameraCaptureMode.CINEMATIC_VIDEO -> Color(0xFFF59E0B)
                    else -> Color(0xFFDC2626)
                }
                val ringBorderColor = when (captureMode) {
                    CameraCaptureMode.PRO_VIDEO -> Color(0xFFFB7185)
                    CameraCaptureMode.CINEMATIC_VIDEO -> Color(0xFFFBBF24)
                    else -> Color.White
                }
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(4.dp, ringBorderColor, CircleShape)
                        .clickable { onVideoShutterClick() },
                    contentAlignment = Alignment.Center
                ) {
                    val recordColor by animateColorAsState(
                        targetValue = if (isRecording) Color(0xFFEF4444) else proColor,
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
            } else {
                // Photo / Portrait / Night Shutter Button
                val (shutterRingColor, shutterInnerColor) = when (captureMode) {
                    CameraCaptureMode.PORTRAIT -> Pair(Color(0xFFF59E0B), Color(0xFFFBBF24))
                    CameraCaptureMode.NIGHT -> Pair(Color(0xFF8B5CF6), Color(0xFFC4B5FD))
                    else -> Pair(Color.White, Color.White)
                }

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(4.dp, shutterRingColor, CircleShape)
                        .clickable { onPhotoShutterClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(shutterInnerColor)
                    )
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
    isGridEnabled: Boolean,
    onToggleGrid: () -> Unit,
    isFaceDetectionEnabled: Boolean,
    onToggleFaceDetection: () -> Unit,
    isAeLocked: Boolean,
    onToggleAeLock: () -> Unit,
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

                // Pengaturan Komposisi & Fokus: Garis Kisi (Grid), Deteksi Wajah, Penguncian Cahaya
                Text(
                    text = "Bidikan & Pencahayaan (Foto & Video):",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Card: Garis Kisi (Grid 3x3)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Garis Kisi Komposisi (Grid 3x3)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Menampilkan panduan 'rule of thirds' emas untuk mempermudah framing objek foto & video.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isGridEnabled,
                            onCheckedChange = { onToggleGrid() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF0284C7)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Card: Deteksi Wajah Otomatis
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Deteksi Wajah Otomatis (Face Detection)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Mengunci fokus dan pencahayaan otomatis ke wajah subjek di depan kamera.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isFaceDetectionEnabled,
                            onCheckedChange = { onToggleFaceDetection() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF10B981)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Card: Penguncian Cahaya (AE Lock)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Penguncian Cahaya (AE Lock)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Mengunci nilai eksposur/cahaya saat ini agar tidak berfluktuasi saat kamera bergerak.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isAeLocked,
                            onCheckedChange = { onToggleAeLock() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFF59E0B)
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
