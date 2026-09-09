package com.example.camera

import android.graphics.RectF
import android.hardware.camera2.CameraMetadata
import android.net.Uri
import android.util.Range
import androidx.camera.video.Quality
import androidx.compose.ui.graphics.Color

/**
 * Camera capture modes aligning with Samsung Galaxy A15 One UI Camera.
 */
enum class CameraCaptureMode(
    val id: String,
    val displayName: String,
    val isVideo: Boolean = false,
    val description: String = ""
) {
    PORTRAIT("PORTRAIT", "POTRET", false, "Live Focus dengan efek kedalaman bokeh & fokus prioritas wajah"),
    PHOTO("PHOTO", "FOTO", false, "Resolusi maksimal 50MP dengan detail tajam & Pengoptimal Adegan AI"),
    NIGHT("NIGHT", "MALAM", false, "Mode Malam multi-frame noise reduction adaptif Samsung"),
    VIDEO("VIDEO", "VIDEO", true, "Video ultra halus 60 FPS & 120 FPS dipaksa aktif via Camera2 API"),
    SLOW_MOTION("SLOW_MOTION", "GERAKAN LAMBAT", true, "Perekaman High-Speed 120 FPS di 720p HD"),
    CINEMATIC_VIDEO("CINEMATIC_VIDEO", "SINEMATIK", true, "Efek Bokeh ML Kit AI, Rasio Bioskop 2.39:1 & Aperture f/1.4"),
    PRO_VIDEO("PRO_VIDEO", "PRO VIDEO", true, "Perekaman video manual dengan Audio VU Meter & 60 FPS"),
    PRO("PRO", "PRO", false, "Kontrol manual profesional ISO, Shutter, Fokus Peaking, & WB"),
    MACRO("MACRO", "MAKRO", false, "Fokus dekat jarak 3-5cm untuk objek kecil & detail mikro")
}

/**
 * Target Frame Rate Modes for Samsung A15 Camera2 Engine.
 */
enum class FpsMode(val displayName: String, val targetFps: Int, val description: String) {
    FPS_120("120 FPS", 120, "High-Speed 120 FPS (Gerakan Lambat di 720p HD)"),
    FPS_60("60 FPS", 60, "Ultra Smooth 60 FPS (Dipaksa aktif via Camera2)"),
    FPS_30("30 FPS", 30, "Format standar 30 FPS"),
    FPS_AUTO("Auto FPS", 0, "Otomatis diatur sensor Samsung")
}

/**
 * Video Resolution Modes.
 */
enum class ResolutionMode(
    val displayName: String,
    val quality: Quality,
    val width: Int,
    val height: Int,
    val description: String,
    val bestFor: String
) {
    RES_4K("4K UHD", Quality.UHD, 3840, 2160, "3840×2160", "Bitrate ultra 100-150 Mbps"),
    RES_1080P("1080p FHD", Quality.FHD, 1920, 1080, "1920×1080", "Standar 60 FPS & 100 Mbps"),
    RES_720P("720p HD", Quality.HD, 1280, 720, "1280×720", "Optimal 120 FPS High Speed (120 fps)"),
    RES_480P("480p SD", Quality.SD, 720, 480, "854×480", "Ukuran file kecil hemat memori")
}

/**
 * Video Bitrate Modes.
 */
enum class BitrateMode(
    val displayName: String,
    val bps: Int,
    val approxPerTenSec: String,
    val description: String
) {
    BITRATE_DEFAULT("Default (~20 Mbps)", 20_000_000, "~25 MB", "Standar hemat memori"),
    BITRATE_50("50 Mbps (Tinggi)", 50_000_000, "~62 MB", "Kualitas tajam, minim artifak"),
    BITRATE_100("100 Mbps (Ultra A15)", 100_000_000, "~125 MB", "Bitrate profesional tanpa kompresi"),
    BITRATE_150("150 Mbps (Master)", 150_000_000, "~185 MB", "Kualitas maksimum sensor Helio G99"),
    BITRATE_200("200 Mbps (Extreme)", 200_000_000, "~245 MB", "Kualitas tertinggi hardware")
}

/**
 * Aspect Ratio / Sensor Resolution Modes.
 */
enum class AspectRatioMode(val displayName: String, val ratioLabel: String, val is50Mp: Boolean = false) {
    RATIO_3_4("3:4", "3:4"),
    RATIO_50MP("50MP", "3:4 50MP", true),
    RATIO_9_16("9:16", "9:16"),
    RATIO_1_1("1:1", "1:1"),
    RATIO_FULL("Full", "Full")
}

/**
 * Flash Modes.
 */
enum class FlashMode(val displayName: String) {
    OFF("Mati"),
    AUTO("Otomatis"),
    ON("Aktif"),
    TORCH("Lampu Kilat")
}

/**
 * Timer Modes.
 */
enum class TimerMode(val seconds: Int, val displayName: String) {
    OFF(0, "Mati"),
    S2(2, "2d"),
    S5(5, "5d"),
    S10(10, "10d")
}

/**
 * Portrait / Live Focus Blur Effects.
 */
enum class PortraitEffect(val displayName: String, val description: String) {
    BLUR("Blur", "Efek kabur latar belakang natural"),
    STUDIO("Studio", "Pencahayaan potret studio wajah cerah"),
    HIGH_KEY_MONO("High-Key Mono", "Monokrom cerah dengan kontras artistik"),
    LOW_KEY_MONO("Low-Key Mono", "Monokrom dramatis gelap"),
    COLOR_POINT("Color Point", "Warna subjek dipertahankan, latar monokrom"),
    BIG_CIRCLE("Big Circle", "Bokeh lingkaran besar khas lensa bioskop")
}

/**
 * Color Filters (Samsung Camera Style).
 */
enum class ColorFilterMode(val displayName: String, val tintColor: Color) {
    ORIGINAL("Asli", Color.Transparent),
    WARM("Hangat", Color(0x1EF59E0B)),
    COOL("Sejuk", Color(0x1E38BDF8)),
    FROST("Frost", Color(0x1806B6D4)),
    IVORY("Ivory", Color(0x18FEF08A)),
    MONO("Hitam Putih", Color(0x33000000)),
    CINEMA("Film Sinema", Color(0x1E6366F1)),
    VINTAGE("Vintage", Color(0x2278350F))
}

/**
 * Pro Manual Camera Settings.
 */
data class ProVideoManualSettings(
    val iso: Int = 0, // 0 = Auto, or 50, 100, 200, 400, 800, 1600, 3200
    val shutterSpeedNs: Long = 0L, // 0L = Auto, or 1/30s, 1/60s, 1/125s, 1/250s, 1/500s, 1/1000s
    val awbMode: Int = CameraMetadata.CONTROL_AWB_MODE_AUTO,
    val focusDistance: Float = -1f, // -1f = Auto Continuous, 0.0f = infinity, 10.0f = macro
    val audioSource: String = "OMNI", // OMNI, FRONT, REAR
    val evIndex: Int = 4,
    val focusPeakingEnabled: Boolean = false,
    val wbKelvin: Int = 5500
) {
    val isIsoManual: Boolean get() = iso > 0
    val isShutterManual: Boolean get() = shutterSpeedNs > 0L
    val isFocusManual: Boolean get() = focusDistance >= 0f
    val isWbManual: Boolean get() = awbMode != CameraMetadata.CONTROL_AWB_MODE_AUTO
}

/**
 * Cinematic Video Bokeh Simulation (ML Kit AI segmentation).
 */
enum class CinematicAperture(
    val label: String,
    val fNumber: Float,
    val blurRadiusDp: Float,
    val description: String
) {
    F1_4("f/1.4", 1.4f, 28f, "Dreamy Bokeh • Kedalaman lensa prima"),
    F2_0("f/2.0", 2.0f, 20f, "Portrait Cinema • Pemisahan subjek halus"),
    F2_8("f/2.8", 2.8f, 13f, "Classic Movie • Kedalaman seimbang"),
    F4_0("f/4.0", 4.0f, 7f, "Subtle Cinema • Latar belakang lembut"),
    F8_0("f/8.0", 8.0f, 2f, "Deep Cinema • Bidang fokus lebar")
}

enum class CinematicStyle(
    val displayName: String,
    val filterColorHex: Long = 0x00000000,
    val description: String = ""
) {
    GAUSSIAN("Creamy Bokeh", 0x00000000, "Lensa 50mm bioskop alami"),
    ANAMORPHIC("Anamorphic Blue", 0x1A0284C7, "Gaya bioskop Hollywood dengan garis anamorphic"),
    SPOTLIGHT("Studio Focus", 0x40000000, "Latar belakang menggelap fokus pada subjek"),
    WARM_GOLD("Golden Cinema", 0x20F59E0B, "Tona hangat film cinema 35mm")
}

data class CinematicBokehState(
    val isEnabled: Boolean = true,
    val aperture: CinematicAperture = CinematicAperture.F1_4,
    val style: CinematicStyle = CinematicStyle.GAUSSIAN,
    val isWidescreen239Enabled: Boolean = true,
    val isSubjectDetected: Boolean = false,
    val subjectConfidence: Float = 0f,
    val subjectBounds: RectF? = null
)

/**
 * Samsung Galaxy A15 Engine Settings.
 */
data class SamsungA15EngineSettings(
    val forceCamera2Api: Boolean = true,
    val force60FpsLock: Boolean = true,
    val force120FpsHighSpeed: Boolean = false,
    val antiDropShutterLimit: Boolean = true,
    val videoStabilization: Boolean = true,
    val highBitrateMode: Boolean = true,
    val showDiagnosticHud: Boolean = true,
    val showGridLines: Boolean = true,
    val watermarkEnabled: Boolean = false,
    val sceneOptimizer: Boolean = true,
    val h265HevcEnabled: Boolean = false
)

/**
 * Hardware diagnostic specs.
 */
data class CameraHardwareDetails(
    val sensorName: String = "Samsung Galaxy A15 (SM-A155F)",
    val hardwareLevel: String = "LEVEL_3 / FULL (Forced Camera2)",
    val isCamera2ApiEnabled: Boolean = true,
    val is60FpsSupported: Boolean = true,
    val is120FpsSupported: Boolean = true,
    val availableFpsRanges: List<Range<Int>> = emptyList(),
    val highSpeedFpsRanges: List<Range<Int>> = emptyList(),
    val activeFpsRange: Range<Int>? = null,
    val isContinuousAfSupported: Boolean = true,
    val isEisSupported: Boolean = true,
    val isOisSupported: Boolean = false,
    val isStabilizationActive: Boolean = true,
    val availableAfModes: List<String> = emptyList(),
    val activeAfMode: String = "CONTINUOUS_VIDEO / PICTURE",
    val isSamsungDevice: Boolean = true,
    val deviceModel: String = "Samsung Galaxy A15 (SM-A155F)",
    val chipset: String = "MediaTek Helio G99 (6nm)",
    val ultraWideSensor: String = "5 MP f/2.2 120° FOV",
    val selfieSensor: String = "13 MP f/2.0",
    val activeCameraId: String = "0 (Belakang - Utama)",
    val maxDigitalZoom: Float = 10.0f
)

/**
 * Item captured in MediaStore.
 */
data class CapturedMediaItem(
    val uri: Uri,
    val isVideo: Boolean,
    val displayName: String,
    val dateAddedMillis: Long,
    val relativePath: String = "DCIM/Camera",
    val durationMs: Long = 0L,
    val formattedSize: String = "",
    val fpsTag: String = "",
    val resolutionTag: String = ""
)
