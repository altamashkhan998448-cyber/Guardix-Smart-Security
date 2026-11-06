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
import com.google.firebase.auth.UserProfileChangeRequest

object SignUpScreen {

    fun create(
        context: Context,
        onSignUpSuccess: () -> Unit,
        onSignInClick: () -> Unit
    ): ScrollView {

        val background = Color.rgb(10, 16, 35)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 30, 28, 30)
            setBackgroundColor(background)
        }

        fun addInput(
            hintText: String,
            inputTypeValue: Int
        ): EditText {

            val input = EditText(context).apply {
                hint = hintText
                inputType = inputTypeValue
                textSize = 16f
                setTextColor(Color.WHITE)
                setHintTextColor(Color.LTGRAY)
                setPadding(18, 12, 18, 12)
                setBackgroundColor(Color.rgb(30, 40, 65))
            }

            root.addView(
                input,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 15
                }
            )

            return input
        }

        val title = TextView(context).apply {
            text = "CREATE ACCOUNT"
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 25)
        }

        root.addView(title)

        val nameInput = addInput(
            "Full Name",
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_WORDS
        )

        val emailInput = addInput(
            "Email Address",
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )

        val phoneInput = addInput(
            "Phone Number",
            InputType.TYPE_CLASS_PHONE
        )

        val passwordInput = addInput(
            "Password",
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        val confirmPasswordInput = addInput(
            "Confirm Password",
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        val signUpButton = Button(context).apply {
            text = "CREATE ACCOUNT"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(35, 110, 230))
        }

        root.addView(
            signUpButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        signUpButton.setOnClickListener {

            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()
            val password = passwordInput.text.toString()
            val confirmPassword =
                confirmPasswordInput.text.toString()

            if (
                name.isEmpty() ||
                email.isEmpty() ||
                phone.isEmpty() ||
                password.isEmpty() ||
                confirmPassword.isEmpty()
            ) {
                Toast.makeText(
                    context,
                    "Please fill all fields",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(
                    context,
                    "Passwords do not match",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(
                    context,
                    "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            signUpButton.isEnabled = false
            signUpButton.text = "CREATING ACCOUNT..."

            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {

                        val user = FirebaseAuth.getInstance().currentUser

                        val profile = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()

                        user?.updateProfile(profile)
                            ?.addOnCompleteListener {

                                context.getSharedPreferences(
                                    "GuardixUserData",
                                    Context.MODE_PRIVATE
                                ).edit()
                                    .putString("phone", phone)
                                    .apply()

                                signUpButton.isEnabled = true
                                signUpButton.text = "CREATE ACCOUNT"

                                onSignUpSuccess()
                            }

                    } else {

                        signUpButton.isEnabled = true
                        signUpButton.text = "CREATE ACCOUNT"

                        Toast.makeText(
                            context,
                            task.exception?.localizedMessage
                                ?: "Account creation failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        val signInText = TextView(context).apply {
            text = "Already have an account? Sign In"
            textSize = 15f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
            setPadding(10, 25, 10, 15)
        }

        root.addView(signInText)

        signInText.setOnClickListener {
            onSignInClick()
        }

        return ScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(background)
            addView(root)
        }
    }
}