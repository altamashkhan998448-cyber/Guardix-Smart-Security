package com.guardix.security.auth

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.widget.*
import com.guardix.security.account.AccountManager

object SignUpScreen {

    fun create(
        context: Context,
        onSignUpSuccess: () -> Unit,
        onSignInClick: () -> Unit,
        onSkip: () -> Unit = {}
    ): ScrollView {

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(25, 30, 25, 30)
            setBackgroundColor(Color.rgb(10, 16, 35))
        }

        fun addText(text: String, size: Float, color: Int) {
            root.addView(TextView(context).apply {
                this.text = text
                textSize = size
                setTextColor(color)
                gravity = Gravity.CENTER
                setPadding(0, 12, 0, 12)
            })
        }

        fun addField(hintText: String, type: Int): EditText {
            val field = EditText(context).apply {
                hint = hintText
                inputType = type
                setTextColor(Color.WHITE)
                setHintTextColor(Color.LTGRAY)
                textSize = 16f
                setPadding(18, 12, 18, 12)
                setSingleLine(true)
                setBackgroundColor(Color.rgb(30, 43, 70))
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 8, 0, 8)
            root.addView(field, params)

            return field
        }

        addText("🛡️ GUARDIX", 29f, Color.CYAN)
        addText("CREATE YOUR ACCOUNT", 21f, Color.WHITE)

        val name = addField(
            "Full Name",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_CAP_WORDS
        )

        val email = addField(
            "Email Address",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )

        val phone = addField(
            "10 Digit Mobile Number",
            InputType.TYPE_CLASS_NUMBER
        )

        phone.filters = arrayOf(InputFilter.LengthFilter(10))

        val password = addField(
            "Password (Minimum 8 Characters)",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        val confirmPassword = addField(
            "Confirm Password",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        fun addButton(
            label: String,
            background: Int,
            action: () -> Unit
        ) {
            val button = TextView(context).apply {
                text = label
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(15, 17, 15, 17)
                setBackgroundColor(background)
                setOnClickListener { action() }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 10, 0, 5)
            root.addView(button, params)
        }

        addButton("CREATE ACCOUNT", Color.rgb(20, 115, 190)) {

            val fullName = name.text.toString().trim()
            val userEmail = email.text.toString().trim()
            val mobile = phone.text.toString()
            val pass = password.text.toString()
            val confirm = confirmPassword.text.toString()

            if (pass != confirm) {
                Toast.makeText(
                    context,
                    "Passwords do not match.",
                    Toast.LENGTH_SHORT
                ).show()
                return@addButton
            }

            val result = AccountManager.createAccount(
                context,
                fullName,
                userEmail,
                mobile,
                pass
            )

            if (result == "SUCCESS") {
                Toast.makeText(
                    context,
                    "Account created successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                onSignUpSuccess()
            } else {
                Toast.makeText(
                    context,
                    result,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        addButton("ALREADY HAVE AN ACCOUNT? SIGN IN", Color.DKGRAY) {
            onSignInClick()
        }

        addButton("SKIP FOR TESTING", Color.rgb(80, 65, 130)) {
            onSkip()
        }

        return ScrollView(context).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = true
            addView(root)
            setBackgroundColor(Color.rgb(10, 16, 35))
        }
    }
}