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

object PinSetupScreen {

    fun create(
        context: Context,
        onPinCreated: () -> Unit
    ): ScrollView {

        val background = Color.rgb(10, 16, 35)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 30, 28, 30)
            setBackgroundColor(background)
        }

        val title = TextView(context).apply {
            text = "CREATE SECURITY PIN"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 20)
        }

        root.addView(title)

        val description = TextView(context).apply {
            text = "Create a 4 to 8 digit PIN to protect your Guardix account."
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 25)
        }

        root.addView(description)

        fun createPinInput(hintText: String): EditText {
            return EditText(context).apply {
                hint = hintText
                inputType = InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_VARIATION_PASSWORD
                setSingleLine(true)
                textSize = 18f
                setTextColor(Color.WHITE)
                setHintTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER
                setPadding(15, 15, 15, 15)
                setBackgroundColor(Color.rgb(30, 40, 65))
            }
        }

        val pinInput = createPinInput("Enter 4–8 Digit PIN")

        root.addView(
            pinInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 18
            }
        )

        val confirmInput = createPinInput("Confirm Your PIN")

        root.addView(
            confirmInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 25
            }
        )

        val saveButton = Button(context).apply {
            text = "SAVE SECURITY PIN"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(35, 110, 230))
        }

        root.addView(
            saveButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        saveButton.setOnClickListener {

            val pin = pinInput.text.toString()
            val confirmPin = confirmInput.text.toString()

            if (!pin.matches(Regex("^\\d{4,8}$"))) {
                Toast.makeText(
                    context,
                    "PIN must contain 4 to 8 digits",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (pin != confirmPin) {
                Toast.makeText(
                    context,
                    "PINs do not match",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            saveButton.isEnabled = false
            saveButton.text = "SAVING..."

            val saved = PinManager.savePin(context, pin)

            saveButton.isEnabled = true
            saveButton.text = "SAVE SECURITY PIN"

            if (saved) {
                Toast.makeText(
                    context,
                    "Security PIN saved successfully",
                    Toast.LENGTH_SHORT
                ).show()

                onPinCreated()

            } else {
                Toast.makeText(
                    context,
                    "Unable to save PIN. Please sign in again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        return ScrollView(context).apply {
            isFillViewport = true
            setBackgroundColor(background)
            addView(root)
        }
    }
}