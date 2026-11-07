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
import java.nio.ByteBuffer
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

        // पुराने callback को भी support करता है।
        fun onLivenessCompleted(faceBitmap: Bitmap) {
            onLivenessCompleted()
        }

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

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({

            if (isShutdown) return@addListener

            try {

                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                val imageAnalysis =
                    ImageAnalysis.Builder()
                        .setBackpressureStrategy(
                            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                        )
                        .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->

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

                        val inputImage = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )

                        faceDetector.process(inputImage)
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
                    imageAnalysis
                )

                listener.onStatusChanged(
                    "Camera started. Look at the camera."
                )

            } catch (e: Exception) {

                isStarted = false

                listener.onError(
                    e.localizedMessage
                        ?: "Unable to start camera"
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
                "No face detected. Please show your face."
            )

            return
        }

        // एक से अधिक चेहरे हों तो Registration रोकें।
        if (faces.size > 1) {
            listener.onStatusChanged(
                "Only one face should be visible."
            )
            return
        }

        val face = faces.firstOrNull() ?: return

        listener.onFaceDetected()

        when (currentStep) {

            Step.WAITING_FOR_FACE -> {

                currentStep = Step.WAITING_FOR_BLINK

                listener.onStatusChanged(
                    "Face detected. Please blink twice."
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

        val leftEye =
            face.leftEyeOpenProbability ?: return

        val rightEye =
            face.rightEyeOpenProbability ?: return

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

            if (blinkCount >= 2) {

                currentStep = Step.WAITING_FOR_LEFT

                listener.onStatusChanged(
                    "Two blinks detected. Turn your head left."
                )

            } else {

                listener.onStatusChanged(
                    "First blink detected. Blink one more time."
                )
            }
        }
    }

    private fun checkLeftTurn(face: Face) {

        if (face.headEulerAngleY < -15f) {

            currentStep = Step.WAITING_FOR_RIGHT

            listener.onStatusChanged(
                "Left turn detected. Now turn your head right."
            )
        }
    }

    private fun checkRightTurn(
        face: Face,
        imageProxy: ImageProxy
    ) {

        if (face.headEulerAngleY > 15f) {

            listener.onStatusChanged(
                "Capturing your face..."
            )

            val faceBitmap = createFaceBitmap(
                imageProxy,
                face.boundingBox
            )

            if (faceBitmap == null) {

                listener.onStatusChanged(
                    "Face capture failed. Please try again."
                )

                currentStep = Step.WAITING_FOR_FACE
                blinkCount = 0
                eyesClosed = false

                return
            }

            currentStep = Step.COMPLETED

            listener.onStatusChanged(
                "Face movement verification completed."
            )

            // अब Registration को चेहरे की Bitmap भी मिलेगी।
            listener.onLivenessCompleted(faceBitmap)
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

            val yBase = yBuffer.position()
            val uBase = uBuffer.position()
            val vBase = vBuffer.position()

            val nv21 = ByteArray(width * height * 3 / 2)

            // Y plane
            for (row in 0 until height) {
                for (col in 0 until width) {

                    val index =
                        yBase +
                            row * yPlane.rowStride +
                            col * yPlane.pixelStride

                    nv21[row * width + col] =
                        yBuffer.get(index)
                }
            }

            // Interleave V and U planes for NV21.
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

                    val outputIndex =
                        ySize + row * width + col * 2

                    nv21[outputIndex] = vBuffer.get(vIndex)
                    nv21[outputIndex + 1] = uBuffer.get(uIndex)
                }
            }

            val output = ByteArrayOutputStream()

            val converted = YuvImage(
                nv21,
                ImageFormat.NV21,
                width,
                height,
                null
            ).compressToJpeg(
                Rect(0, 0, width, height),
                90,
                output
            )

            if (!converted) return null

            val bytes = output.toByteArray()

            val originalBitmap =
                BitmapFactory.decodeByteArray(
                    bytes,
                    0,
                    bytes.size
                ) ?: return null

            val rotation =
                imageProxy.imageInfo.rotationDegrees

            val rotatedBitmap =
                if (rotation != 0) {

                    val matrix = Matrix().apply {
                        postRotate(rotation.toFloat())
                    }

                    Bitmap.createBitmap(
                        originalBitmap,
                        0,
                        0,
                        originalBitmap.width,
                        originalBitmap.height,
                        matrix,
                        true
                    )

                } else {
                    originalBitmap
                }

            // ML Kit bounding box is applied to the rotated image.
            val left = faceBox.left.coerceIn(
                0,
                rotatedBitmap.width - 1
            )

            val top = faceBox.top.coerceIn(
                0,
                rotatedBitmap.height - 1
            )

            val right = faceBox.right.coerceIn(
                left + 1,
                rotatedBitmap.width
            )

            val bottom = faceBox.bottom.coerceIn(
                top + 1,
                rotatedBitmap.height
            )

            val marginX =
                ((right - left) * 0.15f).toInt()

            val marginY =
                ((bottom - top) * 0.15f).toInt()

            val cropRect = Rect(
                (left - marginX).coerceAtLeast(0),
                (top - marginY).coerceAtLeast(0),
                (right + marginX).coerceAtMost(
                    rotatedBitmap.width
                ),
                (bottom + marginY).coerceAtMost(
                    rotatedBitmap.height
                )
            )

            if (
                cropRect.width() < 20 ||
                cropRect.height() < 20
            ) {
                return null
            }

            Bitmap.createBitmap(
                rotatedBitmap,
                cropRect.left,
                cropRect.top,
                cropRect.width(),
                cropRect.height()
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