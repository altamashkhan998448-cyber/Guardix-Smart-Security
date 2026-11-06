package com.guardix.security

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.util.Size
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.util.Base64
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var prefs: SharedPreferences
    private lateinit var root: LinearLayout

    private var isHindi = false
    private var statusText: TextView? = null

    private val bgColor = Color.rgb(10, 16, 35)
    private val cyanColor = Color.CYAN

    private var cameraProvider: ProcessCameraProvider? = null
    private var faceDetector: FaceDetector? = null
    private var cameraPreview: PreviewView? = null
    private var cameraStatus: TextView? = null

    private val processing =
        AtomicBoolean(false)

    private var currentUid = ""
    private var cameraMode = "registration"

    private var faceStep = 0
    private var eyesOpened = false
    private var eyesClosed = false

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                startFaceCamera()
            } else {
                showCameraPermissionError()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        prefs = getSharedPreferences(
            "GuardixSettings",
            MODE_PRIVATE
        )

        showWelcome()
    }

    private fun createRoot() {

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 25, 24, 25)
            setBackgroundColor(bgColor)
        }
    }

    private fun addTitle(text: String) {

        val title = TextView(this).apply {
            this.text = text
            textSize = 29f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 15)
        }

        root.addView(title)
    }

    private fun addText(
        text: String,
        size: Float = 16f
    ) {

        val view = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(8, 12, 8, 12)
        }

        root.addView(view)
    }

    private fun addButton(
        text: String,
        action: () -> Unit
    ) {

        val button = Button(this).apply {
            this.text = text
            isAllCaps = false
            setOnClickListener {
                action()
            }
        }

        root.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 7, 0, 7)
            }
        )
    }

    private fun addInput(
        hint: String,
        password: Boolean = false
    ): EditText {

        val input = EditText(this).apply {

            this.hint = hint
            textSize = 16f

            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)

            setSingleLine(true)

            inputType = if (password) {
                InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_VARIATION_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT
            }

            setPadding(15, 12, 15, 12)
        }

        root.addView(
            input,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 5, 0, 5)
            }
        )

        return input
    }

    private fun addStatus() {

        statusText = TextView(this).apply {
            text = ""
            textSize = 14f
            setTextColor(cyanColor)
            gravity = Gravity.CENTER
            setPadding(8, 12, 8, 12)
        }

        root.addView(statusText)
    }

    private fun showStatus(message: String) {
        statusText?.text = message
    }

    private fun hideKeyboard() {

        val manager =
            getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        manager.hideSoftInputFromWindow(
            root.windowToken,
            0
        )
    }

    private fun showWelcome() {

        createRoot()

        addText("🛡️", 65f)
        addTitle("GUARDIX")
        addText("SMART SECURITY", 15f)

        val info = if (isHindi) {

            """
            ऐप निर्माता

            नाम: Altamash Khan
            उम्र: 14 वर्ष
            कक्षा: 6वीं
            पता: बहराइच, उत्तर प्रदेश, भारत

            इस ऐप को बनाने का उद्देश्य
            मोबाइल सुरक्षा को बेहतर बनाना है।
            """.trimIndent()

        } else {

            """
            CREATED BY

            Name: Altamash Khan
            Age: 14 Years
            Class: 6th
            Address: Bahraich, Uttar Pradesh, India

            Our goal is to make
            mobile security smarter.
            """.trimIndent()
        }

        addText(info, 17f)

        addButton("हिंदी / English") {
            isHindi = !isHindi
            showWelcome()
        }

        addButton("OK / Continue") {
            checkLoginStatus()
        }

        setContentView(root)
    }

    private fun checkLoginStatus() {

        val user = auth.currentUser

        if (user != null) {
            checkSecuritySetup(user.uid)
        } else {
            showSignIn()
        }
    }

    private fun showSignUp() {

        createRoot()

        addTitle("Create Account")
        addText("अपना नया Guardix अकाउंट बनाएँ")

        val nameInput = addInput("Full Name")
        val emailInput = addInput("Email Address")
        val phoneInput = addInput("Phone Number")

        val passwordInput =
            addInput("Create Password", true)

        val confirmInput =
            addInput("Confirm Password", true)

        addStatus()

        addButton("Create Account") {

            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirm = confirmInput.text.toString()

            hideKeyboard()

            if (
                name.isEmpty() ||
                email.isEmpty() ||
                phone.isEmpty() ||
                password.isEmpty() ||
                confirm.isEmpty()
            ) {
                showStatus("कृपया सभी जानकारी भरें।")
                return@addButton
            }

            if (password.length < 6) {
                showStatus(
                    "Password कम से कम 6 अक्षरों का होना चाहिए।"
                )
                return@addButton
            }

            if (password != confirm) {
                showStatus(
                    "दोनों Password एक जैसे नहीं हैं।"
                )
                return@addButton
            }

            showStatus("Account बनाया जा रहा है...")

            auth.createUserWithEmailAndPassword(
                email,
                password
            ).addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val user = auth.currentUser

                    val profile =
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()

                    user?.updateProfile(profile)
                        ?.addOnCompleteListener {

                            val uid = user.uid

                            prefs.edit()
                                .putString("name_$uid", name)
                                .putString("phone_$uid", phone)
                                .putBoolean("setup_$uid", false)
                                .putBoolean("face_$uid", false)
                                .apply()

                            auth.signOut()

                            showSignIn()
                        }

                } else {

                    showStatus(
                        task.exception?.localizedMessage
                            ?: "Account नहीं बन पाया।"
                    )
                }
            }
        }

        addButton("Already have an account? Sign In") {
            showSignIn()
        }

        setContentView(makeScrollable(root))
    }

    private fun showSignIn() {

        createRoot()

        addTitle("GUARDIX LOGIN")
        addText("Email और Password से Sign In करें")

        val emailInput = addInput("Email Address")

        val passwordInput =
            addInput("Password", true)

        addStatus()

        addButton("Sign In") {

            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            hideKeyboard()

            if (
                email.isEmpty() ||
                password.isEmpty()
            ) {
                showStatus("Email और Password भरें।")
                return@addButton
            }

            showStatus("Logging in...")

            auth.signInWithEmailAndPassword(
                email,
                password
            ).addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val user = auth.currentUser

                    if (user != null) {
                        checkSecuritySetup(user.uid)
                    }

                } else {

                    showStatus(
                        task.exception?.localizedMessage
                            ?: "Login असफल हुआ।"
                    )
                }
            }
        }

        addButton("Forgot Password?") {
            showForgotPassword()
        }

        addButton("Create New Account") {
            showSignUp()
        }

        setContentView(makeScrollable(root))
    }

    private fun showForgotPassword() {

        createRoot()

        addTitle("Forgot Password")
        addText("अपना अकाउंट वाला Email डालें।")

        val emailInput = addInput("Email Address")

        addStatus()

        addButton("Send Reset Link") {

            val email =
                emailInput.text.toString().trim()

            if (email.isEmpty()) {
                showStatus("Email Address भरें।")
                return@addButton
            }

            showStatus("Reset link भेजा जा रहा है...")

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        showStatus(
                            "Password reset link आपके Email पर भेज दिया गया है।"
                        )

                    } else {

                        showStatus(
                            task.exception?.localizedMessage
                                ?: "Reset link नहीं भेजा जा सका।"
                        )
                    }
                }
        }

        addButton("Back to Sign In") {
            showSignIn()
        }

        setContentView(root)
    }

    // SECURITY SETUP

    private fun checkSecuritySetup(uid: String) {

        currentUid = uid

        val completed = prefs.getBoolean(
            "setup_$uid",
            false
        )

        val faceRegistered = prefs.getBoolean(
            "face_$uid",
            false
        )

        if (completed && faceRegistered) {
            showPinEntry()
        } else {
            showPinSetup()
        }
    }

    private fun showPinSetup() {

        createRoot()

        addTitle("CREATE SECURITY PIN")

        addText(
            "अपना 4 से 8 अंकों का Security PIN बनाएँ।"
        )

        val pinInput = addInput(
            "Enter New PIN",
            true
        )

        val confirmInput = addInput(
            "Confirm New PIN",
            true
        )

        addStatus()

        addButton("Save PIN & Continue") {

            val pin = pinInput.text.toString()
            val confirm = confirmInput.text.toString()

            hideKeyboard()

            if (
                pin.length !in 4..8 ||
                !pin.all { it.isDigit() }
            ) {
                showStatus(
                    "PIN में 4 से 8 अंक होने चाहिए।"
                )
                return@addButton
            }

            if (pin != confirm) {
                showStatus("दोनों PIN एक जैसे नहीं हैं।")
                return@addButton
            }

            showStatus("PIN सुरक्षित किया जा रहा है...")

            try {

                val salt = ByteArray(16)
                SecureRandom().nextBytes(salt)

                val hash = hashPin(pin, salt)

                prefs.edit()
                    .putString(
                        "pin_salt_$currentUid",
                        Base64.encodeToString(
                            salt,
                            Base64.NO_WRAP
                        )
                    )
                    .putString(
                        "pin_hash_$currentUid",
                        Base64.encodeToString(
                            hash,
                            Base64.NO_WRAP
                        )
                    )
                    .apply()

                startFaceRegistration()

            } catch (e: Exception) {

                showStatus(
                    "PIN सुरक्षित नहीं हो पाया। दोबारा प्रयास करें।"
                )
            }
        }

        setContentView(makeScrollable(root))
    }

    private fun hashPin(
        pin: String,
        salt: ByteArray
    ): ByteArray {

        val spec = PBEKeySpec(
            pin.toCharArray(),
            salt,
            120000,
            256
        )

        val factory = SecretKeyFactory.getInstance(
            "PBKDF2WithHmacSHA1"
        )

        return factory.generateSecret(spec).encoded
    }

    private fun verifyPin(pin: String): Boolean {

        return try {

            val saltString = prefs.getString(
                "pin_salt_$currentUid",
                null
            ) ?: return false

            val hashString = prefs.getString(
                "pin_hash_$currentUid",
                null
            ) ?: return false

            val salt = Base64.decode(
                saltString,
                Base64.NO_WRAP
            )

            val savedHash = Base64.decode(
                hashString,
                Base64.NO_WRAP
            )

            val enteredHash = hashPin(pin, salt)

            MessageDigest.isEqual(
                savedHash,
                enteredHash
            )

        } catch (e: Exception) {
            false
        }
    }

    private fun showPinEntry() {

        createRoot()

        addTitle("GUARDIX SECURITY")

        addText("अपना Security PIN डालें।")

        val pinInput = addInput(
            "Enter Security PIN",
            true
        )

        addStatus()

        addButton("Verify PIN") {

            val pin = pinInput.text.toString()

            hideKeyboard()

            if (pin.isEmpty()) {
                showStatus("PIN डालें।")
                return@addButton
            }

            if (verifyPin(pin)) {

                startFaceVerification()

            } else {

                pinInput.text.clear()

                showStatus(
                    "गलत PIN! दोबारा प्रयास करें।"
                )
            }
        }

        addButton("Sign Out") {
            auth.signOut()
            showSignIn()
        }

        setContentView(makeScrollable(root))
    }

    // FACE REGISTRATION AND VERIFICATION

    private fun startFaceRegistration() {

        cameraMode = "registration"

        faceStep = 0
        eyesOpened = false
        eyesClosed = false

        requestCameraPermission()
    }

    private fun startFaceVerification() {

        cameraMode = "verification"

        faceStep = 0
        eyesOpened = false
        eyesClosed = false

        requestCameraPermission()
    }

    private fun requestCameraPermission() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            startFaceCamera()

        } else {

            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    private fun showCameraPermissionError() {

        createRoot()

        addTitle("CAMERA PERMISSION REQUIRED")

        addText(
            "Guardix Security के लिए Front Camera Permission जरूरी है।\n\n" +
            "Camera Permission के बिना Verification पूरा नहीं हो सकता।"
        )

        addButton("Try Again") {
            requestCameraPermission()
        }

        addButton("Sign Out") {
            auth.signOut()
            showSignIn()
        }

        setContentView(root)
    }

    private fun startFaceCamera() {

        createRoot()

        addTitle(
            if (cameraMode == "registration") {
                "FACE REGISTRATION"
            } else {
                "FACE VERIFICATION"
            }
        )

        val instruction = TextView(this).apply {
            text = "कैमरे के सामने अपना चेहरा रखें।"
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        root.addView(instruction)

        cameraStatus = TextView(this).apply {
            text = "Camera शुरू हो रहा है..."
            textSize = 16f
            setTextColor(cyanColor)
            gravity = Gravity.CENTER
            setPadding(8, 15, 8, 15)
        }

        root.addView(cameraStatus)

        cameraPreview = PreviewView(this).apply {
            scaleType =
                PreviewView.ScaleType.FILL_CENTER
        }

        root.addView(
            cameraPreview,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                480
            )
        )

        setContentView(root)

        val options =
            FaceDetectorOptions.Builder()
                .setPerformanceMode(
                    FaceDetectorOptions.PERFORMANCE_MODE_FAST
        