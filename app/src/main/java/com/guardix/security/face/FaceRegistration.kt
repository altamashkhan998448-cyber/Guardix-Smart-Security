package com.guardix.security.face

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

class FaceRegistration(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onRegistrationComplete: () -> Unit
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
            text = "FACE REGISTRATION"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 15)
        }

        root.addView(title)

        val description = TextView(context).apply {
            text = "Complete the following face movement checks."
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
                STEP 1: Blink your eyes
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
                        "Face movement checks completed!"

                    Toast.makeText(
                        context,
                        "Face checks completed successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                    onRegistrationComplete()
                }

                override fun onError(message: String) {
                    statusText.text = message
                }
            }
        )

        return root
    }

    fun start() {

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