package com.guardix.security

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.content.SharedPreferences
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class MainActivity : Activity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var root: LinearLayout
    private lateinit var prefs: SharedPreferences

    private var isHindi = false
    private var statusText: TextView? = null

    private val bgColor = Color.rgb(10, 16, 35)
    private val cyanColor = Color.CYAN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        prefs = getSharedPreferences("GuardixSettings", MODE_PRIVATE)

        showWelcome()
    }

    private fun createRoot(): LinearLayout {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 25, 24, 25)
            setBackgroundColor(bgColor)
        }
        return root
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

    private fun addText(text: String, size: Float = 16f) {
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
            setOnClickListener { action() }
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
                InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
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
            getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager

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

        addButton("Continue / आगे बढ़ें") {
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

        val passwordInput = addInput(
            "Create Password",
            true
        )

        val confirmInput = addInput(
            "Confirm Password",
            true
        )

        addStatus()

        addButton("Create Account / अकाउंट बनाएँ") {

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
                showStatus("Password कम से कम 6 अक्षरों का होना चाहिए।")
                return@addButton
            }

            if (password != confirm) {
                showStatus("दोनों Password एक जैसे नहीं हैं।")
                return@addButton
            }

            showStatus("Account बनाया जा रहा है...")

            auth.createUserWithEmailAndPassword(
                email,
                password
            ).addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    val user = auth.currentUser

                    val profile = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()

                    user?.updateProfile(profile)
                        ?.addOnCompleteListener {

                            val uid = user.uid

                            prefs.edit()
                                .putString("name_$uid", name)
                                .putString("phone_$uid", phone)
                                .putBoolean("setup_$uid", false)
                                .apply()

                            auth.signOut()

                            AlertDialog.Builder(this)
                                .setTitle("Account Created")
                                .setMessage(
                                    "आपका अकाउंट बन गया है। " +
                                    "अब Email और Password से Sign In करें।"
                                )
                                .setPositiveButton("Sign In") { _, _ ->
                                    showSignIn()
                                }
                                .show()
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

        addButton("Skip / बिना अकाउंट जारी रखें") {
            showSkipWarning()
        }

        setContentView(makeScrollable(root))
    }

    private fun showSignIn() {
        createRoot()

        addTitle("GUARDIX LOGIN")
        addText("Email और Password से Sign In करें")

        val emailInput = addInput("Email Address")
        val passwordInput = addInput(
            "Password",
            true
        )

        addStatus()

        addButton("Sign In / लॉगिन") {

            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            hideKeyboard()

            if (email.isEmpty() || password.isEmpty()) {
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

        addButton("Skip / बिना अकाउंट जारी रखें") {
            showSkipWarning()
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

            val email = emailInput.text.toString().trim()

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

    private fun showSkipWarning() {

        val message = if (isHindi) {
            """
            बिना अकाउंट जारी रखने पर:

            • आप ऐप को स्थानीय रूप से इस्तेमाल कर सकते हैं।
            • Cloud Backup उपलब्ध नहीं होगा।
            • ऐप का डेटा साफ करने या ऐप हटाने पर
              स्थानीय डेटा हमेशा के लिए खो सकता है।
            • Skip करने से अभी कोई डेटा डिलीट नहीं होगा।

            सुरक्षा और डेटा रिकवरी के लिए अकाउंट बनाना बेहतर है।

            क्या आप फिर भी जारी रखना चाहते हैं?
            """.trimIndent()
        } else {
            """
            If you continue without an account:

            • You can use the app locally.
            • Cloud backup will not be available.
            • Clearing app data or uninstalling the app
              may permanently delete local data.
            • Choosing Skip will NOT delete your data now.

            Creating an account is recommended for recovery.

            Do you still want to continue?
            """.trimIndent()
        }

        AlertDialog.Builder(this)
            .setTitle("Important Warning / जरूरी सूचना")
            .setMessage(message)
            .setNegativeButton("Go Back") { _, _ ->
                showSignIn()
            }
            .setPositiveButton("Continue") { _, _ ->
                showLocalDashboard()
            }
            .show()
    }

    private fun checkSecuritySetup(uid: String) {

        val completed = prefs.getBoolean(
            "setup_$uid",
            false
        )

        if (completed) {
            showDashboard()
        } else {
            showSecuritySetup(uid)
        }
    }

    private fun showSecuritySetup(uid: String) {
        createRoot()

        addTitle("SECURITY SETUP")

        addText(
            "Welcome to Guardix!\n\n" +
            "आपका पहला लॉगिन सफल रहा है।\n" +
            "अब Security Setup पूरा करें।"
        )

        addButton("Continue Setup / सेटअप शुरू करें") {

            AlertDialog.Builder(this)
                .setTitle("Security Setup")
                .setMessage(
                    "अगले चरण में PIN, Pattern या Password " +
                    "चुनने और सेट करने की सुविधा जोड़ी जाएगी।"
                )
                .setPositiveButton("Continue") { _, _ ->

                    prefs.edit()
                        .putBoolean("setup_$uid", true)
                        .apply()

                    showDashboard()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        setContentView(root)
    }

    private fun showDashboard() {
        createRoot()

        addText("🛡️", 60f)
        addTitle("GUARDIX")

        val userName = auth.currentUser?.displayName
            ?: "User"

        addText("Welcome, $userName!", 20f)
        addText("SMART SECURITY", 15f)

        addText(
            "आपका Guardix अकाउंट सफलतापूर्वक लॉगिन हो गया है।"
        )

        addButton("Sign Out / लॉगआउट") {
            auth.signOut()
            showSignIn()
        }

        setContentView(root)
    }

    private fun showLocalDashboard() {
        createRoot()

        addText("🛡️", 60f)
        addTitle("GUARDIX")

        addText("Local Mode / बिना अकाउंट")

        addText(
            "आप Guardix को स्थानीय रूप से इस्तेमाल कर रहे हैं।"
        )

        addButton("Create Account") {
            showSignUp()
        }

        addButton("Sign In") {
            showSignIn()
        }

        setContentView(root)
    }

    private fun makeScrollable(view: View): ScrollView {
        return ScrollView(this).apply {
            setBackgroundColor(bgColor)
            isFillViewport = true
            addView(view)
        }
    }
}