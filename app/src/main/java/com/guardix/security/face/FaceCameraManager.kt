package com.guardix.security.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class FaceCameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val listener: FaceListener
) {

    interface FaceListener {
        fun onFaceDetected()
        fun onFaceNotDetected()
        fun onStatusChanged(status: String)
        fun onLivenessCompleted()

        fun onLivenessCompleted(faceBitmap: Bitmap) {
            onLivenessCompleted()
        }

        fun onProgressChanged(progress: Int, message: String) {}

        fun onError(message: String)
    }

    private enum class Step {
        WAITING_FOR_FACE,
        WAITING_FOR_BLINK,
        WAITING_FOR_LEFT,
        WAITING_FOR_RIGHT,
        COMPLETED
    }

    private var currentStep = Step.WAITING_FOR_FACE

    private var blinkCount = 0
    private var eyesClosed = false

    private val isProcessing = AtomicBoolean(false)

    @Volatile
    private var isStarted = false

    @Volatile
    private var isShutdown = false

    private var cameraProvider: ProcessCameraProvider? = null

    private val cameraExecutor: ExecutorService =
        Executors.newSingleThreadExecutor()

    private val detectorOptions =
        FaceDetectorOptions.Builder()
            .setPerformanceMode(
                FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
            )
            .setClassificationMode(
                FaceDetectorOptions.CLASSIFICATION_MODE_ALL
            )
            .enableTracking()
            .build()

    private val faceDetector: FaceDetector =
        FaceDetection.getClient(detectorOptions)

    @Synchronized
    fun startCamera() {

        if (isStarted || isShutdown) return

        isStarted = true

        val future = ProcessCameraProvider.getInstance(context)

        future.addListener({

            if (isShutdown) return@addListener

            try {

                val provider = future.get()
                cameraProvider = provider

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

                analysis.setAnalyzer(cameraExecutor) { imageProxy ->

                    if (
                        isShutdown ||
                        !isStarted ||
                        !isProcessing.compareAndSet(false, true)
                    ) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val mediaImage = imageProxy.image

                    if (mediaImage == null) {
                        isProcessing.set(false)
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    try {

                        val input = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        faceDetector.process(input)
                            .addOnSuccessListener { faces ->

                                if (!isShutdown && isStarted) {
                                    processFaces(faces, imageProxy)
                                }
                            }
                            .addOnFailureListener { error ->

                                if (!isShutdown) {
                                    listener.onError(
                                        error.localizedMessage
                                            ?: "Face detection failed"
                                    )
                                }
                            }
                            .addOnCompleteListener {
                                isProcessing.set(false)
                                imageProxy.close()
                            }

                    } catch (e: Exception) {

                        isProcessing.set(false)
                        imageProxy.close()

                        listener.onError(
                            e.localizedMessage
                                ?: "Image processing failed"
                        )
                    }
                }

                provider.unbindAll()

                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    analysis
                )

                listener.onStatusChanged(
                    "Camera started. Look at the camera."
                )

            } catch (e: Exception) {

                isStarted = false

                listener.onError(
                    e.localizedMessage ?: "Unable to start camera"
                )
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private fun processFaces(
        faces: List<Face>,
        imageProxy: ImageProxy
    ) {

        if (faces.isEmpty()) {

            eyesClosed = false

            listener.onFaceNotDetected()
            listener.onStatusChanged(
                "Please show your face."
            )

            return
        }

        if (faces.size > 1) {
            listener.onStatusChanged(
                "Only one face should be visible."
            )
            return
        }

        val face = faces.first()

        listener.onFaceDetected()

        when (currentStep) {

            Step.WAITING_FOR_FACE -> {

                currentStep = Step.WAITING_FOR_BLINK

                listener.onStatusChanged(
                    "Face detected. Blink your eyes."
                )
            }

            Step.WAITING_FOR_BLINK -> {
                checkBlink(face)
            }

            Step.WAITING_FOR_LEFT -> {
                checkLeftTurn(face)
            }

            Step.WAITING_FOR_RIGHT -> {
                checkRightTurn(face, imageProxy)
            }

            Step.COMPLETED -> Unit
        }
    }

    private fun checkBlink(face: Face) {

        val leftEye = face.leftEyeOpenProbability ?: return
        val rightEye = face.rightEyeOpenProbability ?: return

        if (leftEye < 0.30f && rightEye < 0.30f) {
            eyesClosed = true
        }

        if (
            eyesClosed &&
            leftEye > 0.75f &&
            rightEye > 0.75f
        ) {

            eyesClosed = false
            blinkCount++

            val progress = blinkCount.coerceAtMost(2) * 25

            if (blinkCount == 1) {

                listener.onProgressChanged(
                    25,
                    "First blink completed. Blink again."
                )

            } else {

                currentStep = Step.WAITING_FOR_LEFT

                listener.onProgressChanged(
                    50,
                    "Two blinks completed. Turn your head left."
                )
            }
        }
    }

    private fun checkLeftTurn(face: Face) {

        if (face.headEulerAngleY < -15f) {

            currentStep = Step.WAITING_FOR_RIGHT

            listener.onProgressChanged(
                75,
                "Left turn completed. Turn your head right."
            )
        }
    }

    private fun checkRightTurn(
        face: Face,
        imageProxy: ImageProxy
    ) {

        if (face.headEulerAngleY > 15f) {

            listener.onStatusChanged(
                "Capturing face..."
            )

            val bitmap = createFaceBitmap(
                imageProxy,
                face.boundingBox
            )

            if (bitmap == null) {

                currentStep = Step.WAITING_FOR_FACE
                blinkCount = 0
                eyesClosed = false

                listener.onProgressChanged(
                    0,
                    "Capture failed. Please try again."
                )

                return
            }

            currentStep = Step.COMPLETED

            listener.onProgressChanged(
                100,
                "Face movement completed."
            )

            listener.onLivenessCompleted(bitmap)
        }
    }

    private fun createFaceBitmap(
        imageProxy: ImageProxy,
        faceBox: Rect
    ): Bitmap? {

        return try {

            val width = imageProxy.width
            val height = imageProxy.height

            if (
                imageProxy.format != ImageFormat.YUV_420_888 ||
                width % 2 != 0 ||
                height % 2 != 0
            ) {
                return null
            }

            val planes = imageProxy.planes
            if (planes.size < 3) return null

            val yPlane = planes[0]
            val uPlane = planes[1]
            val vPlane = planes[2]

            val yBuffer = yPlane.buffer.duplicate()
            val uBuffer = uPlane.buffer.duplicate()
            val vBuffer = vPlane.buffer.duplicate()

            val nv21 = ByteArray(width * height * 3 / 2)

            val yBase = yBuffer.position()
            val uBase = uBuffer.position()
            val vBase = vBuffer.position()

            for (row in 0 until height) {
                for (col in 0 until width) {

                    nv21[row * width + col] =
                        yBuffer.get(
                            yBase +
                                row * yPlane.rowStride +
                                col * yPlane.pixelStride
                        )
                }
            }

            val ySize = width * height

            for (row in 0 until height / 2) {
                for (col in 0 until width / 2) {

                    val vIndex =
                        vBase +
                            row * vPlane.rowStride +
                            col * vPlane.pixelStride

                    val uIndex =
                        uBase +
                            row * uPlane.rowStride +
                            col * uPlane.pixelStride

                    val index =
                        ySize + row * width + col * 2

                    nv21[index] = vBuffer.get(vIndex)
                    nv21[index + 1] = uBuffer.get(uIndex)
                }
            }

            val stream = ByteArrayOutputStream()

            val converted = YuvImage(
                nv21,
                ImageFormat.NV21,
                width,
                height,
                null
            ).compressToJpeg(
                Rect(0, 0, width, height),
                90,
                stream
            )

            if (!converted) return null

            val bytes = stream.toByteArray()

            val original = BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size
            ) ?: return null

            val rotation =
                imageProxy.imageInfo.rotationDegrees

            val rotated = if (rotation != 0) {

                Bitmap.createBitmap(
                    original,
                    0,
                    0,
                    original.width,
                    original.height,
                    Matrix().apply {
                        postRotate(rotation.toFloat())
                    },
                    true
                )

            } else {
                original
            }

            val left = faceBox.left.coerceIn(
                0,
                rotated.width - 1
            )

            val top = faceBox.top.coerceIn(
                0,
                rotated.height - 1
            )

            val right = faceBox.right.coerceIn(
                left + 1,
                rotated.width
            )

            val bottom = faceBox.bottom.coerceIn(
                top + 1,
                rotated.height
            )

            val marginX =
                ((right - left) * 0.15f).toInt()

            val marginY =
                ((bottom - top) * 0.15f).toInt()

            val crop = Rect(
                (left - marginX).coerceAtLeast(0),
                (top - marginY).coerceAtLeast(0),
                (right + marginX).coerceAtMost(rotated.width),
                (bottom + marginY).coerceAtMost(rotated.height)
            )

            if (
                crop.width() < 20 ||
                crop.height() < 20
            ) {
                return null
            }

            Bitmap.createBitmap(
                rotated,
                crop.left,
                crop.top,
                crop.width(),
                crop.height()
            )

        } catch (_: Exception) {
            null
        }
    }

    @Synchronized
    fun stopCamera() {

        isStarted = false

        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {
        }
    }

    @Synchronized
    fun shutdown() {

        if (isShutdown) return

        stopCamera()
        isShutdown = true

        try {
            faceDetector.close()
        } catch (_: Exception) {
        }

        cameraExecutor.shutdown()
    }
}