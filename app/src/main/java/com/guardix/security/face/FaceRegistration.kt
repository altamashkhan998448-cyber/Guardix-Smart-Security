package com.guardix.security.face

import android.Manifest
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
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
    private lateinit var progressText: TextView
    private lateinit var progressRing: CircularProgressView

    private var cameraManager: FaceCameraManager? = null
    private var completed = false
    private var stopped = false

    private val isSaving = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val registrationExecutor = Executors.newSingleThreadExecutor()

    fun create(): View {

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(Color.rgb(8, 13, 28))
        }

        val title = TextView(context).apply {
            text = "FACE REGISTRATION"
            textSize = 23f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val description = TextView(context).apply {
            text = "Keep your face inside the circle"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        root.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(45)
            )
        )

        root.addView(
            description,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(35)
            )
        )

        val cameraFrame = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                dp(300),
                dp(300)
            ).apply {
                gravity = Gravity.CENTER
                topMargin = dp(15)
                bottomMargin = dp(15)
            }
        }

        progressRing = CircularProgressView(context)

        cameraFrame.addView(
            progressRing,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        previewView = PreviewView(context).apply {

            scaleType = PreviewView.ScaleType.FILL_CENTER

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.BLACK)
            }

            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND
        }

        val previewParams = FrameLayout.LayoutParams(
            dp(250),
            dp(250),
            Gravity.CENTER
        )

        cameraFrame.addView(previewView, previewParams)

        root.addView(cameraFrame)

        progressText = TextView(context).apply {
            text = "0%"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
        }

        root.addView(
            progressText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(40)
            )
        )

        statusText = TextView(context).apply {
            text = "Position your face inside the circle"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        root.addView(
            statusText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(55)
            )
        )

        val instructions = TextView(context).apply {
            text = """
                1. Blink your eyes once
                2. Blink your eyes again
                3. Turn your head LEFT
                4. Turn your head RIGHT

                Face registration will complete automatically.
            """.trimIndent()

            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        root.addView(
            instructions,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(150)
            )
        )

        cameraManager = FaceCameraManager(
            context,
            lifecycleOwner,
            previewView,
            object : FaceCameraManager.FaceListener {

                override fun onFaceDetected() {
                    mainHandler.post {
                        if (!completed && !stopped) {
                            statusText.text = "Face detected. Follow the instructions."
                        }
                    }
                }

                override fun onFaceNotDetected() {
                    mainHandler.post {
                        if (!completed && !stopped) {
                            statusText.text = "No face detected. Look at the camera."
                        }
                    }
                }

                override fun onStatusChanged(status: String) {
                    mainHandler.post {
                        if (!completed && !stopped) {
                            statusText.text = status
                        }
                    }
                }

                override fun onProgressChanged(
                    progress: Int,
                    message: String
                ) {
                    mainHandler.post {
                        if (!completed && !stopped) {
                            progressRing.setProgressValue(progress)
                            progressText.text = "$progress%"
                            statusText.text = message
                        }
                    }
                }

                override fun onLivenessCompleted() {
                    mainHandler.post {
                        if (!completed && !stopped) {
                            statusText.text =
                                "Face detected. Preparing registration..."
                        }
                    }
                }

                override fun onLivenessCompleted(faceBitmap: android.graphics.Bitmap) {
                    if (!isSaving.compareAndSet(false, true)) {
                        faceBitmap.recycle()
                        return
                    }

                    cameraManager?.stopCamera()

                    mainHandler.post {
                        progressRing.setProgressValue(100)
                        progressText.text = "100%"
                        statusText.text = "Saving your face identity..."
                    }

                    registrationExecutor.execute {

                        var success = false

                        try {
                            val embedding = FaceRecognitionEngine(context).use {
                                it.getFaceEmbedding(faceBitmap)
                            }

                            if (embedding != null && embedding.isNotEmpty()) {
                                success = FaceStorage(context)
                                    .saveFaceEmbedding(embedding)
                            }

                        } catch (e: Exception) {
                            success = false
                        } finally {
                            if (!faceBitmap.isRecycled) {
                                faceBitmap.recycle()
                            }
                        }

                        mainHandler.post {

                            if (stopped || completed) return@post

                            if (success) {

                                completed = true

                                progressRing.setProgressValue(100)
                                progressText.text = "100%"
                                statusText.text =
                                    "FACE REGISTERED SUCCESSFULLY"

                                Toast.makeText(
                                    context,
                                    "Face registration successful!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                mainHandler.postDelayed({
                                    if (!stopped) {
                                        onRegistrationComplete()
                                    }
                                }, 900)

                            } else {

                                isSaving.set(false)

                                statusText.text =
                                    "Face registration failed. Please try again."

                                Toast.makeText(
                                    context,
                                    "Unable to save face. Please restart registration.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }

                override fun onError(message: String) {
                    mainHandler.post {
                        if (!completed && !stopped) {
                            statusText.text = message

                            Toast.makeText(
                                context,
                                message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        )

        return root
    }

    fun start() {

        if (stopped || completed) return

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                context,
                "Camera permission is required.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        cameraManager?.startCamera()
    }

    fun stop() {

        if (stopped) return

        stopped = true

        mainHandler.removeCallbacksAndMessages(null)

        cameraManager?.shutdown()
        cameraManager = null

        registrationExecutor.shutdownNow()
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }
}

private class CircularProgressView(context: Context) : View(context) {

    private val density = context.resources.displayMetrics.density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 9f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 9f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val circleRect = RectF()

    private var currentProgress = 0f
    private var animator: ValueAnimator? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val inset = 15f * density

        circleRect.set(
            inset,
            inset,
            width - inset,
            height - inset
        )

        canvas.drawArc(
            circleRect,
            0f,
            360f,
            false,
            trackPaint
        )

        canvas.drawArc(
            circleRect,
            -90f,
            360f * currentProgress / 100f,
            false,
            progressPaint
        )
    }

    fun setProgressValue(progress: Int) {

        animator?.cancel()

        animator = ValueAnimator.ofFloat(
            currentProgress,
            progress.coerceIn(0, 100).toFloat()
        ).apply {

            duration = 450

            addUpdateListener {
                currentProgress = it.animatedValue as Float
                invalidate()
            }

            start()
        }
    }
}