package com.guardix.security

import android.Manifest
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.guardix.security.auth.SignInScreen
import com.guardix.security.auth.SignUpScreen
import com.guardix.security.auth.ForgotPasswordScreen
import com.guardix.security.dashboard.DashboardScreen
import com.guardix.security.face.FaceRegistration
import com.guardix.security.face.FaceVerification
import com.guardix.security.security.PinManager
import com.guardix.security.security.PinSetupScreen
import com.guardix.security.security.PinEntryScreen
import com.guardix.security.utils.AppPreferences
import com.guardix.security.welcome.WelcomeScreen

class MainActivity : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private var activeFaceStop: (() -> Unit)? = null
    private var pendingCameraStart: (() -> Unit)? = null

    private val cameraPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                pendingCameraStart?.invoke()
            } else {
                showCameraPermissionDialog()
            }

            pendingCameraStart = null
        }

    private val welcomeDelay = Runnable {
        checkAuthentication()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showWelcome()

        handler.postDelayed(welcomeDelay, 3000)
    }

    private fun showWelcome() {
        setContentView(WelcomeScreen.create(this))
    }

    private fun checkAuthentication() {

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            showSignIn()
            return
        }

        if (!PinManager.hasPin(this)) {
            showPinSetup()
        } else {
            showPinEntry()
        }
    }

    private fun showSignIn() {

        activeFaceStop?.invoke()
        activeFaceStop = null

        setContentView(
            SignInScreen.create(
                this,
                {
                    checkAuthentication()
                },
                {
                    showSignUp()
                },
                {
                    showForgotPassword()
                }
            )
        )
    }

    private fun showSignUp() {

        setContentView(
            SignUpScreen.create(
                this,
                {
                    showPinSetup()
                },
                {
                    showSignIn()
                }
            )
        )
    }

    private fun showForgotPassword() {

        setContentView(
            ForgotPasswordScreen.create(
                this
            ) {
                showSignIn()
            }
        )
    }

    private fun showPinSetup() {

        setContentView(
            PinSetupScreen.create(this) {

                startFaceRegistration()
            }
        )
    }

    private fun showPinEntry() {

        setContentView(
            PinEntryScreen.create(
                this,
                {
                    if (AppPreferences.isFaceRegistered(this)) {
                        startFaceVerification()
                    } else {
                        startFaceRegistration()
                    }
                },
                {
                    signOut()
                }
            )
        )
    }

    private fun startFaceRegistration() {

        val faceScreen = FaceRegistration(
            this,
            this
        ) {
            AppPreferences.setFaceRegistered(this, true)
            AppPreferences.setSetupComplete(this, true)
            activeFaceStop = null
            showDashboard()
        }

        activeFaceStop = {
            faceScreen.stop()
        }

        setContentView(faceScreen.create())

        requestCameraAndStart {
            faceScreen.start()
        }
    }

    private fun startFaceVerification() {

        val faceScreen = FaceVerification(
            this,
            this
        ) {
            activeFaceStop = null
            showDashboard()
        }

        activeFaceStop = {
            faceScreen.stop()
        }

        setContentView(faceScreen.create())

        requestCameraAndStart {
            faceScreen.start()
        }
    }

    private fun requestCameraAndStart(
        startAction: () -> Unit
    ) {

        if (
            checkSelfPermission(Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            startAction()
        } else {
            pendingCameraStart = startAction

            cameraPermission.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    private fun showCameraPermissionDialog() {

        AlertDialog.Builder(this)
            .setTitle("Camera Permission Required")
            .setMessage(
                "Guardix needs camera permission for face movement checks. " +
                "Without permission, you cannot continue to the Dashboard."
            )
            .setPositiveButton("Try Again") { _, _ ->

                if (
                    shouldShowRequestPermissionRationale(
                        Manifest.permission.CAMERA
                    )
                ) {
                    cameraPermission.launch(
                        Manifest.permission.CAMERA
                    )
                } else {
                    showCameraSettingsDialog()
                }
            }
            .setNegativeButton("Sign Out") { _, _ ->
                signOut()
            }
            .setCancelable(false)
            .show()
    }

    private fun showCameraSettingsDialog() {

        AlertDialog.Builder(this)
            .setTitle("Permission Needed")
            .setMessage(
                "Please enable Camera permission from Android App Settings."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showDashboard() {

        activeFaceStop?.invoke()
        activeFaceStop = null

        setContentView(
            DashboardScreen.create(this) {
                signOut()
            }
        )
    }

    private fun signOut() {

        activeFaceStop?.invoke()
        activeFaceStop = null

        FirebaseAuth.getInstance().signOut()

        showSignIn()
    }

    override fun onDestroy() {
        handler.removeCallbacks(welcomeDelay)

        activeFaceStop?.invoke()
        activeFaceStop = null

        super.onDestroy()
    }
}