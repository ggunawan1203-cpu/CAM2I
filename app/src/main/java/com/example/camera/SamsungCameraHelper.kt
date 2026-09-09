package com.example.camera

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.Face
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.util.Rational
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import android.graphics.RectF
import androidx.camera.core.ImageAnalysis
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "SamsungCameraHelper"

// Note: FpsMode, BitrateMode, ResolutionMode, CameraCaptureMode, CinematicAperture,
// CinematicStyle, CinematicBokehState, ProVideoManualSettings, CapturedMediaItem,
// and CameraHardwareDetails are defined in CameraModels.kt

object SamsungCameraHelper {

    val isSamsungDevice: Boolean
        get() = Build.MANUFACTURER.contains("samsung", ignoreCase = true) ||
                Build.BRAND.contains("samsung", ignoreCase = true)

    /**
     * Resolves the optimal FPS range strictly supported by the camera hardware.
     * Following Open Camera's strategy:
     * - Priority 1: High speed ranges [120, 120] or [60, 60] if requested
     * - Priority 2: Fixed [target, target] (guarantees fixed FPS without dropping to 22 FPS in low light)
     * - Priority 3: Dynamic with upper >= target (e.g. [30, 60] standard on Galaxy)
     * - Fallback: Sensor max upper FPS
     */
    /**
     * Resolves the target FPS range following Open Camera's strict fixed FPS algorithm.
     *
     * Why videos drop to 17-22 FPS in low light:
     * When targetFpsRange is variable (e.g. [15, 30] or [30, 60]), the Camera2 Auto Exposure (AE)
     * routine in dark scenes increases exposure time up to 1/(lower_bound) (e.g. 1/15s = 66ms),
     * which forces the sensor to drop down to 15-17 FPS to make the image artificially brighter!
     *
     * Open Camera's solution:
     * In Open Camera, when 60 FPS (or 30/120 FPS) is requested, it enforces a STRICT FIXED range:
     * - For 60 FPS: Range(60, 60)
     * - For 30 FPS: Range(30, 30)
     * - For 120 FPS: Range(120, 120)
     *
     * When lower == upper == 60:
     * The camera driver's max exposure time is hard-locked to 1/60s (16.66ms).
     * The camera driver is physically FORBIDDEN from slowing the frame rate to brighten the scene!
     * As observed in Open Camera: the scene stays dark naturally in low light, but 60 FPS
     * is maintained 100% solidly without frame drops!
     */
    fun resolveOptimalFpsRange(
        supportedRanges: List<Range<Int>>,
        highSpeedRanges: List<Range<Int>> = emptyList(),
        requestedFps: FpsMode
    ): Range<Int>? {
        if (requestedFps == FpsMode.FPS_AUTO) {
            return null
        }

        val target = requestedFps.targetFps
        val allRanges = (highSpeedRanges + supportedRanges).distinct()

        // 1. High Speed 120 FPS
        if (target >= 120) {
            val exactHs = allRanges.firstOrNull { it.lower == 120 && it.upper == 120 }
            if (exactHs != null) return exactHs

            val upperHs = allRanges.filter { it.upper >= 120 }.maxByOrNull { it.lower }
            if (upperHs != null) return upperHs

            // Fallback to 60 if sensor hardware doesn't support 120
            val fallback60 = allRanges.filter { it.upper >= 60 }.maxByOrNull { it.lower }
            if (fallback60 != null) return fallback60

            return supportedRanges.maxByOrNull { it.upper } ?: Range(30, 30)
        }

        // 2. 60 FPS
        if (target == 60) {
            // First check if exact [60, 60] is listed by the driver
            val exactFixed = allRanges.firstOrNull { it.lower == 60 && it.upper == 60 }
            if (exactFixed != null) return exactFixed

            // Check if driver declared [30, 60] or [15, 60] - prefer highest lower bound
            val candidate = allRanges.filter { it.upper >= 60 }.maxByOrNull { it.lower }
            if (candidate != null) return candidate

            // If hardware has no 60 FPS profile, take max supported upper FPS
            return supportedRanges.maxByOrNull { it.upper } ?: Range(30, 30)
        }

        // 3. 30 FPS
        if (target == 30) {
            val exact30 = supportedRanges.firstOrNull { it.lower == 30 && it.upper == 30 }
            if (exact30 != null) return exact30
            val best30 = supportedRanges.filter { it.upper == 30 }.maxByOrNull { it.lower }
            if (best30 != null) return best30
            return Range(30, 30)
        }

        return allRanges.firstOrNull { it.upper == target } ?: Range(target, target)
    }

    /**
     * Builds Preview use case.
     * Supports:
     * - Camera2 API with exact FPS range & SENSOR_FRAME_DURATION
     * - Auto Detect Face (STATISTICS_FACE_DETECT_MODE_SIMPLE)
     * - AE Lock (CONTROL_AE_LOCK)
     * - Modes: FOTO, POTRET (scene mode portrait & face priority), MALAM (scene mode night & multi-frame NR), VIDEO (EIS+OIS)
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun buildPreview(
        targetFpsRange: Range<Int>?,
        isPhotoMode: Boolean,
        captureMode: CameraCaptureMode = CameraCaptureMode.PHOTO,
        enableCamera2Api: Boolean = true,
        enableStabilization: Boolean = true,
        lockFpsAntiDrop: Boolean = true,
        enableFaceDetection: Boolean = true,
        isAeLocked: Boolean = false,
        onFacesDetected: ((List<Face>) -> Unit)? = null
    ): Preview {
        val previewBuilder = Preview.Builder()

        if (enableCamera2Api) {
            val camera2Extender = Camera2Interop.Extender(previewBuilder)

            // 1. Target FPS Range (Strict fixed e.g. [60, 60] preventing low-light drop)
            if (targetFpsRange != null && !isPhotoMode) {
                camera2Extender.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    targetFpsRange
                )
                Log.d(TAG, "Preview Camera2: CONTROL_AE_TARGET_FPS_RANGE = $targetFpsRange")

                if (targetFpsRange.upper > 0) {
                    val frameDurationNs = 1_000_000_000L / targetFpsRange.upper
                    camera2Extender.setCaptureRequestOption(
                        CaptureRequest.SENSOR_FRAME_DURATION,
                        frameDurationNs
                    )
                }
            }

            // 2. Continuous AF
            val afMode = if (isPhotoMode) {
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            } else {
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            }
            camera2Extender.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, afMode)
            camera2Extender.setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            camera2Extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
            camera2Extender.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO)
            camera2Extender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO
            )

            // 3. Auto Detect Face (STATISTICS_FACE_DETECT_MODE)
            if (enableFaceDetection) {
                camera2Extender.setCaptureRequestOption(
                    CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                    CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE
                )
                camera2Extender.setSessionCaptureCallback(object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                        val faces = result.get(CaptureResult.STATISTICS_FACES)
                        onFacesDetected?.invoke(faces?.toList() ?: emptyList())
                    }
                })
            } else {
                camera2Extender.setCaptureRequestOption(
                    CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                    CameraMetadata.STATISTICS_FACE_DETECT_MODE_OFF
                )
            }

            // 4. Penguncian Cahaya (AE Lock)
            camera2Extender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_LOCK,
                isAeLocked
            )

            // 5. Scene Modes & Processing per CameraCaptureMode
            when (captureMode) {
                CameraCaptureMode.PORTRAIT -> {
                    camera2Extender.setCaptureRequestOption(
                        CaptureRequest.CONTROL_SCENE_MODE,
                        CameraMetadata.CONTROL_SCENE_MODE_PORTRAIT
                    )
                    Log.d(TAG, "Preview Camera2: Mode POTRET (Portrait Scene) applied")
                }
                CameraCaptureMode.NIGHT -> {
                    camera2Extender.setCaptureRequestOption(
                        CaptureRequest.CONTROL_SCENE_MODE,
                        CameraMetadata.CONTROL_SCENE_MODE_NIGHT
                    )
                    camera2Extender.setCaptureRequestOption(
                        CaptureRequest.NOISE_REDUCTION_MODE,
                        CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY
                    )
                    camera2Extender.setCaptureRequestOption(
                        CaptureRequest.EDGE_MODE,
                        CameraMetadata.EDGE_MODE_HIGH_QUALITY
                    )
                    Log.d(TAG, "Preview Camera2: Mode MALAM (Night Scene + High Quality NR) applied")
                }
                CameraCaptureMode.MACRO -> {
                    camera2Extender.setCaptureRequestOption(
                        CaptureRequest.CONTROL_AF_MODE,
                        CameraMetadata.CONTROL_AF_MODE_MACRO
                    )
                    camera2Extender.setCaptureRequestOption(
                        CaptureRequest.EDGE_MODE,
                        CameraMetadata.EDGE_MODE_HIGH_QUALITY
                    )
                }
                CameraCaptureMode.VIDEO,
                CameraCaptureMode.SLOW_MOTION,
                CameraCaptureMode.CINEMATIC_VIDEO,
                CameraCaptureMode.PRO_VIDEO -> {
                    if (lockFpsAntiDrop) {
                        camera2Extender.setCaptureRequestOption(
                            CaptureRequest.CONTROL_SCENE_MODE,
                            CameraMetadata.CONTROL_SCENE_MODE_DISABLED
                        )
                        camera2Extender.setCaptureRequestOption(
                            CaptureRequest.NOISE_REDUCTION_MODE,
                            CameraMetadata.NOISE_REDUCTION_MODE_FAST
                        )
                        camera2Extender.setCaptureRequestOption(
                            CaptureRequest.EDGE_MODE,
                            CameraMetadata.EDGE_MODE_FAST
                        )
                    }
                }
                CameraCaptureMode.PHOTO,
                CameraCaptureMode.PRO -> {
                    // Standard photo preview
                }
            }

            // 6. Hardware Video Stabilization (EIS + OIS)
            if (enableStabilization && !isPhotoMode) {
                camera2Extender.setCaptureRequestOption(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
                )
                camera2Extender.setCaptureRequestOption(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
                )
                Log.d(TAG, "Preview Camera2: EIS and OIS Stabilization enabled")
            }
        }

        return previewBuilder.build()
    }

    /**
     * Builds ImageCapture use case configured for high quality capture.
     * In Mode Malam (Night): uses CAPTURE_MODE_MAXIMIZE_QUALITY for best low-light detail.
     */
    fun buildImageCapture(captureMode: CameraCaptureMode = CameraCaptureMode.PHOTO): ImageCapture {
        val mode = if (captureMode == CameraCaptureMode.NIGHT) {
            ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        } else {
            ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        }
        return ImageCapture.Builder()
            .setCaptureMode(mode)
            .build()
    }

    /**
     * Builds VideoCapture use case configured with the user's selected resolution, FPS, and Bitrate.
     * Open Camera approach:
     * 1. Injects high video encoding bitrate (up to 100-150 Mbps for crystal clear quality matching Open Camera)
     * 2. Explicitly injects Camera2 options (strict FPS range, continuous AF, EIS/OIS, and Anti-Drop lock)
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun buildVideoCapture(
        resolutionMode: ResolutionMode,
        targetFpsRange: Range<Int>?,
        bitrateMode: BitrateMode = BitrateMode.BITRATE_100,
        enableCamera2Api: Boolean = true,
        enableStabilization: Boolean = true,
        lockFpsAntiDrop: Boolean = true,
        enableFaceDetection: Boolean = true,
        isAeLocked: Boolean = false
    ): VideoCapture<Recorder> {
        val qualitySelector = QualitySelector.from(
            resolutionMode.quality,
            FallbackStrategy.lowerQualityOrHigherThan(resolutionMode.quality)
        )
        val recorderBuilder = Recorder.Builder()
            .setQualitySelector(qualitySelector)

        // Set high video encoding bitrate (e.g. 100 Mbps matching Open Camera ~113 MB / 10s)
        if (bitrateMode.bps > 0) {
            recorderBuilder.setTargetVideoEncodingBitRate(bitrateMode.bps)
            Log.d(TAG, "VideoCapture: targetVideoEncodingBitRate = ${bitrateMode.bps} bps (${bitrateMode.displayName})")
        }

        val recorder = recorderBuilder.build()
        val videoCaptureBuilder = VideoCapture.Builder(recorder)

        // Set explicit Target Frame Rate on VideoCapture.Builder
        // This configures CameraX VideoSpec & MediaCodec to encode at 60/120 FPS
        if (targetFpsRange != null) {
            videoCaptureBuilder.setTargetFrameRate(targetFpsRange)
            Log.d(TAG, "VideoCapture.Builder: setTargetFrameRate($targetFpsRange)")
        }

        if (enableCamera2Api) {
            val camera2Extender = Camera2Interop.Extender(videoCaptureBuilder)

            // Force the target FPS range into the video recording stream request
            if (targetFpsRange != null) {
                camera2Extender.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    targetFpsRange
                )
                Log.d(TAG, "VideoCapture Camera2: Set target FPS range $targetFpsRange")

                if (targetFpsRange.upper > 0) {
                    val frameDurationNs = 1_000_000_000L / targetFpsRange.upper
                    camera2Extender.setCaptureRequestOption(
                        CaptureRequest.SENSOR_FRAME_DURATION,
                        frameDurationNs
                    )
                }
            }

            camera2Extender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            )
            camera2Extender.setCaptureRequestOption(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO
            )
            camera2Extender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CameraMetadata.CONTROL_AE_MODE_ON
            )
            camera2Extender.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO
            )

            // Auto Detect Face
            if (enableFaceDetection) {
                camera2Extender.setCaptureRequestOption(
                    CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                    CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE
                )
            }

            // AE Lock
            if (isAeLocked) {
                camera2Extender.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_LOCK,
                    true
                )
            }

            // Open Camera Anti-Drop: prevent night scene throttling to 22 FPS
            if (lockFpsAntiDrop) {
                camera2Extender.setCaptureRequestOption(
                    CaptureRequest.CONTROL_SCENE_MODE,
                    CameraMetadata.CONTROL_SCENE_MODE_DISABLED
                )
                camera2Extender.setCaptureRequestOption(
                    CaptureRequest.NOISE_REDUCTION_MODE,
                    CameraMetadata.NOISE_REDUCTION_MODE_FAST
                )
                camera2Extender.setCaptureRequestOption(
                    CaptureRequest.EDGE_MODE,
                    CameraMetadata.EDGE_MODE_FAST
                )
            }

            // Apply Hardware EIS and OIS during video recording
            if (enableStabilization) {
                camera2Extender.setCaptureRequestOption(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
                )
                camera2Extender.setCaptureRequestOption(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
                )
                Log.d(TAG, "VideoCapture Camera2: EIS + OIS Stabilization applied to video stream")
            } else {
                camera2Extender.setCaptureRequestOption(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                )
                camera2Extender.setCaptureRequestOption(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
                )
            }
        }

        return videoCaptureBuilder.build()
    }

    /**
     * Builds ML Kit ImageAnalysis use case for real-time subject segmentation and bokeh detection.
     * Uses ML Kit Selfie Segmentation to detect person/subject bounds and confidence.
     */
    fun buildCinematicImageAnalysis(
        context: Context,
        onSubjectDetected: (isDetected: Boolean, confidence: Float, bounds: RectF?) -> Unit
    ): ImageAnalysis {
        val segmenterOptions = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
            .build()
        val segmenter = Segmentation.getClient(segmenterOptions)

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                segmenter.process(inputImage)
                    .addOnSuccessListener { mask ->
                        val w = mask.width
                        val h = mask.height
                        val buffer: ByteBuffer = mask.buffer
                        buffer.rewind()

                        var subjectPixels = 0
                        var totalSamples = 0
                        var minX = w
                        var maxX = 0
                        var minY = h
                        var maxY = 0

                        val step = 8 // Sampling step for smooth 60 FPS performance
                        for (y in 0 until h step step) {
                            for (x in 0 until w step step) {
                                val index = y * w + x
                                if (index * 4 + 3 < buffer.capacity()) {
                                    val conf = buffer.getFloat(index * 4)
                                    if (conf > 0.5f) {
                                        subjectPixels++
                                        if (x < minX) minX = x
                                        if (x > maxX) maxX = x
                                        if (y < minY) minY = y
                                        if (y > maxY) maxY = y
                                    }
                                    totalSamples++
                                }
                            }
                        }

                        val hasSubject = subjectPixels > 5
                        val confidence = if (totalSamples > 0) subjectPixels.toFloat() / totalSamples else 0f
                        val normRect = if (hasSubject && maxX > minX && maxY > minY) {
                            RectF(
                                minX.toFloat() / w,
                                minY.toFloat() / h,
                                maxX.toFloat() / w,
                                maxY.toFloat() / h
                            )
                        } else null

                        onSubjectDetected(hasSubject, confidence, normRect)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "ML Kit Segmentation failed: ${e.message}")
                        onSubjectDetected(false, 0f, null)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }

        return imageAnalysis
    }

    /**
     * Safely applies camera controls to the active session based on Camera2 API toggle,
     * Anti-Drop FPS lock, Video Stabilization state, Face Detection, and AE Lock.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun applyActiveHardwareSettings(
        context: Context,
        camera: Camera,
        targetFpsRange: Range<Int>?,
        isPhotoMode: Boolean,
        captureMode: CameraCaptureMode = CameraCaptureMode.PHOTO,
        enableCamera2Api: Boolean = true,
        enableStabilization: Boolean = true,
        lockFpsAntiDrop: Boolean = true,
        enableFaceDetection: Boolean = true,
        isAeLocked: Boolean = false
    ) {
        try {
            val camera2CameraControl = Camera2CameraControl.from(camera.cameraControl)

            if (!enableCamera2Api) {
                camera2CameraControl.clearCaptureRequestOptions()
                Log.d(TAG, "Camera2 API OFF: Cleared custom capture request options")
                return
            }

            val builder = CaptureRequestOptions.Builder()

            if (targetFpsRange != null && !isPhotoMode) {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    targetFpsRange
                )

                if (targetFpsRange.upper > 0) {
                    val frameDurationNs = 1_000_000_000L / targetFpsRange.upper
                    builder.setCaptureRequestOption(
                        CaptureRequest.SENSOR_FRAME_DURATION,
                        frameDurationNs
                    )
                }
            }

            val afMode = if (isPhotoMode) {
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            } else {
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            }

            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, afMode)
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
            builder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_AUTO)
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_ANTIBANDING_MODE,
                CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO
            )

            // Auto Detect Face
            if (enableFaceDetection) {
                builder.setCaptureRequestOption(
                    CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                    CameraMetadata.STATISTICS_FACE_DETECT_MODE_SIMPLE
                )
            } else {
                builder.setCaptureRequestOption(
                    CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                    CameraMetadata.STATISTICS_FACE_DETECT_MODE_OFF
                )
            }

            // Penguncian Cahaya (AE Lock)
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_LOCK,
                isAeLocked
            )

            // Mode-specific scene settings
            when (captureMode) {
                CameraCaptureMode.PORTRAIT -> {
                    builder.setCaptureRequestOption(
                        CaptureRequest.CONTROL_SCENE_MODE,
                        CameraMetadata.CONTROL_SCENE_MODE_PORTRAIT
                    )
                }
                CameraCaptureMode.NIGHT -> {
                    builder.setCaptureRequestOption(
                        CaptureRequest.CONTROL_SCENE_MODE,
                        CameraMetadata.CONTROL_SCENE_MODE_NIGHT
                    )
                    builder.setCaptureRequestOption(
                        CaptureRequest.NOISE_REDUCTION_MODE,
                        CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY
                    )
                    builder.setCaptureRequestOption(
                        CaptureRequest.EDGE_MODE,
                        CameraMetadata.EDGE_MODE_HIGH_QUALITY
                    )
                }
                CameraCaptureMode.VIDEO,
                CameraCaptureMode.SLOW_MOTION,
                CameraCaptureMode.CINEMATIC_VIDEO,
                CameraCaptureMode.PRO_VIDEO -> {
                    if (lockFpsAntiDrop) {
                        builder.setCaptureRequestOption(
                            CaptureRequest.CONTROL_SCENE_MODE,
                            CameraMetadata.CONTROL_SCENE_MODE_DISABLED
                        )
                        builder.setCaptureRequestOption(
                            CaptureRequest.NOISE_REDUCTION_MODE,
                            CameraMetadata.NOISE_REDUCTION_MODE_FAST
                        )
                        builder.setCaptureRequestOption(
                            CaptureRequest.EDGE_MODE,
                            CameraMetadata.EDGE_MODE_FAST
                        )
                    }
                }
                CameraCaptureMode.PHOTO,
                CameraCaptureMode.PRO,
                CameraCaptureMode.MACRO -> {
                    // standard
                }
            }

            if (enableStabilization && !isPhotoMode) {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON
                )
                builder.setCaptureRequestOption(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON
                )
            } else {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF
                )
                builder.setCaptureRequestOption(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF
                )
            }

            camera2CameraControl.setCaptureRequestOptions(builder.build())
                .addListener(
                    {
                        Log.d(TAG, "Hardware settings applied: Mode=${captureMode.name}, Camera2=ON, FaceDetect=$enableFaceDetection, AELock=$isAeLocked")
                    },
                    ContextCompat.getMainExecutor(context)
                )
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menerapkan capture request options: ${e.message}")
        }
    }

    /**
     * Toggles hardware Auto Exposure (AE) Lock on or off.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun setAeLock(camera: Camera?, isLocked: Boolean) {
        val cam = camera ?: return
        try {
            val camera2Control = Camera2CameraControl.from(cam.cameraControl)
            val options = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, isLocked)
                .build()
            camera2Control.setCaptureRequestOptions(options)
            Log.d(TAG, "AE Lock set to: $isLocked")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengatur AE Lock: ${e.message}")
        }
    }

    /**
     * Sets exposure compensation index safely within the device's hardware supported range.
     */
    fun setExposureCompensation(camera: Camera?, index: Int) {
        val cam = camera ?: return
        try {
            val state = cam.cameraInfo.exposureState
            if (state.isExposureCompensationSupported) {
                val clamped = index.coerceIn(state.exposureCompensationRange.lower, state.exposureCompensationRange.upper)
                cam.cameraControl.setExposureCompensationIndex(clamped)
                Log.d(TAG, "Exposure compensation index set to $clamped")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengatur exposure compensation: ${e.message}")
        }
    }

    /**
     * Applies manual settings (ISO, Shutter Speed, White Balance, Manual Focus) for Mode Pro Video.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun applyProVideoSettings(
        camera: Camera?,
        settings: ProVideoManualSettings
    ) {
        val cam = camera ?: return
        try {
            val camera2Control = Camera2CameraControl.from(cam.cameraControl)
            val builder = CaptureRequestOptions.Builder()

            // 1. Manual ISO & Shutter Speed
            if (settings.iso > 0 || settings.shutterSpeedNs > 0L) {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CameraMetadata.CONTROL_AE_MODE_OFF
                )
                if (settings.iso > 0) {
                    builder.setCaptureRequestOption(
                        CaptureRequest.SENSOR_SENSITIVITY,
                        settings.iso
                    )
                }
                if (settings.shutterSpeedNs > 0L) {
                    builder.setCaptureRequestOption(
                        CaptureRequest.SENSOR_EXPOSURE_TIME,
                        settings.shutterSpeedNs
                    )
                }
            } else {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CameraMetadata.CONTROL_AE_MODE_ON
                )
            }

            // 2. White Balance
            builder.setCaptureRequestOption(
                CaptureRequest.CONTROL_AWB_MODE,
                settings.awbMode
            )

            // 3. Manual Focus
            if (settings.focusDistance >= 0f) {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CameraMetadata.CONTROL_AF_MODE_OFF
                )
                builder.setCaptureRequestOption(
                    CaptureRequest.LENS_FOCUS_DISTANCE,
                    settings.focusDistance
                )
            } else {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                )
            }

            camera2Control.setCaptureRequestOptions(builder.build())
            Log.d(TAG, "Pro Video Manual Settings applied: ISO=${settings.iso}, Shutter=${settings.shutterSpeedNs}ns, WB=${settings.awbMode}, Focus=${settings.focusDistance}")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menerapkan Pro Video manual settings: ${e.message}")
        }
    }

    /**
     * Formats exposure index into readable EV string (e.g. +0.7 EV, 0.0 EV, -1.3 EV).
     */
    fun formatEvString(index: Int, step: Rational?): String {
        if (step == null || step.denominator == 0) {
            return if (index > 0) "+$index EV" else "$index EV"
        }
        val ev = index * (step.numerator.toFloat() / step.denominator.toFloat())
        return if (ev > 0.05f) {
            String.format(Locale.US, "+%.1f EV", ev)
        } else if (ev < -0.05f) {
            String.format(Locale.US, "%.1f EV", ev)
        } else {
            "0.0 EV"
        }
    }

    /**
     * Triggers active autofocus cycle on Samsung HAL (forces the lens to re-focus).
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun triggerAutofocus(camera: Camera, previewView: PreviewView?, onComplete: () -> Unit = {}) {
        try {
            camera.cameraControl.cancelFocusAndMetering()

            previewView?.let { pView ->
                val centerX = pView.width / 2f
                val centerY = pView.height / 2f
                val factory = pView.meteringPointFactory
                val point = factory.createPoint(centerX, centerY)
                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()
                camera.cameraControl.startFocusAndMetering(action)
            }

            val camera2CameraControl = Camera2CameraControl.from(camera.cameraControl)
            val triggerOptions = CaptureRequestOptions.Builder()
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START
                )
                .build()

            camera2CameraControl.setCaptureRequestOptions(triggerOptions)
            Log.d(TAG, "CONTROL_AF_TRIGGER_START sent to camera")
            onComplete()
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering autofocus: ${e.message}")
            onComplete()
        }
    }

    /**
     * Inspects camera characteristics to read real sensor capabilities, Camera2 Hardware Level,
     * High Speed video capabilities, and EIS/OIS stabilization support.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun inspectCameraHardware(
        camera: Camera,
        activeRange: Range<Int>?,
        isCamera2Enabled: Boolean,
        isStabilizationActive: Boolean = true
    ): CameraHardwareDetails {
        return try {
            val camera2Info = Camera2CameraInfo.from(camera.cameraInfo)
            val fpsRanges = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
            )?.toList() ?: emptyList()

            val afModes = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES
            )?.toList() ?: emptyList()

            val hwLevel = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
            )
            val hwLevelString = when (hwLevel) {
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
                else -> "LIMITED"
            }

            // Inspect video stabilization modes (EIS)
            val videoStabModes = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES
            )?.toList() ?: emptyList()
            val hasEis = videoStabModes.contains(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON)

            // Inspect optical image stabilization modes (OIS)
            val oisModes = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION
            )?.toList() ?: emptyList()
            val hasOis = oisModes.contains(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON)

            // Inspect high-speed video fps ranges from StreamConfigurationMap
            val streamMap = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
            )
            val highSpeedRanges = streamMap?.highSpeedVideoFpsRanges?.toList() ?: emptyList()

            val has60Fps = fpsRanges.any { it.upper >= 60 } || highSpeedRanges.any { it.upper >= 60 }
            val hasContinuousAf = afModes.contains(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO) ||
                    afModes.contains(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

            val readableAfModes = afModes.map { mode ->
                when (mode) {
                    CameraMetadata.CONTROL_AF_MODE_OFF -> "OFF (Fixed Focus)"
                    CameraMetadata.CONTROL_AF_MODE_AUTO -> "AUTO"
                    CameraMetadata.CONTROL_AF_MODE_MACRO -> "MACRO"
                    CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> "CONTINUOUS_VIDEO"
                    CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> "CONTINUOUS_PICTURE"
                    CameraMetadata.CONTROL_AF_MODE_EDOF -> "EDOF"
                    else -> "MODE_$mode"
                }
            }

            CameraHardwareDetails(
                sensorName = Build.MODEL,
                hardwareLevel = hwLevelString,
                isCamera2ApiEnabled = isCamera2Enabled,
                is60FpsSupported = has60Fps,
                availableFpsRanges = fpsRanges,
                highSpeedFpsRanges = highSpeedRanges,
                activeFpsRange = activeRange,
                isContinuousAfSupported = hasContinuousAf,
                isEisSupported = hasEis,
                isOisSupported = hasOis,
                isStabilizationActive = isStabilizationActive,
                availableAfModes = readableAfModes,
                activeAfMode = if (hasContinuousAf) "CONTINUOUS" else "AUTO",
                isSamsungDevice = isSamsungDevice
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memeriksa CameraCharacteristics: ${e.message}", e)
            CameraHardwareDetails(
                sensorName = Build.MODEL,
                hardwareLevel = "LIMITED",
                isCamera2ApiEnabled = isCamera2Enabled,
                isStabilizationActive = isStabilizationActive,
                isSamsungDevice = isSamsungDevice
            )
        }
    }

    /**
     * Performs tap to focus with auto-cancel timer back to continuous autofocus.
     */
    fun performTapToFocus(
        previewView: PreviewView,
        cameraControl: CameraControl,
        x: Float,
        y: Float,
        onFocusResult: (Boolean) -> Unit = {}
    ) {
        try {
            val factory = previewView.meteringPointFactory
            val point = factory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(
                point,
                FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
            )
                .setAutoCancelDuration(4, TimeUnit.SECONDS)
                .build()

            val future = cameraControl.startFocusAndMetering(action)
            future.addListener(
                {
                    try {
                        val result = future.get()
                        onFocusResult(result.isFocusSuccessful)
                    } catch (e: Exception) {
                        onFocusResult(false)
                    }
                },
                ContextCompat.getMainExecutor(previewView.context)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error saat tap to focus: ${e.message}")
            onFocusResult(false)
        }
    }

    /**
     * Queries all recent captured photos and videos from MediaStore (DCIM/Camera & Pictures).
     * This ensures the gallery viewer never opens to an empty screen and lists actual captures.
     */
    fun queryRecentMediaList(context: Context, limit: Int = 30): List<CapturedMediaItem> {
        val mediaList = mutableListOf<CapturedMediaItem>()

        // 1. Query Images
        try {
            val imageProjection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )
            val imageSortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            val imageCursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                null,
                null,
                imageSortOrder
            )
            imageCursor?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Foto"
                    val dateAdded = cursor.getLong(dateCol) * 1000L
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    mediaList.add(
                        CapturedMediaItem(
                            uri = uri,
                            isVideo = false,
                            displayName = name,
                            dateAddedMillis = dateAdded,
                            relativePath = "DCIM/Camera"
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal query gambar: ${e.message}")
        }

        // 2. Query Videos
        try {
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED
            )
            val videoSortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"
            val videoCursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                videoSortOrder
            )
            videoCursor?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: "Video"
                    val dateAdded = cursor.getLong(dateCol) * 1000L
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    mediaList.add(
                        CapturedMediaItem(
                            uri = uri,
                            isVideo = true,
                            displayName = name,
                            dateAddedMillis = dateAdded,
                            relativePath = "DCIM/Camera"
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal query video: ${e.message}")
        }

        // Sort combined list newest first
        return mediaList.sortedByDescending { it.dateAddedMillis }.take(limit)
    }

    /**
     * Queries the single latest captured photo in MediaStore.
     */
    fun queryLatestCapturedPhoto(context: Context): Uri? {
        val items = queryRecentMediaList(context, limit = 1)
        return items.firstOrNull()?.uri
    }

    /**
     * Takes a high-resolution photo and saves directly to the standard Android & Samsung Camera roll:
     * DCIM/Camera
     * This fixes the "folder ternyata kosong" issue by placing it where Samsung Gallery looks by default.
     */
    fun takePhoto(
        context: Context,
        imageCapture: ImageCapture,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "SAMSUNG_IMG_$timeStamp.jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Save to standard DCIM/Camera so Samsung Gallery immediately indexes it
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: queryLatestCapturedPhoto(context) ?: Uri.EMPTY
                    Log.d(TAG, "Foto berhasil disimpan ke $savedUri di DCIM/Camera")

                    // Notify media scanner to immediately register file in Samsung Gallery
                    try {
                        MediaScannerConnection.scanFile(
                            context.applicationContext,
                            arrayOf(savedUri.toString()),
                            arrayOf("image/jpeg")
                        ) { _, _ ->
                            Log.d(TAG, "MediaScanner scan completed for $savedUri")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "MediaScanner error: ${e.message}")
                    }

                    onSuccess(savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Gagal mengambil foto: ${exception.message}", exception)
                    onError(exception.message ?: "Unknown image capture error")
                }
            }
        )
    }

    /**
     * Prepares a video recording session storing output directly to standard DCIM/Camera.
     * Both Samsung Gallery and Google Photos recognize DCIM/Camera as the primary camera folder.
     */
    fun prepareRecording(
        context: Context,
        videoCapture: VideoCapture<Recorder>,
        fpsLabel: String,
        enableAudio: Boolean,
        onEvent: (VideoRecordEvent) -> Unit
    ): Recording {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "SAMSUNG_VID_${fpsLabel}_$timeStamp.mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "DCIM/Camera")
            }
        }

        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()

        var pending: PendingRecording = videoCapture.output.prepareRecording(context, mediaStoreOutput)
        if (enableAudio) {
            try {
                pending = pending.withAudioEnabled()
            } catch (e: SecurityException) {
                Log.w(TAG, "Izin mikrofon belum diberikan: ${e.message}")
            }
        }

        return pending.start(ContextCompat.getMainExecutor(context)) { event ->
            if (event is VideoRecordEvent.Finalize && !event.hasError()) {
                val outputUri = event.outputResults.outputUri
                Log.d(TAG, "Video berhasil difinalisasi ke $outputUri di DCIM/Camera")
                try {
                    MediaScannerConnection.scanFile(
                        context.applicationContext,
                        arrayOf(outputUri.toString()),
                        arrayOf("video/mp4")
                    ) { _, _ ->
                        Log.d(TAG, "MediaScanner video scan completed")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "MediaScanner error: ${e.message}")
                }
            }
            onEvent(event)
        }
    }
}
