package org.warifu.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import org.warifu.security.SecurityHardening
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Isolated, lifecycle-safe QR code scanner activity adhering to Aegis Authenticator
 * and Google Authenticator industry standards.
 *
 * Uses native View hierarchy (PreviewView) to prevent any Jetpack Compose View-interop
 * lifecycle races, with deterministic unbindAll() and executor shutdown in onDestroy().
 */
class ScannerActivity : ComponentActivity() {

    companion object {
        const val TAG = "ScannerActivity"
        const val EXTRA_SCAN_RESULT = "extra_scan_result"
        const val EXTRA_MANUAL_INPUT = "extra_manual_input"
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService? = null
    private var cameraControl: CameraControl? = null
    private var isTorchOn = false
    private val hasDetectedCode = AtomicBoolean(false)

    private lateinit var previewView: PreviewView
    private lateinit var torchButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // 1. Block screenshots and OS task preview caching
            SecurityHardening.enableFlagSecure(this)

            // 2. Pre-flight permission check
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission required to scan QR codes", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            cameraExecutor = Executors.newSingleThreadExecutor()

            // 3. Root layout container
            val rootLayout = FrameLayout(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.BLACK)
            }

            // 4. Camera preview view
            previewView = PreviewView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            rootLayout.addView(previewView)

            val density = resources.displayMetrics.density
            val reticleSizePx = (260 * density).toInt()
            val reticleCornerPx = (16 * density)
            val reticleBorderPx = (2.5f * density).toInt()

            // 5. Viewfinder reticle overlay
            val reticleView = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(reticleSizePx, reticleSizePx).apply {
                    gravity = Gravity.CENTER
                }
                background = GradientDrawable().apply {
                    setColor(Color.TRANSPARENT)
                    setStroke(reticleBorderPx, Color.WHITE)
                    cornerRadius = reticleCornerPx
                }
            }
            rootLayout.addView(reticleView)

            // 6. Guidance text
            val instructionText = TextView(this).apply {
                text = "Align QR code within the frame"
                setTextColor(Color.parseColor("#FAFAFA"))
                textSize = 14f
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                    topMargin = (reticleSizePx / 2) + (36 * density).toInt()
                    leftMargin = (32 * density).toInt()
                    rightMargin = (32 * density).toInt()
                }
            }
            rootLayout.addView(instructionText)

            // 7. Top action bar
            val topBar = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.TOP
                    topMargin = (40 * density).toInt()
                    leftMargin = (16 * density).toInt()
                    rightMargin = (16 * density).toInt()
                }
            }

            val buttonBg = {
                GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#80000000"))
                }
            }

            val closeButton = ImageButton(this).apply {
                layoutParams = LinearLayout.LayoutParams((48 * density).toInt(), (48 * density).toInt())
                background = buttonBg()
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(Color.WHITE)
                contentDescription = "Close"
                setOnClickListener { finish() }
            }
            topBar.addView(closeButton)

            val spacer = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, 1, 1.0f)
            }
            topBar.addView(spacer)

            torchButton = ImageButton(this).apply {
                layoutParams = LinearLayout.LayoutParams((48 * density).toInt(), (48 * density).toInt())
                background = buttonBg()
                setImageResource(android.R.drawable.ic_lock_power_off)
                setColorFilter(Color.WHITE)
                contentDescription = "Flashlight"
                setOnClickListener { toggleTorch() }
            }
            topBar.addView(torchButton)

            rootLayout.addView(topBar)

            // 8. Manual entry button
            val manualButton = Button(this).apply {
                text = "Enter Key Manually"
                setTextColor(Color.WHITE)
                textSize = 14f
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#B3000000"))
                    cornerRadius = (24 * density)
                }
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    (48 * density).toInt()
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    bottomMargin = (40 * density).toInt()
                }
                setPadding((24 * density).toInt(), 0, (24 * density).toInt(), 0)
                setOnClickListener {
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_MANUAL_INPUT, true)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }
            rootLayout.addView(manualButton)

            setContentView(rootLayout)

            // 9. Initialize and bind CameraX
            startCamera()
        } catch (t: Throwable) {
            Log.e(TAG, "Fatal in ScannerActivity onCreate", t)
            Toast.makeText(this, "Failed to start scanner: ${t.localizedMessage ?: t.javaClass.simpleName}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun toggleTorch() {
        try {
            isTorchOn = !isTorchOn
            cameraControl?.enableTorch(isTorchOn)
            torchButton.setColorFilter(if (isTorchOn) Color.parseColor("#FFD54F") else Color.WHITE)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to toggle torch", e)
        }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                if (isFinishing || isDestroyed) return@addListener
                val provider = future.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val executor = cameraExecutor ?: return@addListener
                imageAnalysis.setAnalyzer(executor, QrCodeAnalyzer { scannedResult ->
                    if (!hasDetectedCode.getAndSet(true)) {
                        runOnUiThread {
                            val resultIntent = Intent().apply {
                                putExtra(EXTRA_SCAN_RESULT, scannedResult)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    }
                })

                val selector = when {
                    provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> null
                }

                if (selector == null) {
                    Toast.makeText(this, "No camera found on this device", Toast.LENGTH_LONG).show()
                    finish()
                    return@addListener
                }

                provider.unbindAll()
                val camera = provider.bindToLifecycle(this, selector, preview, imageAnalysis)
                cameraControl = camera.cameraControl
            } catch (t: Throwable) {
                Log.e(TAG, "Camera initialization failed", t)
                Toast.makeText(
                    this,
                    "Camera unavailable: ${t.localizedMessage ?: "Device error"}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        try {
            cameraProvider?.unbindAll()
        } catch (t: Throwable) {
            Log.w(TAG, "Error unbinding CameraX use cases", t)
        }
        try {
            cameraExecutor?.shutdownNow()
        } catch (t: Throwable) {
            Log.w(TAG, "Error terminating camera executor", t)
        }
        cameraProvider = null
        cameraExecutor = null
        cameraControl = null
        super.onDestroy()
    }
}

/**
 * High-performance, orientation-aware, and crash-safe offline QR code analyzer using ZXing Core.
 */
private class QrCodeAnalyzer(
    private val onQrCodeFound: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = mapOf<DecodeHintType, Any>(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to java.lang.Boolean.TRUE,
            DecodeHintType.ALSO_INVERTED to java.lang.Boolean.TRUE,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )
        setHints(hints)
    }

    private val isScanningActive = AtomicBoolean(true)

    override fun analyze(image: ImageProxy) {
        if (!isScanningActive.get()) {
            image.close()
            return
        }

        try {
            val plane = image.planes.getOrNull(0) ?: run {
                image.close()
                return
            }

            val buf = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val width = image.width
            val height = image.height

            // Guarantee buffer allocation of exact size width * height
            val yData = if (rowStride == width && pixelStride == 1) {
                val data = ByteArray(width * height)
                buf.position(0)
                buf.get(data, 0, minOf(buf.remaining(), width * height))
                data
            } else {
                val data = ByteArray(width * height)
                val rowBuffer = ByteArray(rowStride)
                val initialPos = buf.position()
                for (row in 0 until height) {
                    val pos = initialPos + row * rowStride
                    if (pos < buf.limit()) {
                        buf.position(pos)
                        val bytesToRead = minOf(rowStride, buf.remaining())
                        buf.get(rowBuffer, 0, bytesToRead)
                        for (col in 0 until width) {
                            val pixelIndex = col * pixelStride
                            if (pixelIndex < bytesToRead) {
                                data[row * width + col] = rowBuffer[pixelIndex]
                            }
                        }
                    }
                }
                data
            }

            // 1. Direct decode using PlanarYUVLuminanceSource
            val source = PlanarYUVLuminanceSource(
                yData,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )

            var result = try {
                reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            } catch (_: NotFoundException) {
                null
            }

            // 2. Sensor orientation fallback (e.g., 90, 180, 270)
            val rotationDegrees = image.imageInfo.rotationDegrees
            if (result == null && rotationDegrees != 0) {
                val rotated = rotateLuminance(yData, width, height, rotationDegrees)
                val rotatedSource = PlanarYUVLuminanceSource(
                    rotated.data,
                    rotated.width,
                    rotated.height,
                    0,
                    0,
                    rotated.width,
                    rotated.height,
                    false
                )
                result = try {
                    reader.decodeWithState(BinaryBitmap(HybridBinarizer(rotatedSource)))
                } catch (_: NotFoundException) {
                    null
                }
            }

            if (result != null && !result.text.isNullOrBlank()) {
                if (isScanningActive.compareAndSet(true, false)) {
                    onQrCodeFound(result.text)
                }
            }
        } catch (_: Throwable) {
            // Defensive: ignore frame decode noise on blurred frames
        } finally {
            try {
                reader.reset()
            } catch (_: Throwable) {}
            try {
                image.close()
            } catch (_: Throwable) {}
        }
    }

    private data class RotatedFrame(val data: ByteArray, val width: Int, val height: Int)

    private fun rotateLuminance(
        data: ByteArray,
        width: Int,
        height: Int,
        rotationDegrees: Int
    ): RotatedFrame {
        return when (rotationDegrees) {
            90 -> {
                val rotated = ByteArray(data.size)
                var i = 0
                for (x in 0 until width) {
                    for (y in height - 1 downTo 0) {
                        rotated[i++] = data[y * width + x]
                    }
                }
                RotatedFrame(rotated, height, width)
            }
            180 -> {
                val rotated = ByteArray(data.size)
                var i = 0
                for (pos in (width * height - 1) downTo 0) {
                    rotated[i++] = data[pos]
                }
                RotatedFrame(rotated, width, height)
            }
            270 -> {
                val rotated = ByteArray(data.size)
                var i = 0
                for (x in width - 1 downTo 0) {
                    for (y in 0 until height) {
                        rotated[i++] = data[y * width + x]
                    }
                }
                RotatedFrame(rotated, height, width)
            }
            else -> RotatedFrame(data, width, height)
        }
    }
}
