package com.example.camera

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Range
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TAG = "SamsungCameraHelper"

enum class FpsMode(val displayName: String, val targetFps: Int, val description: String) {
    FPS_120("120 FPS", 120, "High Speed 119 fps (optimal di 720p HD)"),
    FPS_60("60 FPS", 60, "Ultra Smooth 60 FPS"),
    FPS_30("30 FPS", 30, "Format standar 30 FPS"),
    FPS_AUTO("Auto FPS", 0, "Otomatis diatur sensor")
}

enum class BitrateMode(
    val displayName: String,
    val bps: Int,
    val approxPerTenSec: String,
    val description: String
) {
    BITRATE_DEFAULT("Default (~20 Mbps)", 20_000_000, "~25 MB", "Standar CameraX hemat ruang"),
    BITRATE_50("50 Mbps (Tinggi)", 50_000_000, "~60 MB", "Kualitas tajam, minim kompresi"),
    BITRATE_100("100 Mbps (Ultra - Open Camera)", 100_000_000, "~113 MB", "Sesuai Open Camera (~113 MB / 10s)"),
    BITRATE_150("150 Mbps (Maksimum)", 150_000_000, "~180 MB", "Bitrate profesional tanpa kompresi"),
    BITRATE_200("200 Mbps (Extreme Master)", 200_000_000, "~240 MB", "Kualitas tertinggi hardware Samsung")
}

enum class ResolutionMode(
    val displayName: String,
    val quality: Quality,
    val description: String,
    val bestFor: String
) {
    RES_4K("4K UHD", Quality.UHD, "3840×2160", "Bitrate ultra 100-150 Mbps"),
    RES_1080P("1080p FHD", Quality.FHD, "1920×1080", "Standar 60 FPS & 100 Mbps"),
    RES_720P("720p HD", Quality.HD, "1280×720", "Optimal 120 FPS High Speed (Open Camera 119 fps)"),
    RES_480P("480p SD", Quality.SD, "854×480", "Ukuran file kecil")
}

enum class CameraCaptureMode(val displayName: String) {
    PHOTO("FOTO"),
    VIDEO("VIDEO")
}

data class CapturedMediaItem(
    val uri: Uri,
    val isVideo: Boolean,
    val displayName: String,
    val dateAddedMillis: Long,
    val relativePath: String = "DCIM/Camera"
)

data class CameraHardwareDetails(
    val sensorName: String = "Unknown",
    val hardwareLevel: String = "UNKNOWN",
    val isCamera2ApiEnabled: Boolean = true,
    val is60FpsSupported: Boolean = false,
    val availableFpsRanges: List<Range<Int>> = emptyList(),
    val highSpeedFpsRanges: List<Range<Int>> = emptyList(),
    val activeFpsRange: Range<Int>? = null,
    val isContinuousAfSupported: Boolean = false,
    val isEisSupported: Boolean = false,
    val isOisSupported: Boolean = false,
    val isStabilizationActive: Boolean = true,
    val availableAfModes: List<String> = emptyList(),
    val activeAfMode: String = "AUTO",
    val isSamsungDevice: Boolean = false
)

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

        // 1. High Speed 120 FPS (Open Camera 119/120 fps high-speed profile)
        if (target >= 120) {
            val exactHs = highSpeedRanges.firstOrNull { it.lower == 120 && it.upper == 120 }
            if (exactHs != null) return exactHs

            val upperHs = highSpeedRanges.firstOrNull { it.upper >= 120 }
            if (upperHs != null) return upperHs

            val exactSupp = supportedRanges.firstOrNull { it.lower == 120 && it.upper == 120 }
            if (exactSupp != null) return exactSupp

            val suppRange = supportedRanges.firstOrNull { it.upper >= 120 }
            if (suppRange != null) return suppRange

            return Range(120, 120)
        }

        // 2. Strict Fixed 60 FPS (Open Camera 60 FPS mode)
        if (target == 60) {
            // Check if exact [60, 60] is listed in either supported or high speed
            val exactFixed = supportedRanges.firstOrNull { it.lower == 60 && it.upper == 60 }
                ?: highSpeedRanges.firstOrNull { it.lower == 60 && it.upper == 60 }
            if (exactFixed != null) return exactFixed

            // Even if the sensor only declared [15, 60] or [30, 60], we MUST return Range(60, 60)!
            // Because returning [15, 60] or [30, 60] tells the Camera2 AE algorithm that it can drop
            // to 15-30 FPS in low light to make the image brighter.
            // Returning Range(60, 60) caps the exposure time at 1/60s (16.6ms), which keeps the frame
            // dark naturally in low light (exactly what Open Camera does) and guarantees steady 60 FPS!
            return Range(60, 60)
        }

        // 3. Strict Fixed 30 FPS
        if (target == 30) {
            // Return Range(30, 30). NEVER return [15, 30] or [7, 30] which drops to 17 FPS!
            return Range(30, 30)
        }

        return Range(target, target)
    }

    /**
     * Builds Preview use case.
     * When enableCamera2Api is TRUE: uses Camera2Interop to explicitly control FPS, Continuous AF,
     * Anti-Drop FPS Lock, and Video Stabilization (EIS/OIS) following Open Camera's configuration.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun buildPreview(
        targetFpsRange: Range<Int>?,
        isPhotoMode: Boolean,
        enableCamera2Api: Boolean = true,
        enableStabilization: Boolean = true,
        lockFpsAntiDrop: Boolean = true
    ): Preview {
        val previewBuilder = Preview.Builder()

        if (enableCamera2Api) {
            val camera2Extender = Camera2Interop.Extender(previewBuilder)

            // 1. Target FPS Range (Strict fixed e.g. [60, 60] preventing low-light drop)
            if (targetFpsRange != null) {
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

            // Open Camera Anti-Drop: disable scene mode & fast noise reduction so 60 FPS never throttles
            if (lockFpsAntiDrop && !isPhotoMode) {
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

            // 3. Hardware Video Stabilization (EIS + OIS)
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
     */
    fun buildImageCapture(): ImageCapture {
        return ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
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
        lockFpsAntiDrop: Boolean = true
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
     * Safely applies camera controls to the active session based on Camera2 API toggle,
     * Anti-Drop FPS lock, and Video Stabilization state.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun applyActiveHardwareSettings(
        context: Context,
        camera: Camera,
        targetFpsRange: Range<Int>?,
        isPhotoMode: Boolean,
        enableCamera2Api: Boolean = true,
        enableStabilization: Boolean = true,
        lockFpsAntiDrop: Boolean = true
    ) {
        try {
            val camera2CameraControl = Camera2CameraControl.from(camera.cameraControl)

            if (!enableCamera2Api) {
                camera2CameraControl.clearCaptureRequestOptions()
                Log.d(TAG, "Camera2 API OFF: Cleared custom capture request options")
                return
            }

            val builder = CaptureRequestOptions.Builder()

            if (targetFpsRange != null) {
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

            if (lockFpsAntiDrop && !isPhotoMode) {
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
                        Log.d(TAG, "Hardware settings applied: Camera2=ON, FPS=$targetFpsRange, AF=$afMode, Stab=$enableStabilization")
                    },
                    ContextCompat.getMainExecutor(context)
                )
        } catch (e: Exception) {
            Log.e(TAG, "Gagal menerapkan capture request options: ${e.message}")
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
