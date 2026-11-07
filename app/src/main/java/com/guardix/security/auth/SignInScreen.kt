
package com.guardix.security.auth

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.widget.*
import com.guardix.security.account.AccountManager

object SignInScreen {

    fun create(
        context: Context,
        onSignInSuccess: () -> Unit,
        onSignUpClick: () -> Unit,
        onForgotPasswordClick: () -> Unit,
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

        fun addButton(
            label: String,
            background: Int,
            action: () -> Unit
        ) {
            val button = TextView(context).apply {
                text = label
                textSize = 16f
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

        addText("🛡️ GUARDIX", 30f, Color.CYAN)
        addText("SIGN IN TO YOUR ACCOUNT", 20f, Color.WHITE)

        val email = addField(
            "Email Address",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )

        val password = addField(
            "Password",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        addButton("SIGN IN", Color.rgb(20, 115, 190)) {

            val userEmail = email.text.toString().trim()
            val userPassword = password.text.toString()

            if (userEmail.isBlank() || userPassword.isBlank()) {
                Toast.makeText(
                    context,
                    "Enter email and password.",
                    Toast.LENGTH_SHORT
                ).show()
                return@addButton
            }

            val success = AccountManager.signIn(
                context,
                userEmail,
                userPassword
            )

            if (success) {
                Toast.makeText(
                    context,
                    "Login successful!",
                    Toast.LENGTH_SHORT
                ).show()

                onSignInSuccess()
            } else {
                Toast.makeText(
                    context,
                    "Incorrect email or password.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        addButton("CREATE NEW ACCOUNT", Color.DKGRAY) {
            onSignUpClick()
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