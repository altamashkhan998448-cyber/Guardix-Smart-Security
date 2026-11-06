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
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
    private var eyesClosed = false
    private var isProcessing = false
    private var isStarted = false

    private val cameraExecutor: ExecutorService =
        Executors.newSingleThreadExecutor()

    private val detectorOptions =
        FaceDetectorOptions.Builder()
            .setPerformanceMode(
                FaceDetectorOptions.PERFORMANCE_MODE_FAST
            )
            .setClassificationMode(
                FaceDetectorOptions.CLASSIFICATION_MODE_ALL
            )
            .enableTracking()
            .build()

    private val faceDetector =
        FaceDetection.getClient(detectorOptions)

    fun startCamera() {

        if (isStarted) return

        isStarted = true

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({

            try {

                val cameraProvider =
                    cameraProviderFuture.get()

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

                    if (isProcessing) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val mediaImage =
                        imageProxy.image

                    if (mediaImage == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    isProcessing = true

                    val inputImage = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    faceDetector.process(inputImage)
                        .addOnSuccessListener { faces ->
                            processFaces(faces)
                        }
                        .addOnFailureListener {
                            listener.onError(
                                "Face detection failed"
                            )
                        }
                        .addOnCompleteListener {
                            isProcessing = false
                            imageProxy.close()
                        }
                }

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
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
                    e.localizedMessage ?: "Unable to start camera"
                )
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private fun processFaces(faces: List<Face>) {

        if (faces.isEmpty()) {
            listener.onFaceNotDetected()
            listener.onStatusChanged(
                "No face detected. Look at the camera."
            )
            return
        }

        listener.onFaceDetected()

        val face = faces.maxByOrNull {
            it.boundingBox.width() *
                    it.boundingBox.height()
        } ?: return

        when (currentStep) {

            Step.WAITING_FOR_FACE -> {

                currentStep = Step.WAITING_FOR_BLINK

                listener.onStatusChanged(
                    "Face detected. Please blink your eyes."
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

        if (leftEye < 0.35f && rightEye < 0.35f) {
            eyesClosed = true
        }

        if (
            eyesClosed &&
            leftEye > 0.75f &&
            rightEye > 0.75f
        ) {

            eyesClosed = false
            currentStep = Step.WAITING_FOR_LEFT

            listener.onStatusChanged(
                "Blink detected. Now turn your head left."
            )
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
                "Face movement checks completed."
            )

            listener.onLivenessCompleted()
        }
    }

    fun stopCamera() {

        isStarted = false

        try {
            val cameraProvider =
                ProcessCameraProvider.getInstance(context).get()

            cameraProvider.unbindAll()

        } catch (_: Exception) {
        }

        faceDetector.close()
    }

    fun shutdown() {
        stopCamera()
        cameraExecutor.shutdown()
    }
}