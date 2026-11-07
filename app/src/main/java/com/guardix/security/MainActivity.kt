package com.guardix.security

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

import com.guardix.security.account.AccountManager
import com.guardix.security.auth.SignInScreen
import com.guardix.security.auth.SignUpScreen
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

        if (AppPreferences.isDemoMode(this)) {
            showDashboard()
            return
        }

        if (!AccountManager.isSignedIn(this)) {
            showSignIn()
            return
        }

        if (!PinManager.hasPin(this)) {
            showPinSetup()
        } else {
            showPinEntry()
        }
    }

    private fun stopActiveFaceScreen() {

        activeFaceStop?.invoke()
        activeFaceStop = null
    }

    private fun showSignIn() {

        stopActiveFaceScreen()
        pendingCameraStart = null

        AppPreferences.setDemoMode(this, false)

        setContentView(
            SignInScreen.create(
                this,
                {
                    checkAuthentication()
                },
                {
                    showSignUp()
                },
                {},
                {
                    showDemoWarning()
                }
            )
        )
    }

    private fun showSignUp() {

        stopActiveFaceScreen()
        pendingCameraStart = null

        setContentView(
            SignUpScreen.create(
                this,
                {
                    showPinSetup()
                },
                {
                    showSignIn()
                },
                {
                    showDemoWarning()
                }
            )
        )
    }

    private fun showDemoWarning() {

        AlertDialog.Builder(this)
            .setTitle("Skip Account Setup?")
            .setMessage(
                "You are entering Demo Mode without creating an account.\n\n" +
                "Your demo PIN and face setup will remain on this device only. " +
                "If app data is cleared or the app is uninstalled, " +
                "your demo data may be permanently lost.\n\n" +
                "Create an account later to use account-based backup " +
                "when that feature becomes available."
            )
            .setPositiveButton("OK") { _, _ ->

                AccountManager.signOut(this)
                AppPreferences.setDemoMode(this, true)

                showDashboard()
            }
            .setNegativeButton("CANCEL", null)
            .setCancelable(false)
            .show()
    }

    private fun showPinSetup() {

        stopActiveFaceScreen()
        pendingCameraStart = null

        setContentView(
            PinSetupScreen.create(this) {
                startFaceRegistration()
            }
        )
    }

    private fun showPinEntry() {

        stopActiveFaceScreen()
        pendingCameraStart = null

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

        stopActiveFaceScreen()

        val faceScreen = FaceRegistration(
            this,
            this
        ) {

            // Stop camera before leaving Registration.
            stopActiveFaceScreen()

            AppPreferences.setFaceRegistered(this, true)
            AppPreferences.setSetupComplete(this, true)

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

        stopActiveFaceScreen()

        val faceScreen = FaceVerification(
            this,
            this
        ) {

            // Stop camera before leaving Verification.
            stopActiveFaceScreen()

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
            PackageManager.PERMISSION_GRANTED
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
                "Guardix needs camera permission for face setup and verification."
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
            .setNegativeButton("Back to Dashboard") { _, _ ->
                pendingCameraStart = null
                showDashboard()
            }
            .setCancelable(false)
            .show()
    }

    private fun showCameraSettingsDialog() {

        AlertDialog.Builder(this)
            .setTitle("Camera Permission Needed")
            .setMessage(
                "Please enable Camera permission from Android App Settings."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showDashboard() {

        stopActiveFaceScreen()
        pendingCameraStart = null

        setContentView(
            DashboardScreen.create(
                this,
                onSetupSecurity = {
                    showPinSetup()
                },
                onFaceSecurity = {

                    if (AppPreferences.isFaceRegistered(this)) {
                        startFaceVerification()
                    } else {
                        startFaceRegistration()
                    }
                },
                onSignOut = {
                    signOut()
                }
            )
        )
    }

    private fun signOut() {

        stopActiveFaceScreen()
        pendingCameraStart = null

        if (AppPreferences.isDemoMode(this)) {
            AppPreferences.setDemoMode(this, false)
        } else {
            AccountManager.signOut(this)
        }

        showSignIn()
    }

    override fun onDestroy() {

        handler.removeCallbacks(welcomeDelay)

        pendingCameraStart = null
        stopActiveFaceScreen()

        super.onDestroy()
    }
}