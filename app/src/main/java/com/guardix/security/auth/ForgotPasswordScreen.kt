package com.guardix.security.auth

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

object ForgotPasswordScreen {

    fun create(
        context: Context,
        onBackToSignIn: () -> Unit
    ): ScrollView {

        val background = Color.rgb(10, 16, 35)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 30, 28, 30)
            setBackgroundColor(background)
        }

        val title = TextView(context).apply {
            text = "RESET PASSWORD"
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 20)
        }

        root.addView(title)

        val description = TextView(context).apply {
            text = "Enter your registered email address. We will send you a password reset link."
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 25)
        }

        root.addView(description)

        val emailInput = EditText(context).apply {
            hint = "Registered Email Address"
            textSize = 16f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(18, 15, 18, 15)
            setBackgroundColor(Color.rgb(30, 40, 65))
        }

        root.addView(
            emailInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        )

        val resetButton = Button(context).apply {
            text = "SEND RESET LINK"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(35, 110, 230))
        }

        root.addView(
            resetButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        resetButton.setOnClickListener {

            val email = emailInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(
                    context,
                    "Please enter your email",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            resetButton.isEnabled = false
            resetButton.text = "SENDING..."

            FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->

                    resetButton.isEnabled = true
                    resetButton.text = "SEND RESET LINK"

                    if (task.isSuccessful) {
                        Toast.makeText(
                            context,
                            "If the account exists, a reset email will be sent.",
                            Toast.LENGTH_LONG
                        ).show()

                        onBackToSignIn()

                    } else {
                        Toast.makeText(
                            context,
                            "Unable to send reset email. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        val backButton = TextView(context).apply {
            text = "← Back to Sign In"
            textSize = 16f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
            setPadding(10, 25, 10, 15)
        }

        root.addView(backButton)

        backButton.setOnClickListener {
            onBackToSignIn()
        }

        return ScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(background)
            addView(root)
        }
    }
}