package com.guardix.security.auth

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

object SignInScreen {

    fun create(
        context: Context,
        onSignInSuccess: () -> Unit,
        onSignUpClick: () -> Unit,
        onForgotPasswordClick: () -> Unit
    ): ScrollView {

        val backgroundColor = Color.rgb(10, 16, 35)
        val blueColor = Color.rgb(35, 110, 230)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 35, 28, 35)
            setBackgroundColor(backgroundColor)
        }

        val title = TextView(context).apply {
            text = "GUARDIX"
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        root.addView(title)

        val subtitle = TextView(context).apply {
            text = "Welcome Back"
            textSize = 22f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 30)
        }

        root.addView(subtitle)

        val emailInput = EditText(context).apply {
            hint = "Email Address"
            textSize = 16f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
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
                bottomMargin = 18
            }
        )

        val passwordInput = EditText(context).apply {
            hint = "Password"
            textSize = 16f
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(18, 15, 18, 15)
            setBackgroundColor(Color.rgb(30, 40, 65))
        }

        root.addView(
            passwordInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 22
            }
        )

        val signInButton = Button(context).apply {
            text = "SIGN IN"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(blueColor)
        }

        root.addView(
            signInButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        signInButton.setOnClickListener {

            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                    context,
                    "Please enter email and password",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            signInButton.isEnabled = false
            signInButton.text = "PLEASE WAIT..."

            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    signInButton.isEnabled = true
                    signInButton.text = "SIGN IN"

                    if (task.isSuccessful) {
                        onSignInSuccess()
                    } else {
                        Toast.makeText(
                            context,
                            task.exception?.localizedMessage
                                ?: "Sign in failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        val forgotPassword = TextView(context).apply {
            text = "Forgot Password?"
            textSize = 15f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
            setPadding(0, 25, 0, 20)
        }

        root.addView(forgotPassword)

        forgotPassword.setOnClickListener {
            onForgotPasswordClick()
        }

        val createAccount = TextView(context).apply {
            text = "Don't have an account? Create Account"
            textSize = 15f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 15)
        }

        root.addView(createAccount)

        createAccount.setOnClickListener {
            onSignUpClick()
        }

        return ScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(backgroundColor)
            addView(root)
        }
    }
}