package com.guardix.security.face

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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

    private var isStarted = false
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
                        it.surfaceProvider =
                            previewView.surfaceProvider
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

                        val inputImage =
                            InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )

                        faceDetector.process(inputImage)
                            .addOnSuccessListener { faces ->

                                if (!isShutdown && isStarted) {
                                    processFaces(faces)
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

    private fun processFaces(faces: List<Face>) {

        if (faces.isEmpty()) {

            eyesClosed = false

            listener.onFaceNotDetected()

            listener.onStatusChanged(
                "No face detected. Please show your face."
            )

            return
        }

        val face = faces.maxByOrNull {
            it.boundingBox.width() *
                it.boundingBox.height()
        } ?: return

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
                checkRightTurn(face)
            }

            Step.COMPLETED -> Unit
        }
    }

    private fun checkBlink(face: Face) {

        val leftEye =
            face.leftEyeOpenProbability ?: return

        val rightEye =
            face.rightEyeOpenProbability ?: return

        if (
            leftEye < 0.30f &&
            rightEye < 0.30f
        ) {
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

    private fun checkRightTurn(face: Face) {

        if (face.headEulerAngleY > 15f) {

            currentStep = Step.COMPLETED

            listener.onStatusChanged(
                "Liveness verification completed."
            )

            listener.onLivenessCompleted()
        }
    }

    @Synchronized
    fun stopCamera() {

        isStarted = false

        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {
        }

        isProcessing.set(false)
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