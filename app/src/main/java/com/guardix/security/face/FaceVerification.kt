package com.guardix.security.face

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.FrameLayout
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
    private lateinit var progressText: TextView
    private lateinit var continueButton: Button
    private lateinit var cameraManager: FaceCameraManager

    private var completed = false
    private var stopped = false

    fun create(): View {

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(Color.rgb(10, 16, 35))
        }

        val title = TextView(context).apply {
            text = "FACE VERIFICATION"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        root.addView(title)

        val description = TextView(context).apply {
            text = "Verify your face to continue to Guardix."
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        root.addView(description)

        previewView = PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.BLACK)
            }

            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
        }

        val cameraFrame = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                dp(280),
                dp(280)
            ).apply {
                gravity = Gravity.CENTER
                topMargin = dp(15)
                bottomMargin = dp(15)
            }
        }

        cameraFrame.addView(
            previewView,
            FrameLayout.LayoutParams(
                dp(260),
                dp(260),
                Gravity.CENTER
            )
        )

        root.addView(cameraFrame)

        progressText = TextView(context).apply {
            text = "0%"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
        }

        root.addView(progressText)

        statusText = TextView(context).apply {
            text = "Preparing face verification..."
            textSize = 16f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(15), dp(10), dp(15))
        }

        root.addView(statusText)

        val instructions = TextView(context).apply {
            text = """
                1. Blink your eyes twice.
                2. Turn your head LEFT.
                3. Turn your head RIGHT.

                Complete all checks to verify your face.
            """.trimIndent()

            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        root.addView(instructions)

        continueButton = Button(context).apply {
            text = "Continue"
            textSize = 17f
            isAllCaps = false
            visibility = View.GONE
            isEnabled = false

            background = GradientDrawable().apply {
                setColor(Color.rgb(0, 190, 220))
                cornerRadius = dp(14).toFloat()
            }

            setTextColor(Color.WHITE)

            setOnClickListener {
                if (completed && !stopped) {
                    stopped = true
                    cameraManager.shutdown()
                    onVerificationComplete()
                }
            }
        }

        root.addView(
            continueButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(55)
            ).apply {
                topMargin = dp(15)
            }
        )

        cameraManager = FaceCameraManager(
            context,
            lifecycleOwner,
            previewView,
            object : FaceCameraManager.FaceListener {

                override fun onFaceDetected() {
                    if (completed || stopped) return
                    statusText.text = "Face detected. Follow instructions."
                }

                override fun onFaceNotDetected() {
                    if (completed || stopped) return
                    statusText.text = "Please show your face."
                }

                override fun onStatusChanged(status: String) {
                    if (completed || stopped) return
                    statusText.text = status
                }

                override fun onProgressChanged(
                    progress: Int,
                    message: String
                ) {
                    if (completed || stopped) return

                    progressText.text =
                        "${progress.coerceIn(0, 100)}%"
                    statusText.text = message
                }

                override fun onLivenessCompleted() {
                    if (completed || stopped) return

                    completed = true

                    progressText.text = "100%"
                    statusText.text = "FACE VERIFIED SUCCESSFULLY"

                    continueButton.visibility = View.VISIBLE
                    continueButton.isEnabled = true

                    cameraManager.stopCamera()
                }

                override fun onLivenessCompleted(
                    faceBitmap: android.graphics.Bitmap
                ) {
                    faceBitmap.recycle()
                    onLivenessCompleted()
                }

                override fun onError(message: String) {
                    if (completed || stopped) return
                    statusText.text = message
                }
            }
        )

        return root
    }

    fun start() {
        if (completed || stopped) return

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            statusText.text = "Camera permission is required."
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
    }

    private fun dp(value: Int): Int {
        return (
            value * context.resources.displayMetrics.density
        ).toInt()
    }
}