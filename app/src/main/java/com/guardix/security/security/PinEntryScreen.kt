package com.guardix.security.security

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

object PinEntryScreen {

    fun create(
        context: Context,
        onPinVerified: () -> Unit,
        onSignOut: () -> Unit
    ): ScrollView {

        val background = Color.rgb(10, 16, 35)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 30, 28, 30)
            setBackgroundColor(background)
        }

        val logo = TextView(context).apply {
            text = "🛡️"
            textSize = 55f
            gravity = Gravity.CENTER
        }

        root.addView(logo)

        val title = TextView(context).apply {
            text = "GUARDIX SECURITY"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 15, 0, 12)
        }

        root.addView(title)

        val description = TextView(context).apply {
            text = "Enter your security PIN to continue"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 25)
        }

        root.addView(description)

        val pinInput = EditText(context).apply {
            hint = "Enter Your PIN"
            inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setSingleLine(true)
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(15, 15, 15, 15)
            setBackgroundColor(Color.rgb(30, 40, 65))
        }

        root.addView(
            pinInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 25
            }
        )

        val verifyButton = Button(context).apply {
            text = "UNLOCK GUARDIX"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(35, 110, 230))
        }

        root.addView(
            verifyButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        verifyButton.setOnClickListener {

            val enteredPin = pinInput.text.toString()

            if (!enteredPin.matches(Regex("^\\d{4,8}$"))) {
                Toast.makeText(
                    context,
                    "Enter a valid 4 to 8 digit PIN",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            verifyButton.isEnabled = false
            verifyButton.text = "VERIFYING..."

            val isCorrect = PinManager.verifyPin(
                context,
                enteredPin
            )

            verifyButton.isEnabled = true
            verifyButton.text = "UNLOCK GUARDIX"

            if (isCorrect) {
                onPinVerified()
            } else {
                pinInput.text.clear()

                Toast.makeText(
                    context,
                    "Incorrect PIN. Try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val signOutText = TextView(context).apply {
            text = "Sign Out"
            textSize = 16f
            setTextColor(Color.CYAN)
            gravity = Gravity.CENTER
            setPadding(10, 30, 10, 15)
        }

        root.addView(signOutText)

        signOutText.setOnClickListener {
            onSignOut()
        }

        return ScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(background)
            addView(root)
        }
    }
}