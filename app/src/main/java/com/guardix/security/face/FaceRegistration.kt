package com.guardix.security.face

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class FaceRegistration(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onRegistrationComplete: () -> Unit
) {

    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private lateinit var cameraManager: FaceCameraManager

    private val mainHandler = Handler(Looper.getMainLooper())

    private val registrationExecutor =
        Executors.newSingleThreadExecutor()

    private val isProcessing = AtomicBoolean(false)

    @Volatile
    private var completed = false

    @Volatile
    private var stopped = false

    fun create(): View {

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(20, 25, 20, 25)
            setBackgroundColor(Color.rgb(10, 16, 35))
        }

        val title = TextView(context).apply {
            text = "FACE REGISTRATION"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 15)
        }

        root.addView(title)

        val description = TextView(context).apply {
            text = "Complete the following face verification steps."
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        root.addView(description)

        previewView = PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }

        root.addView(
            previewView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                320
            )
        )

        statusText = TextView(context).apply {
            text = "Camera is waiting to start."
            textSize = 17f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
            setPadding(10, 25, 10, 25)
        }

        root.addView(statusText)

        val steps = TextView(context).apply {
            text = """
                STEP 1: Blink your eyes twice
                STEP 2: Turn your head left
                STEP 3: Turn your head right
            """.trimIndent()

            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setLineSpacing(8f, 1f)
        }

        root.addView(steps)

        cameraManager = FaceCameraManager(
            context,
            lifecycleOwner,
            previewView,
            object : FaceCameraManager.FaceListener {

                override fun onFaceDetected() {
                    updateStatus("Face detected.")
                }

                override fun onFaceNotDetected() {
                    updateStatus("Please show your face.")
                }

                override fun onStatusChanged(status: String) {
                    updateStatus(status)
                }

                // पुराने callback से Registration पूरा नहीं होगा।
                override fun onLivenessCompleted() {
                    updateStatus(
                        "Face image is required. Please try again."
                    )
                }

                // नया callback: कैमरे से मिली वास्तविक Face Bitmap।
                override fun onLivenessCompleted(
                    faceBitmap: Bitmap
                ) {
                    processFaceRegistration(faceBitmap)
                }

                override fun onError(message: String) {
                    updateStatus(message)
                }
            }
        )

        return root
    }

    private fun processFaceRegistration(faceBitmap: Bitmap) {

        if (completed || stopped) {
            faceBitmap.recycle()
            return
        }

        if (!isProcessing.compareAndSet(false, true)) {
            faceBitmap.recycle()
            return
        }

        updateStatus("Creating your face identity...")

        try {

            registrationExecutor.execute {

                var success = false
                var errorMessage = "Face registration failed."

                try {

                    // Generate face embedding.
                    val embedding =
                        FaceRecognitionEngine(context).use { engine ->
                            engine.getFaceEmbedding(faceBitmap)
                        }

                    // Save encrypted embedding locally.
                    val faceStorage = FaceStorage(context)

                    success = faceStorage.saveFaceEmbedding(
                        embedding
                    )

                    if (!success) {
                        errorMessage =
                            "Unable to securely save face data."
                    }

                } catch (e: Exception) {

                    errorMessage =
                        e.localizedMessage
                            ?: "Face processing failed."
                } finally {

                    if (!faceBitmap.isRecycled) {
                        faceBitmap.recycle()
                    }
                }

                mainHandler.post {

                    isProcessing.set(false)

                    if (stopped || completed) return@post

                    if (success) {

                        completed = true

                        statusText.text =
                            "Face registered successfully!"

                        Toast.makeText(
                            context,
                            "Face registration successful",
                            Toast.LENGTH_LONG
                        ).show()

                        onRegistrationComplete()

                    } else {

                        statusText.text = errorMessage

                        Toast.makeText(
                            context,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

        } catch (e: Exception) {

            isProcessing.set(false)

            if (!faceBitmap.isRecycled) {
                faceBitmap.recycle()
            }

            updateStatus(
                e.localizedMessage ?: "Registration could not start."
            )
        }
    }

    private fun updateStatus(message: String) {

        mainHandler.post {
            if (!stopped && ::statusText.isInitialized) {
                statusText.text = message
            }
        }
    }

    fun start() {

        if (stopped || completed) return

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            updateStatus("Camera permission is required.")
            return
        }

        cameraManager.startCamera()
    }

    fun stop() {

        if (stopped) return

        stopped = true

        if (::cameraManager.isInitialized) {
            cameraManager.shutdown()
        }

        registrationExecutor.shutdownNow()
    }
}