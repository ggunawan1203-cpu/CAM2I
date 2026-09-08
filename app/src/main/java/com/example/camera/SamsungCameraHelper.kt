package com.example.camera

import android.content.ContentValues
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
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

private const val TAG = "Samsung60FpsCamera"

/**
 * Diagnostic data containing camera hardware details detected from Samsung HAL.
 */
data class CameraHardwareDetails(
    val sensorName: String = "Unknown",
    val is60FpsSupported: Boolean = false,
    val availableFpsRanges: List<Range<Int>> = emptyList(),
    val isContinuousAfSupported: Boolean = false,
    val availableAfModes: List<String> = emptyList(),
    val isSamsungDevice: Boolean = false
)

object SamsungCameraHelper {

    val isSamsungDevice: Boolean
        get() = Build.MANUFACTURER.contains("samsung", ignoreCase = true) ||
                Build.BRAND.contains("samsung", ignoreCase = true)

    /**
     * Build CameraX Preview use case with Camera2Interop injecting:
     * - CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE to (60, 60)
     * - CaptureRequest.CONTROL_AF_MODE to CONTROL_AF_MODE_CONTINUOUS_VIDEO
     * - CaptureRequest.CONTROL_MODE to CONTROL_MODE_AUTO
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun build60FpsPreview(): Preview {
        val previewBuilder = Preview.Builder()
        val camera2Extender = Camera2Interop.Extender(previewBuilder)

        // 1. Memaksa target FPS ke 60 FPS range (60, 60) pada level hardware HAL Samsung
        camera2Extender.setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
            Range(60, 60)
        )

        // 2. Memaksa Continuous Video Auto Focus tetap aktif (mencegah autofocus terkunci pada Samsung)
        camera2Extender.setCaptureRequestOption(
            CaptureRequest.CONTROL_AF_MODE,
            CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO
        )

        // 3. Pastikan Auto Mode dan Auto Exposure aktif
        camera2Extender.setCaptureRequestOption(
            CaptureRequest.CONTROL_MODE,
            CameraMetadata.CONTROL_MODE_AUTO
        )
        camera2Extender.setCaptureRequestOption(
            CaptureRequest.CONTROL_AE_MODE,
            CameraMetadata.CONTROL_AE_MODE_ON
        )

        Log.d(TAG, "Preview dibangun dengan target FPS (60, 60) dan CONTINUOUS_VIDEO AF")
        return previewBuilder.build()
    }

    /**
     * Build CameraX VideoCapture use case configured with high quality FHD recording.
     */
    fun buildVideoCapture(): VideoCapture<Recorder> {
        val qualitySelector = QualitySelector.from(
            Quality.FHD,
            FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
        )
        val recorder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .build()

        return VideoCapture.withOutput(recorder)
    }

    /**
     * Apply Camera2 capture request options directly onto the active camera stream.
     * This guarantees that during video recording repeating requests, Samsung's HAL
     * does not revert back to 30 FPS or fixed focus.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun enforceSamsungHardwareSettings(context: Context, camera: Camera) {
        val camera2CameraControl = Camera2CameraControl.from(camera.cameraControl)

        val captureRequestOptions = CaptureRequestOptions.Builder()
            .setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                Range(60, 60)
            )
            .setCaptureRequestOption(
                CaptureRequest.CONTROL_AF_MODE,
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            )
            .setCaptureRequestOption(
                CaptureRequest.CONTROL_MODE,
                CameraMetadata.CONTROL_MODE_AUTO
            )
            .setCaptureRequestOption(
                CaptureRequest.CONTROL_AE_MODE,
                CameraMetadata.CONTROL_AE_MODE_ON
            )
            .build()

        camera2CameraControl.setCaptureRequestOptions(captureRequestOptions)
            .addListener(
                { Log.d(TAG, "Hardware settings (60, 60 FPS + Continuous AF) berhasil diterapkan ke Camera Control") },
                ContextCompat.getMainExecutor(context)
            )
    }

    /**
     * Query Camera2 characteristics to inspect supported FPS ranges and AF modes on Samsung HAL.
     */
    @OptIn(ExperimentalCamera2Interop::class)
    fun inspectCameraHardware(camera: Camera): CameraHardwareDetails {
        return try {
            val camera2Info = Camera2CameraInfo.from(camera.cameraInfo)
            val fpsRanges = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
            )?.toList() ?: emptyList()

            val afModes = camera2Info.getCameraCharacteristic(
                CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES
            )?.toList() ?: emptyList()

            val has60Fps = fpsRanges.any { it.upper >= 60 }
            val hasContinuousAf = afModes.contains(CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO)

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
                is60FpsSupported = has60Fps,
                availableFpsRanges = fpsRanges,
                isContinuousAfSupported = hasContinuousAf,
                availableAfModes = readableAfModes,
                isSamsungDevice = isSamsungDevice
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memeriksa CameraCharacteristics: ${e.message}", e)
            CameraHardwareDetails(
                sensorName = Build.MODEL,
                isSamsungDevice = isSamsungDevice
            )
        }
    }

    /**
     * Trigger manual tap-to-focus on preview without permanently disabling continuous AF.
     * After 3 seconds auto-cancel duration, it smoothly returns to continuous video AF.
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
                .setAutoCancelDuration(3, TimeUnit.SECONDS)
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
     * Prepare a video recording session storing output directly to MediaStore Movies directory.
     */
    fun prepareRecording(
        context: Context,
        videoCapture: VideoCapture<Recorder>,
        enableAudio: Boolean,
        onEvent: (VideoRecordEvent) -> Unit
    ): Recording {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "SAMSUNG_60FPS_$timeStamp.mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Samsung60FPS")
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
                Log.w(TAG, "Audio recording permission not granted: ${e.message}")
            }
        }

        return pending.start(ContextCompat.getMainExecutor(context)) { event ->
            onEvent(event)
        }
    }
}
