package com.guardix.security.face

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

class FaceVerification(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onVerificationComplete: () -> Unit
) {

    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private lateinit var cameraManager: FaceCameraManager

    private var completed = false

    fun create(): View {

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(20, 25, 20, 25)
            setBackgroundColor(Color.rgb(10, 16, 35))
        }

        val title = TextView(context).apply {
            text = "FACE VERIFICATION"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 20)
        }

        root.addView(title)

        val description = TextView(context).apply {
            text = "Verify your face to continue to Guardix."
            textSize = 16f
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
            text = "Preparing face verification..."
            textSize = 17f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
            setPadding(10, 25, 10, 25)
        }

        root.addView(statusText)

        val instructions = TextView(context).apply {
            text = """
                Please complete these checks:

                1. Look directly at the camera.
                2. Blink your eyes.
                3. Turn your head left.
                4. Turn your head right.
            """.trimIndent()

            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setLineSpacing(8f, 1f)
            setPadding(5, 10, 5, 15)
        }

        root.addView(instructions)

        cameraManager = FaceCameraManager(
            context,
            lifecycleOwner,
            previewView,
            object : FaceCameraManager.FaceListener {

                override fun onFaceDetected() {
                    statusText.text = "Face detected."
                }

                override fun onFaceNotDetected() {
                    statusText.text = "Please show your face."
                }

                override fun onStatusChanged(status: String) {
                    statusText.text = status
                }

                override fun onLivenessCompleted() {

                    if (completed) return

                    completed = true

                    statusText.text =
                        "Face movement checks completed."

                    onVerificationComplete()
                }

                override fun onError(message: String) {
                    statusText.text = message
                }
            }
        )

        return root
    }

    fun start() {

        if (completed) return

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            statusText.text =
                "Camera permission is required."

            return
        }

        cameraManager.startCamera()
    }

    fun stop() {

        if (::cameraManager.isInitialized) {
            cameraManager.shutdown()
        }
    }
}