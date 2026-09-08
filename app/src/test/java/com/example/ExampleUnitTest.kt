package com.example

import android.util.Range
import com.example.camera.FpsMode
import com.example.camera.ResolutionMode
import com.example.camera.SamsungCameraHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testResolveOptimalFpsRange_exact60Supported() {
        val supportedRanges = listOf(
            Range(15, 30),
            Range(30, 30),
            Range(30, 60),
            Range(60, 60)
        )
        val result = SamsungCameraHelper.resolveOptimalFpsRange(supportedRanges, FpsMode.FPS_60)
        assertNotNull(result)
        assertEquals(Range(60, 60), result)
    }

    @Test
    fun testResolveOptimalFpsRange_dynamic60Supported() {
        // Typical Samsung Galaxy device without exact [60,60] but with [30,60]
        val supportedRanges = listOf(
            Range(15, 30),
            Range(30, 30),
            Range(30, 60)
        )
        val result = SamsungCameraHelper.resolveOptimalFpsRange(supportedRanges, FpsMode.FPS_60)
        assertNotNull(result)
        assertEquals(Range(30, 60), result)
    }

    @Test
    fun testResolveOptimalFpsRange_autoReturnsNull() {
        val supportedRanges = listOf(Range(30, 30), Range(30, 60))
        val result = SamsungCameraHelper.resolveOptimalFpsRange(supportedRanges, FpsMode.FPS_AUTO)
        assertNull(result)
    }

    @Test
    fun testResolutionModesAvailable() {
        val resolutions = ResolutionMode.values()
        assertTrue(resolutions.contains(ResolutionMode.RES_4K))
        assertTrue(resolutions.contains(ResolutionMode.RES_1080P))
        assertTrue(resolutions.contains(ResolutionMode.RES_720P))
        assertTrue(resolutions.contains(ResolutionMode.RES_480P))
    }
}
